package tr.com.portos.nutch.parse.haber;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by birkan on 22.12.2016.
 */
public class HaberExtractor {
	
	public static final Logger LOG = LoggerFactory.getLogger(HaberExtractor.class);

    private final Map<String,String> geonamesLatLonMap;

    public HaberExtractor(Map geonamesMap) {
        geonamesLatLonMap = geonamesMap;
    }

//    public HaberExtractor(){}

    public JSONObject processHTML(String html){
        Document myDoc = Jsoup.parse(html);
        //TODO configurable css selector wrt to url (dha.com.tr, aa.com.tr, www.hurriyet.com.tr)
        //FIXME DONE in Haber Parse Filter move that code  to some service
        Elements elts = myDoc.select("div.icerikbaslik, div.icerikyazi"); //dha.com.tr
        
        String content = null;
        StringBuffer sb = new StringBuffer();
        if(!elts.isEmpty()){
        	
	        for(Element elt: elts){
	        	String text = elt.html().replaceAll("<.*>"," ");
	        	sb.append(text)/*.append("\n")*/;
	        }
	        
        }
        else{
        	elts = myDoc.select("div.article header, div.article div.article-post-content"); //aa.com.tr
        	if(!elts.isEmpty()){
        		sb = new StringBuffer();
    	        for(Element elt: elts){
    	        	String text = elt.html().replaceAll("<.*>"," ");
    	        	sb.append(text)/*.append("\n")*/;
    	        }
        	}
        	else{
        		elts = myDoc.select("div[class^=\"news-detail-\"]");//www.hurriyet.com.tr
        		sb = new StringBuffer();
    	        for(Element elt: elts){
    	        	String text = elt.html().replaceAll("<.*>"," ");
    	        	sb.append(text)/*.append("\n")*/;
    	        }
        	}
        }
        
        String regExText = sb.toString();
        regExText = regExText.replaceAll("<script[^<]*>.*<\\/script>","");
        System.out.println(regExText);
        
        regExText = regExText.replaceAll("<[^<]*>","");
       // System.out.println(elt.html().replaceAll("<[^<]*>"," "));
        System.out.println(regExText);
        
        LOG.info("regexText -> {}", regExText);
        
        content = regExText;
        
        JSONObject jo = new JSONObject();
        jo.put("content", content);
        return jo;
    }
	
	
	
	

//	    public JSONObject processHTML(String html, String baseUri) throws IOException {
//	        //Document myDoc = Jsoup.parse(html,  baseUri);
//	       // Document myDoc = Jsoup.parse(html);
//	        Document myDoc = Jsoup.connect(baseUri).get();
//
//	        //TODO configurable css selector
//	        Elements elts = myDoc.select("div.icerikbaslik, div.icerikyazi");
//	        for(Element elt: elts){
//	            System.out.println(elt.html().replaceAll("<.*>"," "));
//	        }
//	        return null;
//	    }
	
	
	//public Map<String, String> processOlay()
	

