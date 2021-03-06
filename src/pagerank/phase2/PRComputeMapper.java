package pagerank.phase2;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;

public class PRComputeMapper extends Mapper<Text, Text, Text, Text> {

	private static final Logger logger = Logger.getLogger(PRComputeMapper.class);
	private static final String DELIM_SINGLE_SPACE = " ";
	private static final String DELIM_PR_LINKS = "###";
	
	@Override
	public void map(Text key, Text value, Context context) throws IOException,
			InterruptedException {

		String[] pageRankData = value.toString().split(DELIM_PR_LINKS);

		String pageRankStr = pageRankData[0].trim();
		String outgoingLinks = "";

		if (pageRankData.length > 1) {
			outgoingLinks = pageRankData[1].trim();
			
			String[] outLinks = outgoingLinks.split(DELIM_SINGLE_SPACE);
			int outLinksCount = outLinks.length;

			for (int i = 0; i < outLinksCount; i++) {
				StringBuffer oVal = new StringBuffer();
				oVal.append(pageRankStr);
				oVal.append(DELIM_PR_LINKS);
				oVal.append(Integer.toString(outLinksCount));
				context.write(new Text(outLinks[i].trim()), new Text(oVal.toString()));
			}
		}

		context.write(key, new Text(outgoingLinks));
	}
}
