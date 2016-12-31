package tr.com.portos.nutch.indexingfilter.haber;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.net.protocols.Response;
import org.apache.nutch.parse.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.com.portos.nutch.parse.haber.HaberExtractor;
//import tr.com.portos.analytics.haber.HaberExtractor;

public class HaberIndexingFilter implements IndexingFilter {
	
	/*public static final String[] ldFields = new String[]{
			"ld.keywords",
			"ld.headline",
			"ld.datePublished",
			"ld.dateModified",
			"ld.description"
	};*/
	
	public static final Logger LOG = LoggerFactory.getLogger(HaberIndexingFilter.class);
	
	private Configuration conf; 

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration arg0) {
		this.conf = arg0;
	}

	@Override
	public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks)
			throws IndexingException {

		// check if LANGUAGE found, possibly put there by HTMLLanguageParser
		//String lang = parse.getData().getParseMeta().get(Metadata.LANGUAGE);

		// check if HTTP-header tels us the language
//		if (lang == null) {
//			lang = parse.getData().getContentMeta().get(Response.CONTENT_LANGUAGE);
//		}
		//LOG.info(parse.getData().toString());

		//JSONObject knowledge = new JSONObject(parse.getData().getContentMeta().get("knowledge"));
		String[] selectorContent = parse.getData().getContentMeta().getValues("selectorContent");
		Boolean olay = Boolean.parseBoolean(parse.getData().getMeta("olay"));

		LOG.info("olay -> {}", olay.toString());

		if(olay){
			doc.add("olay", olay);
		}
		else{
			return null;
			//FIXME olay yoksa indeksleme
		}

		
		HaberExtractor haberExtractor = new HaberExtractor();
		
		Map<String,Object> knowledge = new HashMap<>(); 
		
		for(String content : selectorContent){
			LOG.info("selectorContent -> {}", content);
			Map<String,Object> contentKnowledge =  haberExtractor.process(content);
			knowledge.putAll(contentKnowledge);
			doc.add("selectorContent", content);
		}
		
		for (Map.Entry<String,Object> knowledgeEntry : knowledge.entrySet()) {
			doc.add(knowledgeEntry.getKey(),knowledgeEntry.getValue());
		}
		
		
		/*LOG.info("knowledge -> {}", knowledge);
		
		for(Object keyObj : knowledge.keySet()){
			String key = (String) keyObj;
			Object value = knowledge.get(key);
			if(value instanceof JSONArray){
				JSONArray ja  = (JSONArray) value;
				doc.add(key, ja.join(","));
			}else{
				doc.add(key, knowledge.get(key));
			}
		}*/

		/*for(String content : selectorContent){
			doc.add("content", content);
		}*/
		
		/*for(String key: ldFields){
			String[] values = parse.getData().getContentMeta().getValues(key);
			for(String val: values){
				doc.add(key, val);
			}
		
		}*/
		//LOG.info("jsonLdVals: {}", jsonLdVals);
		
		/*if(jsonLdVals.length > 0){
			for(String jsonLd : jsonLdVals){
				doc.add("jsonLd", jsonLd);
				JSONObject jo = new JSONObject(jsonLd);
				Set<String> keyset = jo.keySet();
				for( String key : keyset){
					Object val = jo.get(key);
					if(val instanceof JSONObject){
						JSONObject jo2 = ((JSONObject)val);
						Set<String> keyset2 = jo2.keySet();
						for( String key2 : keyset2){
							Object val2 = jo2.get(key2);
							doc.add("ld." + key + "." + key2, val2);
						}
					}
					else{
						doc.add("ld." + key, jo.get(key));
					}
				}
				
			}
			
			//doc.add("jsonLdJo", new JSONObject(jsonLd).keys());
		}*/
		//datum.
		
		return doc;
	}

}
