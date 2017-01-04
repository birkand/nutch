package tr.com.portos.nutch.parse.haber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.util.StringUtils;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.net.URLNormalizer;
import org.apache.nutch.net.URLNormalizers;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.EncodingDetector;
import org.apache.nutch.util.NutchConfiguration;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.xml.sax.InputSource;

import com.ibm.icu.text.SimpleDateFormat;

import java.io.*;
import java.lang.String;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaberParseFilter implements HtmlParseFilter {

//    private static final Logger LOG = LoggerFactory
//            .getLogger(HaberParseFilter.class);

    public static final Logger LOG = LoggerFactory.getLogger(HaberParseFilter.class);

    public static final String CSSSELECTOR_FILE = "parsefilter.haber.cssselectorfile";
    public static final String GEONAMES_REF_FILE = "parsefilter.haber.geonamesfile";

    private static Map geonamesLatLonMap;

    
    
    // I used 1000 bytes at first, but found that some documents have
    // meta tag well past the first 1000 bytes.
    // (e.g. http://cn.promo.yahoo.com/customcare/music.html)
    // NUTCH-2042 (cf. TIKA-357): increased to 8 kB
    private static final int CHUNK_SIZE = 8192;

    // NUTCH-1006 Meta equiv with single quotes not accepted
    private static Pattern metaPattern = Pattern.compile(
        "<meta\\s+([^>]*http-equiv=(\"|')?content-type(\"|')?[^>]*)>",
        Pattern.CASE_INSENSITIVE);
    private static Pattern charsetPattern = Pattern.compile(
        "charset=\\s*([a-z][_\\-0-9a-z]*)", Pattern.CASE_INSENSITIVE);
    private static Pattern charsetPatternHTML5 = Pattern.compile(
        "<meta\\s+charset\\s*=\\s*[\"']?([a-z][_\\-0-9a-z]*)[^>]*>",
        Pattern.CASE_INSENSITIVE);
//    public static final String DICTFILE_MODELFILTER = "parsefilter.naivebayes.wordlist";

  //  public static final String TRAINFILE_MODELFILTER = "parsefilter.naivebayes.trainfile";
  //  public static final String DICTFILE_MODELFILTER = "parsefilter.naivebayes.wordlist";

    private Configuration conf;
    private String inputFilePath;
    private String rawContentOutputDirectory;
    private String dictionaryFile;
    private ArrayList<String> wordlist = new ArrayList<String>();
    private ArrayList<UrlCssSelector> selectors = new ArrayList<UrlCssSelector>();
    private HaberExtractor extractor;
    private SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");

    /*public boolean filterParse(String text) {

        try {
            return classify(text);
        } catch (IOException e) {
            LOG.error("Error occured while classifying:: " + text + " ::"
                    + StringUtils.stringifyException(e));
        }

        return false;
    }*/

    public boolean filterUrl(String url) {

        return containsWord(url, wordlist);

    }

    /*public boolean classify(String text) throws IOException {

        // if classified as relevant "1" then return true
        if (Classify.classify(text).equals("1"))
            return true;
        return false;
    }

    public void train() throws Exception {
        // check if the model file exists, if it does then don't train
        if (!FileSystem.get(conf).exists(new Path("naivebayes-model"))) {
            LOG.info("Training the Naive Bayes Model");
            Train.start(inputFilePath);
        } else {
            LOG.info("Model file already exists. Skipping training.");
        }
    }*/

    public boolean containsWord(String url, ArrayList<String> wordlist) {
        for (String word : wordlist) {
            if (url.contains(word)) {
                return true;
            }
        }

        return false;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
        this.defaultCharEncoding = getConf().get(
                "parser.character.encoding.default", "UTF-8");
        inputFilePath = conf.get(CSSSELECTOR_FILE);
        if(inputFilePath == null || inputFilePath.trim().length() == 0){
            String message = "ParseFilter: Haber: cssSelectorsFile not set in the parsefilter.haber.cssselectorfile";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        try{
            FileSystem fs = FileSystem.get(conf);
            BufferedReader br = null;

            String CurrentLine;
            Reader reader = conf.getConfResourceAsReader(inputFilePath);
            br = new BufferedReader(reader);
            while ((CurrentLine = br.readLine()) != null) {
                if(!CurrentLine.startsWith("#") && !CurrentLine.trim().isEmpty()) {
                    String[] line = CurrentLine.split("\t");
                    String urlRegex = line[0];
                    String cssSelector = line[1];
                    Pattern pattern = Pattern.compile(urlRegex);
                    selectors.add(new UrlCssSelector(pattern, cssSelector));
                    LOG.info("successfully parsed pattern, selector pair -> {}", line.toString());
                }
            }
            
            rawContentOutputDirectory = getConf().get("parsefilter.haber.rawContentOutputDirectory", null);

            Reader reader2 = conf.getConfResourceAsReader("TR_all_names_lines.json");
            br = new BufferedReader(reader);
//            List<String> allPlaces = IOUtils.readLines( new FileInputStream("/backup/aselsan-poc/TR_all_names_lines.json"));
            List<String> allPlaces = IOUtils.readLines( reader2);
            List<JSONObject> places = new ArrayList<JSONObject>();
            HashMap<String,String> placesMap = new HashMap<String, String>();
            List<String> refFeatureCodes = Arrays.asList(new String[]{"PPLA", "PPLA2", "PPLC"});
            for(String placeJson: allPlaces){
                JSONObject place = new JSONObject(placeJson);
                places.add(place);
                if(refFeatureCodes.contains(place.getString("featureCode"))){
                    placesMap.put(place.getString("name"), place.getString("location"));
                }
            }
            extractor = new HaberExtractor(placesMap);
//            conf.set("places", new JSONObject(placesMap).toString());
            geonamesLatLonMap = placesMap;


        } catch (IOException e) {
            LOG.error(StringUtils.stringifyException(e));
        }
    }

    public Configuration getConf() {
        return conf;
    }


    @Override
    public ParseResult filter(Content content, ParseResult parseResult,
                              HTMLMetaTags metaTags, DocumentFragment doc) {

        Parse parse = parseResult.get(content.getUrl());
        Metadata metadata = parse.getData().getContentMeta();
        
        try {

            byte[] contentInOctets = content.getContent();
            InputSource input = new InputSource(new ByteArrayInputStream(
                    contentInOctets));
            ByteArrayInputStream bis = new ByteArrayInputStream(
                    contentInOctets);

            EncodingDetector detector = new EncodingDetector(conf);
            detector.addClue(sniffCharacterEncoding(contentInOctets), "sniffed");
            String encoding = detector.guessEncoding(content, defaultCharEncoding).toUpperCase(Locale.ENGLISH);

            metadata.set(Metadata.ORIGINAL_CHAR_ENCODING, encoding);
            metadata.set(Metadata.CHAR_ENCODING_FOR_CONVERSION, encoding);

            //FIXME burada olay yeri adaylari ile geonames veritabanini karsilastir ve lat long degerlerini indeksle
            //FIXME burada olay yeri adaylari ile geonames veritabanini karsilastir ve lat long degerlerini indeksle
            Map crawlMap = new HashMap<String,Object>();

            JSONObject crawledPage = new JSONObject();
            crawledPage.put("id", content.getUrl());
            crawledPage.put("content", parse.getText());



            String selectedContent = null;
        	
            String url = content.getUrl();
            String cssSelector = null;

            for(UrlCssSelector selector: selectors){
                Matcher m = selector.urlPattern.matcher(url);
                if(m.find()){
                    LOG.info("matched pattern {} for url {}, will use css selector for content -> {}", selector.urlPattern.pattern(), url, selector.cssSelector);
                    cssSelector = selector.cssSelector;
                    break;
                }
            }

            if(cssSelector != null) {
            	String test = new String(content.getContent(), encoding/*, "UTF-8"*/);
                Document origDoc = Jsoup.parse(test);

                Elements elts = origDoc.select(cssSelector);
                if(elts.isEmpty()){
                    LOG.error("could not find matching content with css selector: {}", cssSelector);
                    LOG.debug("unmatched content body: {}", origDoc.body().html());
                }
                else{
                	StringBuffer sb = new StringBuffer();	
                	
	                for (Element elt : elts) {
	                	String txt = elt.text();
	                    //String selectorContent = new String(elt.html().replaceAll("<script[^<]*>.*<\\/script>","").replaceAll("<[^<]*>","").getBytes("UTF-8"));
//	                    String selectorContent = elt.html().replaceAll("<script[^<]*>.*<\\/script>","").replaceAll("<[^<]*>","").replaceAll("\n","");
	                    sb.append(elt.text()).append(" ");
	                    LOG.debug("parsed selectorContent ({}): {}",encoding, elt.text());
	                    metadata.add("selectorContent", elt.text());
	                }
	                selectedContent = sb.toString();
                }

            }
            else{
                LOG.error("cannot find a cssSelector for url: {}", url);
                //throw new IllegalArgumentException("cannot find a valid cssSelector for url: "+ url);
            }

//            HaberExtractor extractor = new HaberExtractor();
            
            if(selectedContent != null && selectedContent.trim().length() != 0){

	            Map<String,Object> knowledge = extractor.process(selectedContent);
	
	            metadata.set("knowledge", new JSONObject(knowledge).toString());
	            //TODO write haber json and knowledge merged to file
	            //FIXME config property for output directory path
	            
	            for(Map.Entry<String,Object> entry : knowledge.entrySet()){
	            	crawledPage.put(entry.getKey(), entry.getValue());
	            }
	            //TODO selector content
            }

            String normalizedUrl = content.getUrl().replaceAll("(http:|\\/)", "") + ".json";
            String contentOutputDirPath =  "/backup/aselsan-poc/news/pages/single";
            /*String crawlDir = fmt.format(new Date());*/
            File contentOutputDir = new File(contentOutputDirPath/*, crawlDir*/);
            /*contentOutputDir.mkdirs();*/
            File f1 = new File(contentOutputDir,normalizedUrl);
            IOUtils.write(crawledPage.toString(),
                    new FileOutputStream(f1));
            //FIXME encoding??
            LOG.info("saved merged page json -> {}", f1.getAbsoluteFile().getPath());
            
            
            if(rawContentOutputDirectory != null){
            	String fileName = content.getUrl().replaceAll("http|[:\\/]", "")+".txt";
            	LOG.info("saving raw html file (encoding:{})-> {}", encoding,fileName);
            	File f = new File(rawContentOutputDirectory, fileName);
            	FileOutputStream fos = new FileOutputStream(f);
//            	OutputStreamWriter os = new OutputStreamWriter(fos, "UTF-8");
//            	//FileWriter fw = new FileWriter(f);
//            	IOUtils.write(content.getContent(), os);
            	//IOUtils.write(new String(content.getContent(), "UTF-8"), new FileOutputStream(new File(rawContentOutputDirectory,"1.txt")));
            	//IOUtils.write(new String(content.getContent(), "ISO-8859-9"), new FileOutputStream(new File(rawContentOutputDirectory,"2.txt")));
            	IOUtils.write(new String(content.getContent(), encoding.toUpperCase(Locale.ENGLISH)), 
            			new FileOutputStream(new File(rawContentOutputDirectory,fileName)));
            	//IOUtils.write(new String(content.getContent(), "WINDOWS-1254"), new FileOutputStream(new File(rawContentOutputDirectory,"3.txt")));
            	//IOUtils.write(new String(content.getContent(), "ASCII"), new FileOutputStream(new File(rawContentOutputDirectory,"4.txt")));
            	//IOUtils.write(new String(content.getContent(), encoding), fos);
            	LOG.info("saved raw file -> {}, ", f.getAbsoluteFile().getPath());
            }
        }
        catch(Exception e){
            e.printStackTrace();
            LOG.error("", e);
        }

        return parseResult;
    }
    
    /**
     * Given a <code>byte[]</code> representing an html file of an
     * <em>unknown</em> encoding, read out 'charset' parameter in the meta tag
     * from the first <code>CHUNK_SIZE</code> bytes. If there's no meta tag for
     * Content-Type or no charset is specified, the content is checked for a
     * Unicode Byte Order Mark (BOM). This will also cover non-byte oriented
     * character encodings (UTF-16 only). If no character set can be determined,
     * <code>null</code> is returned. <br />
     * See also
     * http://www.w3.org/International/questions/qa-html-encoding-declarations,
     * http://www.w3.org/TR/2011/WD-html5-diff-20110405/#character-encoding, and
     * http://www.w3.org/TR/REC-xml/#sec-guessing
     * 
     * @param content
     *          <code>byte[]</code> representation of an html file
     */

    private static String sniffCharacterEncoding(byte[] content) {
      int length = content.length < CHUNK_SIZE ? content.length : CHUNK_SIZE;

      // We don't care about non-ASCII parts so that it's sufficient
      // to just inflate each byte to a 16-bit value by padding.
      // For instance, the sequence {0x41, 0x82, 0xb7} will be turned into
      // {U+0041, U+0082, U+00B7}.
      String str = new String(content, 0, length, StandardCharsets.US_ASCII);

      Matcher metaMatcher = metaPattern.matcher(str);
      String encoding = null;
      if (metaMatcher.find()) {
        Matcher charsetMatcher = charsetPattern.matcher(metaMatcher.group(1));
        if (charsetMatcher.find())
          encoding = new String(charsetMatcher.group(1));
      }
      if (encoding == null) {
        // check for HTML5 meta charset
        metaMatcher = charsetPatternHTML5.matcher(str);
        if (metaMatcher.find()) {
          encoding = new String(metaMatcher.group(1));
        }
      }
      if (encoding == null) {
        // check for BOM
        if (content.length >= 3 && content[0] == (byte) 0xEF
            && content[1] == (byte) 0xBB && content[2] == (byte) 0xBF) {
          encoding = "UTF-8";
        } else if (content.length >= 2) {
          if (content[0] == (byte) 0xFF && content[1] == (byte) 0xFE) {
            encoding = "UTF-16LE";
          } else if (content[0] == (byte) 0xFE && content[1] == (byte) 0xFF) {
            encoding = "UTF-16BE";
          }
        }
      }

      return encoding;
    }

    private String defaultCharEncoding;

}
