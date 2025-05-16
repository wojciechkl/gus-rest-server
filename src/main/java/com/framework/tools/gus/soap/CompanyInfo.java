package com.framework.tools.gus.soap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
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
@ToString
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CompanyInfo {
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

    private String getField(Element root, String field){
        NodeList list = root.getElementsByTagName(field);
        if(list.getLength() > 0){
            return list.item(0).getTextContent();
        }
        return "";
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
            this.KRS = getField(root, computeFieldName("numerWrejestrzeEwidencji"));
            this.Posesja = getField(root, computeFieldName("adSiedzNumerNieruchomosci"));
            this.Lokal = getField(root, computeFieldName("adSiedzNumerLokalu"));
            this.organRejestrowy = getField(root, computeFieldName("organRejestrowy_Nazwa"));
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
        @JsonIgnore
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

    @JsonIgnore
    public boolean isError(){
        return StringUtils.isNotEmpty(ErrorCode);
    }
}
