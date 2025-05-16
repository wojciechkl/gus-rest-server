package com.framework.tools.gus.rest;

import com.framework.tools.gus.soap.CompanyInfo;
import com.framework.tools.gus.soap.GusSoapClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GusClientWithRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(GusClientWithRateLimiter.class);

    private final int rateLimitPerSecond = 1;
    private final int maxQueueSize = 100;

    private final BlockingQueue<GusFuture<CompanyInfo>> requestQueue = new LinkedBlockingQueue<>(maxQueueSize);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService workerPool = Executors.newCachedThreadPool();

    private final AtomicInteger tokens = new AtomicInteger(rateLimitPerSecond);
    private Instant lastRefillTime = Instant.now();

    @Autowired
    private GusSoapClient soapClient;

    public GusClientWithRateLimiter() {
        // refill tokens and process queue every 200ms
        scheduler.scheduleAtFixedRate(this::refillTokensAndProcessQueue, 0, 30, TimeUnit.MILLISECONDS);
    }

    public void setUrl(String url){
        this.soapClient.setUrl(url);
    }

    private synchronized void refillTokensAndProcessQueue() {
        long elapsedMillis = Instant.now().toEpochMilli() - lastRefillTime.toEpochMilli();
        int refill = (int) (elapsedMillis * rateLimitPerSecond / 1000);
        if (refill > 0) {
            int current = tokens.get();
            tokens.set(Math.min(rateLimitPerSecond, current + refill));
            lastRefillTime = Instant.now();
        }

        while (!requestQueue.isEmpty() && tokens.get() > 0) {
            GusFuture<CompanyInfo> future = requestQueue.poll();
            if (future != null && tokens.getAndDecrement() > 0) {
                CompanyInfo ci = getInfo(future.getNip(), future.getKey());
                workerPool.submit(() -> future.complete(ci));
            }
        }
    }

    public boolean submit(GusFuture<CompanyInfo> future) {
        synchronized (this) {
            if (tokens.get() > 0) {
                tokens.decrementAndGet();

                CompanyInfo ci = getInfo(future.getNip(), future.getKey());
                workerPool.submit(() -> future.complete(ci));
                return true;
            } else {
                return requestQueue.offer(future);
            }
        }
    }

    private CompanyInfo getInfo(String nip, String key) {
        logger.info("Retrieving company info for nip: {}", nip);
        soapClient.login(key);
        return soapClient.getCompanyFullData(nip);
    }

    @AllArgsConstructor
    @Getter
    public static class GusFuture<CompanyInfo> extends CompletableFuture<CompanyInfo>{
        private String nip;
        private String key;
    }

    public GusFuture<CompanyInfo> createFuture(String nip, String key) {
        return new GusFuture<>(nip, key);
    }
}