    public Map<String,Object> process(String content){

    	Map<String,Object> haber = new HashMap<>();
        String source = content;

        Pattern ldJSONregex = Pattern.compile("<script type=\"application\\/ld\\+json\">([^<>]*\"@type\": \"NewsArticle\"[^<>]*)<\\/script>{1}");


        Pattern keywordsInJsonLdRegex = Pattern.compile("\"keywords\":\\s?\"([^\"]*)\"");
        Pattern headlineInJsonLdRegex = Pattern.compile("\"headline\":\\s?\"([^\"]*)\"");
        Pattern datePublishedInJsonLdRegex = Pattern.compile("\"datePublished\":\\s?\"([^\"]*)\"");
        Pattern dateModifiedInJsonLdRegex = Pattern.compile("\"dateModified\":\\s?\"([^\"]*)\"");
        Pattern descriptionInJsonLdRegex = Pattern.compile("\"description\":\\s*\"([^\"]*)\"");
        Pattern articleBodyInJsonLdRegex = Pattern.compile("\"articleBody\":\\s*\"([^\"]*)\"");

        Pattern sehitRegex = Pattern.compile("(\\d+)\\s[a-zşöçüığ]*\\s*(şehit)");
        Pattern yaraliRegex = Pattern.compile("(\\d+)\\s[a-zşöçüığ]*\\s*(yaralı|yaralandı)");
        Pattern olayYeriRegex = Pattern.compile("([A-ZŞÖÇÜİ][a-zşöçüığ]+[-]?[\\s]*([A-ZŞÖÇÜİ]?[a-zşöçüığ]*))('da|'de|'ta|'te)\\s");

        /*String sehitAskerPolisRegex = ".+\\s(\\S+)('da|'de|'ta|'te)\\s(\\d)\\D+(asker|polis)\\D+(şehit)";
        String yaraliAskerPolisRegex = ".+\\s(\\S+)('da|'de|'ta|'te)\\s(\\d)\\D+(asker|polis)\\D+(yaralı|yaralandı)";
*/


        JSONObject info = new JSONObject();
        
        Matcher m = ldJSONregex.matcher(source);

        if(m.find()) {//can only match once (mostly hurriyet.com.tr matches)  
            source = m.group(1);
            LOG.debug("source -> {}", source);
            System.out.println(source);

            m = keywordsInJsonLdRegex.matcher(source);

            List<String> keywords = new ArrayList<String>();
            while (m.find()) {
                System.out.println(m.group(1));
                keywords.add(m.group(1));
            }
            LOG.info("ld_keywords -> {}", Arrays.toString(keywords.toArray(new String[]{})));
            haber.put("ld_keywords",  keywords.toArray(new String[]{}));

            m = headlineInJsonLdRegex.matcher(source);
            if (m.find()) {
                haber.put("ld_headline", m.group(1));
                System.out.println(m.group(1));
                LOG.info("ld_headline -> {}", m.group(1));
            }

            m = datePublishedInJsonLdRegex.matcher(source);
            if (m.find()) {
                info.put("ld_datePublished", m.group(1));
                haber.put("ld_datePublished", m.group(1));
                System.out.println(m.group(1));
                LOG.info("ld_datePublished -> {}", m.group(1));
            }

            m = dateModifiedInJsonLdRegex.matcher(source);
            if (m.find()) {
                haber.put("ld_dateModified", m.group(1));
                System.out.println(m.group(1));
                LOG.info("ld_dateModified -> {}", m.group(1));
            }

            m = descriptionInJsonLdRegex.matcher(source);
            if (m.find()) {
                haber.put("ld_description", m.group(1));
                System.out.println(m.group(1));
                LOG.info("ld_description -> {}", m.group(1));
            }
            
            m = articleBodyInJsonLdRegex.matcher(source);
            if (m.find()) {
                haber.put("ld_articleBody", m.group(1));
                System.out.println(m.group(1));
                LOG.info("ld_articleBody -> {}", m.group(1));
            }

        }else{
        	//parse dha, hurriyet, aa without json+ld
        	//already DONE in HaberParseFilter with custom css selectors
        	
        }


        m = sehitRegex.matcher(source);
        if(m.find()){
        	haber.put("haber_sehit",Integer.parseInt(m.group(1)));
            LOG.info("haber_sehit -> {}",Integer.parseInt(m.group(1)));
        }
        else{
        	haber.put("haber_sehit",0);
        }

        m = yaraliRegex.matcher(source);
        if(m.find()){
        	haber.put("haber_yarali", Integer.parseInt(m.group(1)));
            LOG.info("haber_yarali -> {}", Integer.parseInt(m.group(1)));
        }else{
        	haber.put("haber_yarali", 0);
        }

        m = olayYeriRegex.matcher(source);
        List<String> olayYeri = new ArrayList<String>();
        List<String> olayYeriLatLon = new ArrayList<String>();
        while(m.find()) {
            String olayYeriStr = m.group(1).trim();

            if (olayYeriStr.length() > 0) {
                if (geonamesLatLonMap.get(olayYeriStr) != null) {
                    olayYeri.add(olayYeriStr);
                    olayYeriLatLon.add(geonamesLatLonMap.get(olayYeriStr));
                }
            }
        }

        haber.put("haber_olayYeriLatLon",  olayYeriLatLon.toArray(new String[]{}));

        haber.put("haber_olayYeri", olayYeri.toArray(new String[]{}));
        
        LOG.info("haber_olayYeri -> {}", Arrays.toString(olayYeri.toArray(new String[]{})));
        LOG.info("haber_olayYeriLatLon -> {}", Arrays.toString(olayYeriLatLon.toArray(new String[]{})));

        return haber;

    }
}
