package com.framework.tools.gus.rest;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SoapClientConfig {

    @Getter
    @Setter
    public static class ConnectionConfig {
        private String keyTest = "abcde12345abcde12345";
        private String key;
        private String urlTest = "https://wyszukiwarkaregontest.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc";
        private String urlProd = "https://wyszukiwarkaregon.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc";
        private List<String> pkdVet = Arrays.asList("7500Z");
    }

    @Bean
    public ConnectionConfig connectionConfig() {
        ConnectionConfig cc = new ConnectionConfig();
        String key = System.getenv("GUS_KEY");
        if(StringUtils.isNotEmpty(key)){
            cc.setKey(key);
        }
        String keyTest = System.getenv("GUS_KEY_TEST");
        if(StringUtils.isNotEmpty(keyTest)){
            cc.setKeyTest(keyTest);
        }
        String urlTest = System.getenv("GUS_URL_TEST");
        if(StringUtils.isNotEmpty(urlTest)){
            cc.setUrlTest(keyTest);
        }
        String urlProd = System.getenv("GUS_URL_PROD");
        if(StringUtils.isNotEmpty(urlProd)){
            cc.setUrlProd(urlProd);
        }
        String pkd = System.getenv("GUS_PKD_VET");
        if(StringUtils.isNotEmpty(pkd)){
            List<String> pkds = Arrays.asList(StringUtils.split(pkd, ","));
            cc.setPkdVet(pkds);

        }
        return cc;
    }

    @Bean
    public SaajSoapMessageFactory messageFactory() throws Exception {
        SaajSoapMessageFactory factory = new SaajSoapMessageFactory();
        factory.setMessageFactory(MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL));
        factory.afterPropertiesSet();
        return factory;
    }
}