import org.apache.hadoop.util.ToolRunner;
import org.apache.nutch.util.NutchConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class Nutch1XDriver {

	public static void main(String[] args) throws Exception {
//		String index = args[0];
//		String solrUrl = args[2];
		String seedDir = args[0];
		String crawlDir = args[1];
		int numberOfRounds = Integer.parseInt(args[2]);
		// TODO Auto-generated method stub
		
		int res = ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.crawl.Injector(), new String[]{crawlDir+"/crawldb", seedDir});

		if(res != 0){
			return;
		}

		for (int i = 0; i < numberOfRounds; i++) {


			int res2 = ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.crawl.Generator(),
					new String[]{crawlDir + "/crawldb", crawlDir + "/segments", "-topN", "10", "-nofilter"});

			if(res2 != 0){
				return;
			}

			File segmentsDir = new File(crawlDir + "/segments");
			String[] segments = segmentsDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.matches("20[0-9]+");
				}
			});
			Arrays.sort(segments);
			String latestSegmentName = segments[segments.length - 1];
			int res3 = ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.fetcher.Fetcher(), new String[]{crawlDir + "/segments/" + latestSegmentName});
			if(res3 != 0){
				return;
			}
			int res4 = ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.parse.ParseSegment(), new String[]{crawlDir + "/segments/" + latestSegmentName});
			if(res4 != 0){
				return;
			}
			int res5 = ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.crawl.CrawlDb(), new String[]{crawlDir + "/crawldb", crawlDir + "/segments/" + latestSegmentName});
			if(res5 != 0){
				return;
			}
//		__bin_nutch invertlinks "$CRAWL_PATH"/linkdb "$CRAWL_PATH"/segments/$SEGMENT
//
//		echo "Dedup on crawldb"
//		__bin_nutch dedup "$CRAWL_PATH"/crawldb

			int res6 = ToolRunner.run(NutchConfiguration.create(), new org.apache.nutch.indexer.IndexingJob(), new String[]{crawlDir + "/crawldb", crawlDir + "/segments/" + latestSegmentName, seedDir});
//		echo "Cleaning up index if possible"
//		__bin_nutch clean $JAVA_PROPERTIES "$CRAWL_PATH"/crawldb
			//org.apache.nutch.crawl.Injector.main(new String[]{crawlDir+"/crawldb", seedDir});
			//org.apache.nutch.crawl.Generator.main(new String[]{crawlDir+"/crawldb", crawlDir+"/segments", "-topN", "100", "-nofilter"});
		}
	}

}
