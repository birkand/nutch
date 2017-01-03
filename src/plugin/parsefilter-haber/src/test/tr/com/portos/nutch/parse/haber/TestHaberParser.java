package tr.com.portos.nutch.parse.haber;


import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.ParseUtil;
import org.apache.nutch.protocol.Content;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.NutchConfiguration;

import java.util.Properties;
import java.io.*;
import java.net.URL;

import junit.framework.TestCase;
import tr.com.portos.nutch.indexingfilter.haber.HaberIndexingFilter;

/*
 * Loads test page recommended.html and verifies that the recommended
 * meta tag has recommended-content as its value.
 *
 */
public class TestHaberParser extends TestCase {

  private static final File testDir =
    //new File(System.getProperty("test.data"));
    new File("/backup/aselsan-poc/nutch-master2/src/plugin/parsefilter-haber/data");

  public void testPages() throws Exception {
	  
	    Configuration conf = NutchConfiguration.create();

	    //String file = SAMPLES + SEPARATOR + "regex-parsefilter.txt";
	    //RegexParseFilter filter = new RegexParseFilter(file);
	    //filter.setConf(conf);
//	    HaberParseFilter filter = new HaberParseFilter();
//	    filter.setConf(conf);
//
//	    String url = "http://nutch.apache.org/";
//	    String html = "<body><html><h1>nutch</h1><p>this is the extracted text blablabla</p></body></html>";
//	    Content content = new Content(url, url, html.getBytes("UTF-8"), "text/html", new Metadata(), conf);
//	    Parse parse = new ParseImpl("nutch this is the extracted text blablabla", new ParseData());
//	    
//	    ParseResult result = ParseResult.createParseResult(url, parse);
//	    result = filter.filter(content, result, null, null);
//
//	    Metadata meta = parse.getData().getParseMeta();
//	    Metadata metadata = parse.getData().getContentMeta();
	    
	    //assertEquals("true", meta.get("first"));
	    //assertEquals("true", meta.get("second"));
//    pageTest(new File(testDir, "hurriyet.html"), "http://www.hurriyet.com.tr/",
//             "recommended-content");

    /*pageTest(new File(testDir, "dha_haber.html"), "http://www.dha.com.tr/",
             "recommended-content");*/
    
    pageTest(new File(testDir, "hurriyet.html"), "http://www.hurriyet.com.tr/ypgye-katilan-ingiliz-asci-suriyede-olduruldu-40325215",
            "recommended-content");
    
    

  }


  public void pageTest(File file, String url, String recommendation)
    throws Exception {

    String contentType = "text/html";
    InputStream in = new FileInputStream(file);
    ByteArrayOutputStream out = new ByteArrayOutputStream((int)file.length());
    byte[] buffer = new byte[1024];
    int i;
    while ((i = in.read(buffer)) != -1) {
      out.write(buffer, 0, i);
    }
    in.close();
    byte[] bytes = out.toByteArray();
    Configuration conf = NutchConfiguration.create();

    Content content =
            new Content(url, url, bytes, contentType, new Metadata(), conf);


    NutchDocument doc = new NutchDocument();

  /*  TikaParser parser = new TikaParser();
    parser.setConf(conf);

    ParseResult parseResult = parser.getParse(content);
    Parse parse = parseResult.get(content.getUrl());*/

    Parse parse = new ParseImpl(new String(content.getContent()), new ParseData());
    ParseResult parseResult =  ParseResult.createParseResult(url, parse);
    //TODO ParseResult parseResult =  new ParseUtil(conf).parseByExtensionId("parse-html",content);
    HaberParseFilter filter = new HaberParseFilter();
    filter.setConf(conf);
    parseResult = filter.filter(content, parseResult, null,null);

    HaberIndexingFilter indexFilter = new HaberIndexingFilter();
    indexFilter.setConf(conf);

    indexFilter.filter(doc, parse, new Text(url), new CrawlDatum(), new Inlinks());

    Metadata metadata = parseResult.get(url).getData().getContentMeta();

    System.out.println(doc.toString());
//    assertEquals(recommendation, metadata.get("selectorContent"));
    assertTrue(null != metadata.getValues("selectorContent"));
    assertTrue(metadata.getValues("selectorContent").length == doc.getField("selectorContent").getValues().size());
  }
}
