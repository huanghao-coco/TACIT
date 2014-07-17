package edu.usc.pil.nlputils.plugins.wordcount.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Shell;

import snowballstemmer.PorterStemmer;
import edu.usc.pil.nlputils.plugins.wordcount.utilities.*;

public class WordCount {
	
	private Trie categorizer = new Trie();
	private TreeMap<Integer,String> categories = new TreeMap<Integer, String>();
	private String delimiters;
	private boolean doLower;
	private boolean doStopWords;
	private HashSet<String> stopWordSet = new HashSet<String>();
	private boolean doLiwcStemming = true;
	private boolean doSpss = true;
	private boolean doWordDistribution = true;
	private boolean doSnowballStemming = true;
	PorterStemmer stemmer = new PorterStemmer();
	
	private static Logger logger = Logger.getLogger(WordCount.class.getName());
	
	// Updated function that can handle multiple input files
	public int wordCount(String[] inputFiles, String dictionaryFile, String stopWordsFile, String outputFile, String delimiters, boolean doLower, boolean doLiwcStemming, boolean doSnowBallStemming, boolean doSpss, boolean doWordDistribution) throws IOException{
		int returnCode = -1;
		this.delimiters = delimiters;
		this.doLower = doLower;
		this.doLiwcStemming = doLiwcStemming;
		this.doSpss = doSpss;
		this.doWordDistribution = doWordDistribution;
		this.doSnowballStemming = doSnowBallStemming;
		
		if (stopWordsFile.equals(null) || stopWordsFile.equals(""))
			this.doStopWords=false;
		else
			this.doStopWords=true;
		// An error flag to check the error conditions
		boolean error = false;
		
		// Checking the dictionary
		File dFile = new File(dictionaryFile);
		if (!dFile.exists() || dFile.isDirectory()) {
			logger.warning("Please check the dictionary file path.");
			error = true;
			returnCode = -3;
		}
		
		// Checking the output path
		File oFile = new File(outputFile);
		if (outputFile=="" || oFile.exists() || oFile.isDirectory()) {
			logger.warning("The output file path is incorrect or the file already exists.");
			error = true;
			returnCode = -5;
		}
		
		
		// Checking the spss path
		File spssFile = new File(outputFile+".dat");
				if (outputFile=="" || spssFile.exists() || spssFile.isDirectory()) {
					logger.warning("The SPSS output file path is incorrect or the file already exists.");
					error = true;
					returnCode = -6;
				}
				
		
		// StopWords is optional
		File sFile = new File(stopWordsFile);
		if (doStopWords){
			if (!sFile.exists() || sFile.isDirectory()) {
				logger.warning("Please check the stop words file path.");
				error = true;
				returnCode = -4;
			}
		}
			
		if(error) {
			return returnCode;
		}
		
		// No errors with the output, dictionary and stop-words paths. Start processing.

		long startTime = System.currentTimeMillis();
		buildCategorizer(dFile);
		logger.info("Finished building the dictionary trie in "+(System.currentTimeMillis()-startTime)+" milliseconds.");
		
		// Create Stop Words Set if doStopWords is true
		if (doStopWords){
			startTime = System.currentTimeMillis();
			stopWordSetBuild(sFile);
			logger.info("Finished building the Stop Words Set in "+(System.currentTimeMillis()-startTime)+" milliseconds.");
		}
		
		// Write the titles in the output file.
		buildOutputFile(oFile);
		
		// Write the SPSS file
		if (doSpss)
			buildSpssFile(spssFile);
		
		//categorizer.printTrie();
		//System.out.println(categories);
		//System.out.println(stopWordSet);

		// for each inputFile,
		for (String inputFile: inputFiles) {
			
			File iFile = new File(inputFile);
			if (!iFile.exists() || iFile.isDirectory()){
				logger.warning("Please check the input file path "+inputFile);
				error = true;
				returnCode = -2;
			}
			
			if(error) {
				return returnCode;
			}
			
			System.out.println(inputFile);
			countWords(inputFile, oFile, spssFile);
		}
		
		if (doSpss)
			finalizeSpssFile(spssFile);
		//No errors
		returnCode = 0;
		return returnCode;
	}
	
	
	public void countWords(String inputFile, File oFile, File spssFile) throws IOException{
		File iFile = new File(inputFile);
		logger.info("Current input file - "+inputFile);
		// For calculating Category wise distribution of each word.
		HashMap<String,HashSet<String>> wordCategories = new HashMap<String, HashSet<String>>();
		
		
		// Build a hashmap of the words in the input file
		long startTime = System.currentTimeMillis();	
		BufferedReader br = new BufferedReader(new FileReader(iFile));
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		String currentLine;
		int totalWords = 0;
		int sixltr = 0;
		int noOfLines = 0;
		while ((currentLine = br.readLine()) != null) {
			noOfLines = noOfLines + StringUtils.countMatches(currentLine, ". ");
			noOfLines = noOfLines + 1; // For the final sentence
			int[] i = process(currentLine, map);
			totalWords = totalWords + i[0];
			sixltr = sixltr + i[1];
		}
		br.close();
		logger.info("Total number of words - "+totalWords);
		logger.info("Finished building hashmap in "+(System.currentTimeMillis()-startTime)+" milliseconds.");
		
		// Calculate Category-wise count
		HashMap<String,Integer> catCount = new HashMap<String, Integer>();
		List<Integer> currCategories;
		int dicCount = 0;
		String currCategoryName = "";
		// Search each input word in the trie prefix tree categorizer (dictionary).
		for (String currWord : map.keySet()){
			currCategories = categorizer.query(currWord);
			// If the word is in the trie, update the dictionary words count and the per-category count
			if (currCategories!=null){
				dicCount = dicCount+1;
				for (int i : currCategories) {
					currCategoryName = categories.get(i);
					if (catCount.get(currCategoryName)!=null){
						//catCount.put(currCategoryName, catCount.get(currCategoryName)+1);
						// Add 1 to count the unique words in the category. 
						// Add map.get(currWord), i.e, the num of each word to count total number of words in the category
						catCount.put(currCategoryName, catCount.get(currCategoryName)+map.get(currWord));
					} else {
						catCount.put(currCategoryName, 1);
					}
					
					// Populate the Category Set for each Word
					HashSet<String> currWordCategories = wordCategories.get(currWord);
					if (currWordCategories!=null){
						wordCategories.get(currWord).add(currCategoryName);
					} else {
						currWordCategories = new HashSet<String>();
						currWordCategories.add(currCategoryName);
						wordCategories.put(currWord, currWordCategories);
					}

				}
			}
		}
		// If Word Distribution output is enabled, calculate the values
		if (doWordDistribution)
			calculateWordDistribution(map, catCount, wordCategories, inputFile);
			
		writeToFile(oFile, iFile.getName(), totalWords, totalWords/(float)noOfLines, (sixltr*100)/(float)totalWords, (dicCount*100)/(float)totalWords, catCount);
		if (doSpss)
			writeToSpss(spssFile, iFile.getName(), totalWords, totalWords/(float)noOfLines, (sixltr*100)/(float)totalWords, (dicCount*100)/(float)totalWords, catCount);
	}
	
