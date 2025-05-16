package com.framework.tools.gus.soap;

import com.framework.tools.gus.rest.GusClientConfig;
import com.framework.tools.gus.rest.SoapClientConfig;
import com.framework.tools.gus.ws.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpUrlConnection;

import javax.xml.namespace.QName;

@RequestScope
@Component
public class GusSoapClient extends WebServiceGatewaySupport {
    private static final Logger logger = LoggerFactory.getLogger(GusSoapClient.class);
    @Setter
    private String url;

    @Getter
    private String sessionId;

    @Autowired
    private SoapClientConfig.ConnectionConfig connectionConfig;

    public static class GusServiceMsg {
        private String msg;
        private String code;
        public boolean isSessionActive(){
            return msg != null && code != null && !msg.isEmpty() && !code.isEmpty();
        }
        public GusServiceMsg(GetValueResponse msgCode, GetValueResponse msg) {
            if(msgCode != null && msgCode.getGetValueResult() != null){
                this.code = msgCode.getGetValueResult();
            }
            if(msg != null && msg.getGetValueResult() != null){
                this.msg = msg.getGetValueResult();
            }
        }

        public String toString(){
            return code + ": " + msg;
        }
    }

    public GusServiceMsg getLastMessage() {
        String act = "http://CIS/BIR/2014/07/IUslugaBIR/GetValue";
        ObjectFactory fac = new ObjectFactory();
        GetValue gv = fac.createGetValue();
        gv.setPNazwaParametru(fac.createGetValuePNazwaParametru("KomunikatKod"));
        GetValueResponse msgCode = (GetValueResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, gv, new CustomHeader(act, null));

        GetValue gvMsg = fac.createGetValue();
        gvMsg.setPNazwaParametru(fac.createGetValuePNazwaParametru("KomunikatTresc"));
        GetValueResponse msg = (GetValueResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, gvMsg, new CustomHeader(act, null));
        return new GusServiceMsg(msgCode, msg);
    }

    public boolean  login(String key) {
        if(StringUtils.isEmpty(this.url)){
            throw new IllegalStateException("Url not configured - select test or prod server first");
        }
        ObjectFactory fac = new ObjectFactory();
        Zaloguj zal = fac.createZaloguj();
        zal.setPKluczUzytkownika(fac.createZalogujPKluczUzytkownika(key));
        ZalogujResponse zr = (ZalogujResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, zal, new CustomHeader("http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/Zaloguj", null));
        this.sessionId = zr.getZalogujResult().getValue();
        return StringUtils.isEmpty(this.sessionId);
    }

    public boolean logout() {
        if(StringUtils.isEmpty(this.sessionId)){
            throw new IllegalStateException("No session id - login first");
        }
        ObjectFactory fac = new ObjectFactory();
        Wyloguj wyloguj = fac.createWyloguj();
        wyloguj.setPIdentyfikatorSesji(fac.createWylogujPIdentyfikatorSesji(this.sessionId));
        WylogujResponse resp = (WylogujResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, wyloguj, new CustomHeader("http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/Wyloguj", this.sessionId));
        if(resp == null){
            throw new RuntimeException("Empty response in logout");
        }
        return resp.isWylogujResult();
    }

    public String searchCompany(String nip) {
        if(StringUtils.isEmpty(this.sessionId)){
            throw new IllegalStateException("No session id - login first");
        }
        ObjectFactory fac = new ObjectFactory();
        DaneSzukajPodmioty request = fac.createDaneSzukajPodmioty();
        ParametryWyszukiwania params = fac.createParametryWyszukiwania();
        params.setNip(fac.createParametryWyszukiwaniaNip(nip));
        request.setPParametryWyszukiwania(fac.createDaneSzukajPodmiotyPParametryWyszukiwania(params));
        DaneSzukajPodmiotyResponse res = (DaneSzukajPodmiotyResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, request,  new CustomHeader("http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/DaneSzukajPodmioty", this.sessionId));
        if(res != null && res.getDaneSzukajPodmiotyResult() != null){
            return res.getDaneSzukajPodmiotyResult().getValue();
        }
        return null;
    }

