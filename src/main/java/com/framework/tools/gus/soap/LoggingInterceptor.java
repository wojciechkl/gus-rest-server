package com.framework.tools.gus.soap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class LoggingInterceptor implements ClientInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean handleRequest(MessageContext messageContext) {
        logMessage("Request", messageContext.getRequest());
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            messageContext.getResponse().writeTo(out);
            logger.info("SOAP Response:\n{}", out.toString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.warn("Failed to log SOAP response", e);
        }
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) {
        logMessage("Fault", messageContext.getResponse());
        return true;
    }

    private void logMessage(String label, WebServiceMessage message) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            message.writeTo(out);
            logger.info("{} SOAP Message:\n{}", label, out.toString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.warn("Failed to log {} SOAP message", label, e);
        }
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            messageContext.getResponse().writeTo(out);
            logger.info("SOAP Message:\n{}", out.toString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.warn("Failed to log SOAP message", e);
        }
        if(ex != null){
            logger.error("after completion ex ", ex);
        }
    }
}