	public void calculateWordDistribution(HashMap<String,Integer> map, HashMap<String,Integer> catCount, HashMap<String,HashSet<String>> wordCategories, String inputFile) throws IOException{
		File wdFile = new File(inputFile+"_wordDistribution.csv");
		BufferedWriter bw = new BufferedWriter(new FileWriter(wdFile));
		bw.write("Word,Count,");
		StringBuilder toWrite = new StringBuilder();
		
		for (String currCat : catCount.keySet()){
			toWrite.append(currCat+",");
		}
		bw.write(toWrite.toString());
		bw.newLine();
		
		// check for words in wordCategories instead of map because wordCategories has the words that are present in the dictionary
		for(String currWord : wordCategories.keySet()){
			StringBuilder row = new StringBuilder();
			int currWC = map.get(currWord);
			row.append(currWord+","+currWC+",");
			
			for (String currCat : catCount.keySet()){
				// multiplier is 0 if the current word does not belong to the current category
				int multiplier = 0;
				if (wordCategories.get(currWord).contains(currCat)){
					multiplier = 100;	// 100 instead of 1 because the output should be of the form 25%, not 0.25
				}
				row.append( (multiplier * map.get(currWord)) / (float)catCount.get(currCat) +",");
			}
			bw.write(row.toString());
			bw.newLine();
		}
		
		bw.close();
	}
	
	
	// Legacy single-inputfile function
	public int oldWordCount(String inputFile, String dictionaryFile, String stopWordsFile, String outputFile, String delimiters, boolean doLower) throws IOException{
		int returnCode = -1;
		this.delimiters = delimiters;
		this.doLower = doLower;
		File iFile = new File(inputFile);
		File dFile = new File(dictionaryFile);
		File sFile = new File(stopWordsFile);
		File oFile = new File(outputFile);

		boolean error = false;
		if (!iFile.exists() || iFile.isDirectory()) {
			logger.warning("Please check the input file path.");
			error = true;
			returnCode = -2;
		} else if (!dFile.exists() || dFile.isDirectory()) {
			logger.warning("Please check the dictionary file path.");
			error = true;
			returnCode = -3;
		} else if (!sFile.exists() || sFile.isDirectory()) {
			logger.warning("Please check the stop words file path.");
			error = true;
			returnCode = -4;
		} else if (outputFile=="" || oFile.exists() || oFile.isDirectory()) {
			logger.warning("The output file path is incorrect or the file already exists.");
			error = true;
			returnCode = -5;
		} 
		// All the files exist. Start analysis.
		if (!error) {
			long startTime = System.currentTimeMillis();	
			BufferedReader br = new BufferedReader(new FileReader(iFile));
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			String currentLine;
			Integer totalWords = new Integer(0);
			while ((currentLine = br.readLine()) != null) {
				//System.out.println(currentLine);
				int[] i = process(currentLine, map);
				totalWords = totalWords + i[0];
				//System.out.println(map);
			}
			br.close();
			//System.out.println(map);
			logger.info("Total number of words - "+totalWords);
			logger.info("Finished building hashmap in "+(System.currentTimeMillis()-startTime)+" milliseconds.");
			startTime = System.currentTimeMillis();
			buildCategorizer(dFile);
			stopWordSetBuild(sFile);
			//System.out.println(categories);
			//System.out.println(categorizer);
			logger.info("Finished building the dictionary trie in "+(System.currentTimeMillis()-startTime)+" milliseconds.");
			//categorizer.printTrie();
			/*
			Test Data
			 
			System.out.println(categorizer.query("pizza"));
			System.out.println(categorizer.query("pizzahut"));
			System.out.println(categorizer.query("piz"));
			System.out.println(categorizer.query("zero"));
			System.out.println(categorizer.query("yielding"));
			System.out.println(categorizer.query("abhorrence"));
			System.out.println(categorizer.query("determined"));
			System.out.println(categorizer.query("fellow"));
			System.out.println(categorizer.query("january"));
			System.out.println(categorizer.query("maps"));
			System.out.println(categorizer.query("nutrition"));
			System.out.println(categorizer.query("orchestra"));
			System.out.println(categorizer.query("perception"));
			System.out.println(categorizer.query("abomination"));
			System.out.println(categorizer.query("abomin"));
			System.out.println(categorizer.query("abominat"));
			System.out.println(categorizer.query("abominat*"));
			System.out.println(categorizer.query("accept"));
			System.out.println(categorizer.query("acceptr"));
			System.out.println(categorizer.query("acceptra"));
			System.out.println(categorizer.query("accep"));
			System.out.println(categorizer.query("accept*"));
			*/
			HashMap<String,Integer> catCount = new HashMap<String, Integer>();
			List<Integer> currCategories;
			int dicCount = 0;
			String currCategoryName = "";
			
			for (String currWord : map.keySet()){
				currCategories = categorizer.query(currWord);
				if (currCategories!=null){
					dicCount = dicCount+1;
					for (int i : currCategories) {
						currCategoryName = categories.get(i);
						if (catCount.get(currCategoryName)!=null){
							catCount.put(currCategoryName, catCount.get(currCategoryName)+1);
						} else {
							catCount.put(currCategoryName, 1);
						}
					}
				}
				//System.out.println(currWord);
			}
			writeToFile(oFile, iFile.getName(), totalWords, 0, 0, dicCount, catCount);
			returnCode = 0;
		}
		return returnCode;
	}
	
