package edu.usc.cssl.tacit.common.queryprocess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minidev.json.JSONArray;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

public class QueryProcessSample {
	private static void applyFilters(List<Filter> filters, String jsonFilePath) throws FileNotFoundException, IOException, ParseException {
		Set<Object> filteredResults = processFilter(filters, jsonFilePath);
		writeToFile(jsonFilePath, filteredResults);
	}
	
	private static void writeToFile(String jsonFilePath, Set<Object> filteredResults) {
        try {
	    	JSONArray results = new JSONArray(); // convert JSON objects to array //
	        for(Object ob : filteredResults) 
	        	results.add(ob);
        	String filePath = jsonFilePath + ".test.txt";
			FileWriter file = new FileWriter(filePath, true);
	    	Writer writer = new JSONWriter(); // for pretty-printing 	
        	results.writeJSONString(writer);
        	file.write(writer.toString());
    		file.flush();
            file.close();        	
	        System.out.println("Filtered results are stored in " + filePath);
        } catch (IOException e) { }        
	}
	

	private static Set<Object> processFilter(List<Filter> filters, String jsonFilePath) throws FileNotFoundException, IOException, ParseException {
        Set<Object> filteredResults = new HashSet<Object>();
		JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(jsonFilePath));
        JSONObject jsonObject = (JSONObject) obj;
        
		for(Filter f : filters) {
			Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonObject.toJSONString());
			String filterQuery = constructJSONPathQuery(f);
			System.out.println("Query : " + filterQuery);
			JSONArray records =  JsonPath.parse(document).read(filterQuery);
			for(Object ob : records)
				filteredResults.add(ob);
		}
		return filteredResults;
	}

	private static String constructJSONPathQuery(Filter f) {
		String[] queryComponents = f.getTargetName().split("\\.");
		StringBuilder query = new StringBuilder();
		query.append("$.");
		for(int i = 0; i<queryComponents.length-1; i++) { // except the last component
			query.append(queryComponents[i]);
			if(i!= queryComponents.length-2) query.append(".");
		}
		if(f.getTargetType() == QueryDataType.INTEGER || f.getTargetType() == QueryDataType.DOUBLE) 
			query.append("[?(@."+ queryComponents[queryComponents.length-1] + f.getOperationType() + f.filterValue + ")]");
		else if(f.getTargetType() == QueryDataType.STRING) {
			if(f.getOperationType().equals(QueryOperatorType.STRING_EQUALS))
				query.append("[?(@."+ queryComponents[queryComponents.length-1] + "== '" + f.filterValue + "')]");						
		}
		return new String(query);
	}

	
	private static List<Filter> createSampleFilters() {
		List<Filter> filters = new ArrayList<Filter>();
		//Filter f1 = new Filter("comments.score", QueryDataType.supportedOperations(QueryDataType.DOUBLE).get(0), "5", QueryDataType.DOUBLE); // target names should always be valid
		//Filter f2 = new Filter("post.score", QueryDataType.supportedOperations(QueryDataType.DOUBLE).get(0), "50", QueryDataType.DOUBLE);
		//Filter f3 = new Filter("post.score", QueryDataType.supportedOperations(QueryDataType.DOUBLE).get(0), "60", QueryDataType.DOUBLE);
		Filter f4 = new Filter("comments.author", QueryDataType.supportedOperations(QueryDataType.STRING).get(0), "hankbaumbach", QueryDataType.STRING);
		//filters.add(f1);
		//filters.add(f2);
		//filters.add(f3);
		filters.add(f4);
		return filters;
	}	
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParseException {
		String jsonFilePath = "C:\\Program Files (x86)\\eclipse\\json_corpuses\\reddit\\REDDIT_1443138695389\\Dummy\\test.json";
		System.out.println(jsonFilePath);
		List<String> parentKeys = JsonParser.getParentKeys(jsonFilePath);
		List<Filter> filters = createSampleFilters();
		checkFilters(filters, parentKeys);
		applyFilters(filters, jsonFilePath);
	}

	private static void checkFilters(List<Filter> filters, List<String> parentKeys) {
		
	}
}