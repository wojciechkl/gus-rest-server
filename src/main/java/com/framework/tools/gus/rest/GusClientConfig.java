package com.framework.tools.gus.rest;

import com.framework.tools.gus.soap.GusSoapClient;
import com.framework.tools.gus.soap.LoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

@Configuration
public class GusClientConfig {
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.framework.tools.gus.ws");
        marshaller.setMtomEnabled(true);
        return marshaller;
    }

    @Bean
    public GusSoapClient soapClient(SaajSoapMessageFactory messageFactory, Jaxb2Marshaller marshaller) {
        GusSoapClient client = new GusSoapClient();
        client.setMessageFactory(messageFactory);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);

        client.setInterceptors(new ClientInterceptor[]{
                new LoggingInterceptor()
        });
        return client;
    }
}