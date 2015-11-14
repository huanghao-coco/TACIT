package edu.usc.cssl.tacit.common.queryprocess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

class Attribute {
	String key = null;
	QueryDataType dataType =  null;
	public Attribute(String key, QueryDataType dataType) {
		this.key = key;
		this.dataType = dataType;
	}
	public Attribute() {		
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public QueryDataType getDataType() {
		return dataType;
	}
	public void setDataType(QueryDataType d) {
		this.dataType = d;
	}	
}
public class JsonParser {
	public JsonParser(String[] keyList) {
		for (int i = 0; i < keyList.length; i++) {
			keyList[i] = keyList[i].trim();
		}
		keyList.clone();
	}
	public JsonParser() {
	}

	public ArrayList<Attribute> findJsonStructure(String filePath) {
		ArrayList<Attribute> resultAttr = new ArrayList<Attribute>();
		try {
			getKeysFromJson(filePath, resultAttr);
		} catch (JsonSyntaxException e) {			
			e.printStackTrace();
		} catch (JsonIOException e) {			
			e.printStackTrace();
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		}
		return resultAttr;
	}
	
	private void getKeysFromJson(String fileName, ArrayList<Attribute> resultAttr) throws JsonSyntaxException, JsonIOException, FileNotFoundException  {
	    Object things = new Gson().fromJson(new FileReader(fileName), Object.class);
	    collectAllTheKeys(things, resultAttr, null);
	 }

	public static List<String> getParentKeys(String jsonFileName) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		Object jsonData = new Gson().fromJson(new FileReader(jsonFileName), Object.class);
		List<String> parentKeys = new ArrayList<String>();
		if (jsonData instanceof Map)
	    {
	    	Map<?, ?> map = (Map<?,?>) jsonData;
	    	for (Object key : map.keySet()) {
	    		parentKeys.add(key.toString());
	    	}
	    } else if(jsonData instanceof Collection) {
	    	for (Object key : (Collection<?>)jsonData) {
	    		parentKeys.add(key.toString());
	    	}
	    } else {
	    	// TODO: ?? 
	    }
		
		return parentKeys;		
	}
	private ArrayList<Attribute> collectAllTheKeys(Object o, ArrayList<Attribute> resultAttr, String parent) {
		if (o instanceof Map)
	    {
	    	Map<?, ?> map = (Map<?,?>) o;
	    	for (Object key : map.keySet()) {
		    	if (!(map.get(key) instanceof Map) && !(map.get(key) instanceof Collection)){
		    		Attribute attr = new Attribute();
		    		setDataType(attr, map.get(key));

		    		if(null != parent) attr.setKey(parent + "." + key.toString());
		    		else attr.setKey(key.toString());
		    		resultAttr.add(attr);
		    	} else if(map.get(key) instanceof Map) {
		    		if(null == parent) collectAllTheKeys(map.get(key), resultAttr, key.toString());
		    		else collectAllTheKeys(map.get(key), resultAttr, parent + "." + key.toString());
		    	} else if(map.get(key) instanceof Collection) {
		    		if(null == parent) collectAllTheKeys(map.get(key), resultAttr, key.toString());
		    		else collectAllTheKeys(map.get(key), resultAttr, parent + "." + key.toString());		    		
		    	}
	    	}
	    } else if(o instanceof Collection) {
	    	for (Object key : (Collection<?>)o) {
		    	if (!(key instanceof Map) && !(key instanceof Collection)){
		    		Attribute attr = new Attribute();
		    		setDataType(attr, key);		    		
		    		if(null != parent) attr.setKey(parent + "." + key.toString());
		    		else attr.setKey(key.toString());
		    		resultAttr.add(attr);
		    	} else if(key instanceof Map) {
		    		collectAllTheKeys(key, resultAttr, parent);
		    	} else if(key instanceof Collection) {
		    		if(null == parent) collectAllTheKeys(key, resultAttr, key.toString());
		    		else collectAllTheKeys(key, resultAttr, parent + "." + key.toString());		    	}
		    	break;
	    	}	    	
	    } else {
	    	// TODO: ?? 
	    }
	    return resultAttr;
	 }
  	
	private void setDataType(Attribute attr, Object key) {
		if (key instanceof Double) 
			attr.setDataType(QueryDataType.DOUBLE);
		else if(key instanceof String)
			attr.setDataType(QueryDataType.STRING);
		else if(key instanceof Integer)
			attr.setDataType(QueryDataType.INTEGER);
		else
			attr.setDataType(QueryDataType.STRING); // TODO : as of now, for default cases		
	}
	
	public static void main(String[] args) {
		JsonParser jh = new JsonParser();
		//HashMap<String, String> jsonKeys = jh.findJsonStructure("C:\\Program Files (x86)\\eclipse\\json_corpuses\\reddit\\REDDIT_1443138695389\\Dummy\\test.json");
		ArrayList<Attribute> jsonKeys = jh.findJsonStructure("C:\\Program Files (x86)\\eclipse\\json_corpuses\\reddit\\REDDIT_1443138695389\\Dummy\\test.json");
		for(Attribute attr : jsonKeys) {
			System.out.println(attr.key + "->"+ attr.dataType);
		}
	}
}
