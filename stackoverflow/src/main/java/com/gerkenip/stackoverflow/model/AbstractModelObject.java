package com.gerkenip.stackoverflow.model;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gerkenip.stackoverflow.SofAccess;
import com.gerkenip.stackoverflow.SofIterator;
import com.gerkenip.stackoverflow.elasticsearch.EsClient;

public abstract class AbstractModelObject {

	protected static  EsClient esClient = EsClient.localClient();
	protected static  SofAccess sofAccess = new SofAccess("stackoverflow");
	
	protected static String baseUrl = "https://api.stackexchange.com/2.1";
	
	public static long DURATION_DAYS_0007 = 7 * 24 * 60 * 60 * 1000;
	public static long DURATION_DAYS_1000 = 1000 * 24 * 60 * 60 * 1000;
	
	private JSONObject _data = null;
	
	public AbstractModelObject(JSONObject jobj) {
		this._data = jobj;
	}

	protected JSONObject getDataObject() {
		return _data;
	}
	
	protected boolean hasValue(String key) {
		try {
			JSONObject jobj = getDataObject(path(key));
			if (jobj == null) {
				return false;
			}
			if (jobj.get(property(key)) == null) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	protected String getStringValue(String key) {
		try {
			return getDataObject(path(key)).getString(property(key));
		} catch (Exception e) {
			return null;
		}
	}
	
	protected int getIntValue(String key) {
		try {
			return getDataObject(path(key)).getInt(property(key));
		} catch (Exception e) {
			return 0;
		}
	}
	
	protected long getLongValue(String key) {
		try {
			return getDataObject(path(key)).getLong(property(key));
		} catch (Exception e) {
			return 0;
		}
	}
	
	protected boolean getBooleanValue(String key) {
		try {
			return getDataObject(path(key)).getBoolean(property(key));
		} catch (Exception e) {
			return false;
		}
	}
	
	protected String[] getStringArrayValue(String key) {
		try {
			JSONArray jarr = getDataObject(path(key)).getJSONArray(property(key));
			String result[] = new String[jarr.length()];
			for (int i = 0; i < jarr.length(); i++) {
				result[i] = jarr.get(i).toString();
			}
			return result;
		} catch (Exception e) {
			return new String[0];
		}
	}

	private JSONObject getDataObject(String path) {
		if (path == null) {
			return getDataObject();
		}
		JSONObject jobj = getDataObject();
		try {
			StringTokenizer st = new StringTokenizer(path, "/");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				jobj = jobj.getJSONObject(token);
			}
			return jobj;
		} catch (Exception e) {
			return null;
		}
	}

	private String path(String key) {
		int index = key.lastIndexOf("/");
		if (index < 0) {
			return null;
		}
		String result = key.substring(0,index);
		return result;
	}

	private String property(String key) {
		int index = key.lastIndexOf("/");
		if (index < 0) {
			return key;
		}
		String result = key.substring(index+1); 
		return result;
	}
	
	public abstract String asHtmlLink();

	protected void cache(String type, String id, long shelfLife) {
		sofAccess.cacheDocument(type, id, _data, shelfLife);
	}
	
	protected static JSONObject[] getMultipleUnderlyingData(String type, String idAttribute, String getUrl, long shelfLife) {
		
		ArrayList<JSONObject> jsons = new ArrayList<JSONObject>();
		SofIterator iter = sofAccess.getMany(getUrl);
		while (iter.hasNext()) {
			JSONObject jobj = iter.next();
			jsons.add(jobj);
			try {
				sofAccess.cacheDocument(type, jobj.getString(idAttribute), jobj, shelfLife);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		JSONObject[] jobj = new JSONObject[jsons.size()];
		jsons.toArray(jobj);
	
		return jobj;
	}

	protected static JSONObject getUnderlyingData(String type, String id, String getUrl, long shelfLife) {
		
		JSONObject jobj =  sofAccess.getDocument(type, id);
		
		if ((jobj == null) || (sofAccess.isExpired(jobj))) {
			jobj = sofAccess.getSingle(getUrl);
			sofAccess.cacheDocument(type, id, jobj, shelfLife);
		}

		if (jobj == null) {
			return null;
		}
		
		return jobj;
	}

	protected static JSONObject[] getUnderlyingData(String type, String id[], String url1, String url2, long shelfLife) {

		ArrayList<JSONObject> jsons = new ArrayList<JSONObject>();
		
		for (int i = 0; i < id.length; i=i+100 ) {
			String delim = "";
			StringBuffer sb = new StringBuffer();
			sb.append(url1);
			for (int j = 0; ((j < 100) && ((i+j)<id.length)); j++) {
				int offset = i + j;
				sb.append(delim);
				sb.append(id[offset]);
				delim = ";";
			}
			sb.append(url2);
			String url = sb.toString();
			SofIterator iter = sofAccess.getMany(url);
			while (iter.hasNext()) {
				jsons.add(iter.next());
			}
		}
		
		JSONObject[] jobj = new JSONObject[jsons.size()];
		jsons.toArray(jobj);
		
		return jobj;
		
	}
	
}