	// Builds the Stop Word Set
	public void stopWordSetBuild(File sFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(sFile));
		String currentLine = null;
		while ((currentLine = br.readLine()) != null ) {
			stopWordSet.add(currentLine.trim().toLowerCase());
		}
		br.close();
	}
	
	public void buildOutputFile(File oFile) throws IOException{
		StringBuilder titles = new StringBuilder();
		titles.append("Filename,WC,WPS,Sixltr,Dic,");
		for (String title : categories.values()){
			titles.append(title+",");
		}
		FileWriter fw = new FileWriter(oFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(titles.toString());
		bw.newLine();
		bw.close();
		logger.info("Created the output File.");
	}
	
	public void buildSpssFile(File spssFile) throws IOException{
		StringBuilder titles = new StringBuilder();
		titles.append("DATA LIST LIST\n/ Filename A(40) WC WPS Sixltr Dic ");
		for (String title : categories.values()){
			titles.append(title+" ");
		}
		titles.append(".\nBEGIN DATA.");
		FileWriter fw = new FileWriter(spssFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(titles.toString());
		bw.newLine();
		bw.close();
		logger.info("Created the SPSS output File.");
	}
	
	public void finalizeSpssFile(File spssFile) throws IOException{
		String end = "END DATA.\n\nLIST.";
		FileWriter fw = new FileWriter(spssFile, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(end);
		bw.newLine();
		bw.close();
		logger.info("Created the SPSS output File.");
	}

	public void writeToSpss(File spssFile, String docName, int totalCount, float wps, float sixltr, float dic, HashMap<String,Integer> catCount) throws IOException{
		StringBuilder row = new StringBuilder();
		row.append("\""+docName+"\""+" "+totalCount+" "+wps+" "+sixltr+" "+dic+" ");
		int currCatCount = 0;
		// Get the category-wise word count and create the comma-separated row string 
		for (String title : categories.values()){
			if (catCount.get(title) == null)
				currCatCount = 0;
			else
				currCatCount = catCount.get(title);
			row.append(((currCatCount*100)/(float)totalCount)+" ");
		}
		// Append mode because the titles are already written. Append a row corresponding to each input file
		FileWriter fw = new FileWriter(spssFile, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(row.toString());
		bw.newLine();
		bw.close();
		logger.info("SPSS File Updated Successfully");
	}

	public void writeToFile(File oFile, String docName, int totalCount, float wps, float sixltr, float dic, HashMap<String,Integer> catCount) throws IOException{
		StringBuilder row = new StringBuilder();
		row.append(docName+","+totalCount+","+wps+","+sixltr+","+dic+",");
		int currCatCount = 0;
		// Get the category-wise word count and create the comma-separated row string 
		for (String title : categories.values()){
			if (catCount.get(title) == null)
				currCatCount = 0;
			else
				currCatCount = catCount.get(title);
			row.append(((currCatCount*100)/(float)totalCount)+",");
		}
		
		// Append mode because the titles are already written. Append a row corresponding to each input file
		FileWriter fw = new FileWriter(oFile, true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(row.toString());
		bw.newLine();
		bw.close();
		logger.info("CSV File Updated Successfully");
	}

	public void buildCategorizer(File dFile) throws IOException {
		BufferedReader br= new BufferedReader(new FileReader(dFile));
		String currentLine=br.readLine();
		if (currentLine == null) {
			logger.warning("The dictionary file is empty");
		}
		if (currentLine.equals("%"))
			while ((currentLine=br.readLine()) != null && !currentLine.equals("%"))
				categories.put(Integer.parseInt(currentLine.split("\t")[0].trim()), currentLine.split("\t")[1].trim());
		
		if (currentLine == null){
			logger.warning("The dictionary file does not have categorized words");
		} else {
			while ((currentLine=br.readLine())!=null) {
				ArrayList<Integer> categories = new ArrayList<Integer>();
				String[] words = currentLine.split(" ");
				for (int i=1; i<words.length; i++){
					categories.add(Integer.parseInt(words[i]));
				}
				// do Stemming or not. if Stemming is disabled, remove * from the dictionary words
				if (doLiwcStemming)
					categorizer.insert(words[0], categories);
				else
					categorizer.insert(words[0].replace("*", ""), categories);
				//categorizer.printTrie();
			}
		}
		br.close();
	}
	
	// Adds words and their corresponding count to the hashmap. Returns total number of words.
	public int[] process(String line, HashMap<String, Integer> map) {
		int ret[] = new int[2];
		int numWords = 0;
		int sixltr = 0;
		//preprocess
		if (doLower)
			line = line.toLowerCase();
		StringTokenizer st = new StringTokenizer(line,delimiters);
		
		while (st.hasMoreTokens()){
			String currentWord = st.nextToken();
			
			//Do Porter2/Snowball Stemming if enabled
			if (doSnowballStemming){
				stemmer.setCurrent(currentWord);
				if(stemmer.stem())
					currentWord = stemmer.getCurrent();
			}
			
			// If stop word, ignore
			if (doStopWords)
				if (stopWordSet.contains(currentWord))
					continue;
			if (currentWord.length()>=6){
				sixltr = sixltr + 1;
			}
			numWords = numWords + 1;
			// can use map.containsKey function. But avoiding two calls with the one below.
			Object value = map.get(currentWord);
			if (value!=null) {
				int i = (int) value;
				map.put(currentWord, i+1);
			} else {
				map.put(currentWord, 1);
			}
		}
		ret[0] = numWords;
		ret[1] = sixltr;
		return ret;
	}
	
}
