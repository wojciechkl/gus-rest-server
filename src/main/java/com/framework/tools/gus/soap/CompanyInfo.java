package com.framework.tools.gus.soap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.framework.tools.gus.rest.GusController;
import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Data
public class CompanyInfo {
    private static final Logger logger = LoggerFactory.getLogger(CompanyInfo.class);

    public String NIP = "";
    
    public String Nazwa = "";
    
    public String Regon = "";
    
    public String KRS = "";
    
    public String Wojewodztwo = "";
    
    public String Powiat = "";
    
    public String Gmina = "";
    
    public String Miejscowosc = "";
    
    public String KodPocztowy = "";
    
    public String Ulica = "";
    
    public String Posesja = "";
    
    public String Lokal = "";
    
    public String Typ = "";
    
    public String SilosID ="";
    
    public String organRejestrowy = "";
    
    public PKDList listaPKD = new PKDList();
    
    public String ErrorCode;
    
    public String ErrorMessagePl;

    private String getNormalizedRegon(String regonParam){
        if(regonParam != null){
            if(regonParam.endsWith("00000")){
                return regonParam.replace("00000", "");
            }
        }
        return regonParam;
    }

    private void fillField(String name, String value) {
        try {
            name = name.toLowerCase();
            for (Field f : this.getClass().getDeclaredFields()) {
                if (f.getName().toLowerCase().equals(name)) {
                    f.set(this, value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompanyInfo(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new java.io.ByteArrayInputStream(xml.getBytes("utf-8"))));
            Element root = doc.getDocumentElement();
            Node dane = root.getElementsByTagName("dane").item(0);
            NodeList nl = dane.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    String name = n.getNodeName();
                    fillField(name, n.getTextContent());
                }
            }
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public List<String> normalizePKDs(PKDList ps){
        List<String> resPkd = new ArrayList<>();
        for(PKD pkd: ps.getList()) {
            resPkd.add(pkd.kod);
        }
        return resPkd;
    }

    private String getField(Node root, String field){
        NodeList list = root.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String name = n.getNodeName();
                if(field.equalsIgnoreCase(name)) {
                    if(n.getTextContent() != null) {
                        return n.getTextContent().trim();
                    }
                    else{
                        return "";
                    }
                }
            }
        }
        return null;
    }

    private String getFieldPrefix(String type) {
        if("P".equals(type) || "LP".equals(type)){
            return "praw_";
        }
        else{
            return "fiz_";
        }
    }

    private String computeFieldName(String name){
        return getFieldPrefix(this.Typ) + name;
    }

    public void mergeFullReport(String repXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new java.io.ByteArrayInputStream(repXml.getBytes("utf-8"))));
            Element root = doc.getDocumentElement();
            Node dane = root.getElementsByTagName("dane").item(0);
            this.KRS = getField(dane, computeFieldName("numerWrejestrzeEwidencji"));
            this.Posesja = getField(dane, computeFieldName("adSiedzNumerNieruchomosci"));
            this.Lokal = getField(dane, computeFieldName("adSiedzNumerLokalu"));
            this.organRejestrowy = getField(dane, computeFieldName("organRejestrowy_Nazwa"));
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public boolean czyOsobaPrawna() {
        return "P".equals(this.Typ) || "LP".equals(this.Typ);
    }

    public boolean czyOsobaFizyczna() {
        return !czyOsobaPrawna();
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @ToString
    @Data
    public static class PKD{
        public String kod;
        public String nazwa;
        public String przewazajace;
    }

    @Data
    public static class PKDList{
        private List<PKD> list;
        private boolean dzialalnoscWeterynaryjna;
        
        public boolean isLoaded() {
            return list != null;
        }

        public void mergeReport(String repXml, List<String> vetPkd) {
           try {
               this.list = new ArrayList<>();
               if (StringUtils.isEmpty(repXml)) {
                   return;
               }
               DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
               DocumentBuilder builder = factory.newDocumentBuilder();
               Document doc = builder.parse(new InputSource(new java.io.ByteArrayInputStream(repXml.getBytes("utf-8"))));
               Element root = doc.getDocumentElement();

               NodeList daneList = root.getElementsByTagName("dane");
               for (int i = 0; i < daneList.getLength(); i++) {
                   Element el = (Element) daneList.item(i);
                   PKD pkd = createPKD(el);
                   this.list.add(pkd);
               }
           }
           catch(Exception e){
               throw new RuntimeException(e);
           }
           for(PKD pkd: this.list){
               if(pkd != null && vetPkd.contains(pkd.kod)){
                   this.dzialalnoscWeterynaryjna = true;
               }
           }
        }

        private PKD createPKD(Element el) {
            PKD pkd = new PKD();
            NodeList childNodes = el.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node n = childNodes.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) n;
                    if(child.getTagName().toLowerCase().endsWith("kod")) {
                        pkd.kod = child.getTextContent();
                    }
                    else if(child.getTagName().toLowerCase().endsWith("nazwa")) {
                        pkd.nazwa = child.getTextContent();
                    }
                    else if(child.getTagName().toLowerCase().endsWith("przewazajace")) {
                        pkd.przewazajace = child.getTextContent();
                    }
                }
            }
            return pkd;
        }

    }

    
    public boolean isError(){
        return StringUtils.isNotEmpty(ErrorCode);
    }


}
