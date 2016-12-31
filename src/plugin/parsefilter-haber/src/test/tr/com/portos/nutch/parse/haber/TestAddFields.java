package tr.com.portos.nutch.parse.haber;


import junit.framework.TestCase;
import tr.com.portos.nutch.indexingfilter.haber.HaberIndexingFilter;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.NutchConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/*
 * Loads test page recommended.html and verifies that the recommended
 * meta tag has recommended-content as its value.
 *
 */
public class TestAddFields extends TestCase {

  private static final File testDir =
    //new File(System.getProperty("test.data"));
    new File("/backup/aselsan-poc/nutch-master2/src/plugin/parsefilter-haber/data");

  public void testPages() throws Exception {

    pageTest(new File(testDir, "dha_haber.html"), "http://www.dha.com.tr/",
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

    Parse parse = new ParseImpl(new String(content.getContent()), new ParseData());
    ParseResult parseResult =  ParseResult.createParseResult(url, parse);// new ParseUtil(conf).parseByExtensionId("parse-html",content);
    HaberParseFilter filter = new HaberParseFilter();
    filter.setConf(conf);
    parseResult = filter.filter(content, parseResult, null,null);

    HaberIndexingFilter indexFilter = new HaberIndexingFilter();
    indexFilter.setConf(conf);

    indexFilter.filter(doc, parse, new Text(url), new CrawlDatum(), new Inlinks());
    
    Metadata metadata = parseResult.get(url).getData().getContentMeta();
//    assertEquals(recommendation, metadata.get("selectorContent"));
    assertTrue(null != metadata.getValues("selectorContent"));
    assertTrue(metadata.getValues("selectorContent").length == doc.getField("selectorContent").getValues().size());
  }
}
