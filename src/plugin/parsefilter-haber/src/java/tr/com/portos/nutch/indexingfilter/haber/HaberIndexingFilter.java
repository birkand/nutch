package tr.com.portos.nutch.indexingfilter.haber;

import java.util.Collection;
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

		String[] selectorContent = parse.getData().getContentMeta().getValues("selectorContent");
		Boolean olay = Boolean.parseBoolean(parse.getData().getMeta("olay"));
		
		for(String content : selectorContent){
			LOG.info("selectorContent -> {}", content);
			doc.add("selectorContent", content);
		}

		LOG.info("olay -> {}", olay.toString());

		if(parse.getData().getContentMeta().get("knowledge") != null){

			JSONObject knowledge = new JSONObject(parse.getData().getContentMeta().get("knowledge"));
			
			for (Object keyObj : knowledge.keySet()) {
				String key = keyObj.toString();
				Object val  = knowledge.get(key);
				LOG.info("{} -> {}", key, val);
				if( val instanceof String[]){
					String[] values = (String[])val;
					for (int i = 0; i < values.length; i++) {
						doc.add(key, values[i]);
					}
				}
				else if(val instanceof JSONArray){
					JSONArray values = ((JSONArray)val);
					for (int i = 0; i < values.length(); i++) {
						doc.add(key, values.getString(i));
					}
				}
				else if(val instanceof Collection){
					throw new RuntimeException("NOT IMPLEMENTED YET!");
				}
				else {
					doc.add(key, val);
				}
			}
		
		}
		
		return doc;
	}

}
