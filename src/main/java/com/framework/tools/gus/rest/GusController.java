package com.framework.tools.gus.rest;

import com.framework.tools.gus.soap.CompanyInfo;
import com.framework.tools.gus.soap.GusException;
import com.framework.tools.gus.soap.GusSoapClient;
import com.framework.tools.gus.ws.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@RestController
public class GusController {
    private static final Logger logger = LoggerFactory.getLogger(GusController.class);

    @Autowired
    private GusClientWithRateLimiter gusClientWithRateLimiter;
    @Autowired
    private SoapClientConfig.ConnectionConfig connectionConfig;

    @GetMapping(path="/gus", produces="application/json")
    public ResponseEntity<CompanyInfo> getCompanyInfo(@RequestParam String nip, @RequestParam(required = false) String key) {
        key = configureClientAndGetKey(key);
        nip = validateAndNormalizeNip(nip);
        GusClientWithRateLimiter.GusFuture<CompanyInfo> future = gusClientWithRateLimiter.createFuture(nip, key);

        boolean accepted = gusClientWithRateLimiter.submit(future);

        if (!accepted) {
            return ResponseEntity.status(429).build();
        }

        try {
            // Optional timeout for waiting clients
            CompanyInfo result = future.get(60, TimeUnit.SECONDS);
            return ResponseEntity.ok(result);
        }
         catch(CompletionException ex) {
            try {
                throw ex.getCause();
            }
            catch(Error|RuntimeException possible) {
                throw possible;
            }
            catch(Throwable impossible) {
                throw new AssertionError(impossible);
            }
        } catch (ExecutionException|InterruptedException|TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    private String validateAndNormalizeNip(String nip) {
        nip = nip.trim().toUpperCase();
        nip = StringUtils.replace(nip, "-", "");
        nip = StringUtils.replace(nip, " ", "");
        nip = StringUtils.replace(nip, "PL", "");

        if(!nip.matches("\\d{10}")){
            throw new IllegalArgumentException("Błędny format NIP");
        }
        return nip;
    }

    private String configureClientAndGetKey(String key) {
        if(StringUtils.isEmpty(key)){
            key = connectionConfig.getKey();
            gusClientWithRateLimiter.setUrl(connectionConfig.getUrlProd());
        }
        if(StringUtils.isEmpty(key)){
            throw new NullPointerException("API key not found");
        }
        if("test".equals(key)){
            key = connectionConfig.getKeyTest();
            gusClientWithRateLimiter.setUrl(connectionConfig.getUrlTest());
        }
        else{
            gusClientWithRateLimiter.setUrl(connectionConfig.getUrlProd());
        }
        return key;
    }

    @ExceptionHandler(GusException.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(GusException ex) {
        logger.error("Exception", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "error", ex.getError(),
                        "errorCode", ex.getErrorCode()
                ));
    }
}
