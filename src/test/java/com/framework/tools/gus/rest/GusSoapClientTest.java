package com.framework.tools.gus.rest;

import com.framework.tools.gus.soap.CompanyInfo;
import com.framework.tools.gus.soap.GusSoapClient;
import com.framework.tools.gus.ws.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GusSoapClientTest {
    private static final Logger logger = LoggerFactory.getLogger(GusSoapClientTest.class);

    @Autowired
    private GusSoapClient soapClient;
    @Autowired
    private SoapClientConfig.ConnectionConfig connectionConfig;
    @Test
    void login() {
        soapClient.setUrl(connectionConfig.getUrlTest());
        soapClient.login("abcde12345abcde12345");
        logger.info("sid returned: {}", soapClient.getSessionId());

        CompanyInfo companyFullData = soapClient.getCompanyFullData("9522033291");
        logger.info("Company data loaded: {}", companyFullData);

        boolean wylResult = soapClient.logout();
        logger.info("Wyloguj response: {}", wylResult);
        assertTrue(wylResult);
    }
}