    private String searchForPKD(String regon, boolean osobaFizyczna) {
        if(StringUtils.isEmpty(this.sessionId)){
            throw new IllegalStateException("No session id - login first");
        }
        ObjectFactory fac = new ObjectFactory();
        DanePobierzPelnyRaport param = fac.createDanePobierzPelnyRaport();
        String reportName = osobaFizyczna?"BIR12OsFizycznaPkd":"BIR12OsPrawnaPkd";

        logger.info("searchForPKD Running report  {} for regon {}", reportName, regon);
        param.setPNazwaRaportu(fac.createDanePobierzPelnyRaportPNazwaRaportu(reportName));
        param.setPRegon(fac.createDanePobierzPelnyRaportPRegon(regon));
        DanePobierzPelnyRaportResponse resp = (DanePobierzPelnyRaportResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, param,  new CustomHeader("http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/DanePobierzPelnyRaport", this.sessionId));

        if(resp != null && resp.getDanePobierzPelnyRaportResult() != null &&
                !StringUtils.isEmpty(resp.getDanePobierzPelnyRaportResult().getValue())){
            return resp.getDanePobierzPelnyRaportResult().getValue();
        }
        else {
            throw new RuntimeException("Full report returned empty response");
        }
    }

    public CompanyInfo getCompanyFullData(String nip) {
        if(StringUtils.isEmpty(this.sessionId)){
            throw new IllegalStateException("No session id - login first");
        }
        String compXml = this.searchCompany(nip);
        if(StringUtils.isEmpty(compXml)){
            throw new RuntimeException("No data returned for company " + nip);
        }
        CompanyInfo ci = new CompanyInfo(compXml);

        if(ci.isError()){
            throw new GusException(ci.getErrorCode(), ci.getErrorMessagePl());
        }

        ObjectFactory fac = new ObjectFactory();
        DanePobierzPelnyRaport param = fac.createDanePobierzPelnyRaport();

        String reportName = getReportName(ci.Typ, ci.SilosID);
        logger.info(" getCompanyFullData Running report {} for regon {}", reportName, ci.Regon);
        param.setPNazwaRaportu(fac.createDanePobierzPelnyRaportPNazwaRaportu(reportName));
        param.setPRegon(fac.createDanePobierzPelnyRaportPRegon(ci.Regon));
        DanePobierzPelnyRaportResponse resp = (DanePobierzPelnyRaportResponse) getWebServiceTemplate()
                .marshalSendAndReceive(url, param,  new CustomHeader("http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/DanePobierzPelnyRaport", this.sessionId));

        if(resp != null && resp.getDanePobierzPelnyRaportResult() != null &&
                !StringUtils.isEmpty(resp.getDanePobierzPelnyRaportResult().getValue())){
            ci.mergeFullReport(resp.getDanePobierzPelnyRaportResult().getValue());
        }
        else{
            throw new RuntimeException("Full report returned empty response");
        }

        if(!StringUtils.isEmpty(ci.Regon)) {
            String pkdXml = searchForPKD(ci.Regon, ci.czyOsobaFizyczna());
            ci.listaPKD.mergeReport(pkdXml, this.connectionConfig.getPkdVet());
        }
        else {
            logger.warn("Regon not found in compny info - PKD report not loaded");
        }

        return ci;
    }

    private final class CustomHeader implements WebServiceMessageCallback {

        private String soapAction;
        private String sid;


        public CustomHeader(String soapAction, String sid) {
            super();
            this.soapAction = soapAction;
            this.sid = sid;
        }

        @Override
        public void doWithMessage(WebServiceMessage message) {
            try {
                SoapHeader soapHeader = ((SoapMessage) message).getSoapHeader();

                SoapMessage soapMessage = (SoapMessage) message;
                soapMessage.setSoapAction(soapAction);

                QName wsa = new QName("http://www.w3.org/2005/08/addressing", "", "wsa");
                soapHeader.addHeaderElement(new QName(wsa.getNamespaceURI(), "Action", wsa.getPrefix()))
                        .setText(soapAction);
                soapHeader.addHeaderElement(new QName(wsa.getNamespaceURI(), "To", wsa.getPrefix()))
                        .setText(url);

                if(sid != null && !sid.isEmpty()){
                    TransportContext context = TransportContextHolder.getTransportContext();
                    HttpUrlConnection conn = (HttpUrlConnection) context.getConnection();
                    conn.addRequestHeader("sid", sid);
                }
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }

        }
    }

    /*
	 * „Algorytm” jak otrzymać pełny adres.
        A) Jeśli podmiot to osoba prawna (<Typ>P</Typ> w odpowiedzi z DaneSzukaj )
        wtedy adres zwróci raport PublDaneRaportPrawna.

        B) Jeśli podmiot to osoba fizyczna (<Typ>F</Typ> w odpowiedzi z DaneSzukaj )
        to:
        1) jeśli podmiot prowadzi działalność zarejestrowaną w CEIDG (<SilosID>1</SilosID>  w odpowiedzi z DaneSzukaj)
        wtedy adres zwróci raport PublDaneRaportDzialalnoscFizycznejCeidg

        2) jeśli podmiot jest rolnikiem (<SilosID>2</SilosID> w odpowiedzi z DaneSzukaj)
        wtedy adres zwróci raport PublDaneRaportDzialalnoscFizycznejRolnicza

        3) jeśli podmiot prowadzi działalność inną niż 1) i 2)  (<SilosID>3</SilosID> w odpowiedzi z DaneSzukaj)
        wtedy adres zwróci raport PublDaneRaportDzialalnoscFizycznejPozostala

        4) jeśli działalność osoby fizycznej została skreślona z rejestru REGON w poprzednim systemie informatycznym (w którym nie było rozróżnienia typów działalności), tj. przed datą 08.11.2014 (<SilosID>4</SilosID> w odpowiedzi z DaneSzukaj)
        wtedy adres zwróci raport PublDaneRaportDzialalnoscFizycznejWKrupgn
        (Uwaga: dana osoba fizyczna mogła w międzyczasie założyć nową działalność gospodarczą, wtedy odpowiedź będzie wg punktów 1-3)

        C) Jeśli podmiot to jednostka lokalna osoby prawnej (<Typ>LP</Typ> w odpowiedzi z DaneSzukaj )
        wtedy adres zwróci PublDaneRaportLokalnaPrawnej

        D) Jeśli podmiot to jednostka lokalna podmiotu osoby fizycznej (<Typ>LF</Typ> w odpowiedzi z DaneSzukaj )
        wtedy adres zwróci PublDaneRaportLokalnaFizycznej
	 */

    private String getReportName(String type, String silos){
        if(StringUtils.isEmpty(type)){
            throw new IllegalArgumentException("Type is empty");
        }
        if(StringUtils.isEmpty(silos)){
            throw new IllegalArgumentException("Silos is empty");
        }
        if("P".equals(type)) {
            // A
            return "BIR12OsPrawna";
        }
        else if("F".equals(type)){
            //B
            if("1".equals(silos)) {
                return "BIR12OsFizycznaDzialalnoscCeidg";
            }
            else if("2".equals(silos)){
                return "BIR12OsFizycznaDzialalnoscRolnicza";
            }
            else if("3".equals(silos)){
                return "BIR12OsFizycznaDzialalnoscPozostala";
            }
            else if("4".equals(silos)) {
                return "BIR12OsFizycznaDzialalnoscSkreslona";
            }
        }
        else if("LP".equals(type)){
            //C
            return "BIR12JednLokalnaOsPrawnej";
        }
        else if("LF".equals(type)) {
            //D
            return "BIR12JednLokalnaOsFizycznej";
        }
        throw new IllegalStateException("Raport name not found for type " + type + " and silos " + silos);
    }



}
