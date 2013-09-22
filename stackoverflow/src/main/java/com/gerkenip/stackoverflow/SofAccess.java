package com.gerkenip.stackoverflow;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gerkenip.stackoverflow.elasticsearch.EsClient;
import com.gerkenip.stackoverflow.elasticsearch.EsCursor;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsDocumentDoesNotExistException;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsServerException;
import com.gerkenip.stackoverflow.http.StackoverflowClient;

public class SofAccess {

	EsClient 			esClient = EsClient.localClient();
	StackoverflowClient soClient = new StackoverflowClient();
	
	private String index;
	private static String access_token = null;
	private static String application_key = null;

	public SofAccess(String index) {
		this.index = index;
		try {
			if (!esClient.indexExists(index)) {
				esClient.createIndex(index);
			}
		} catch (EsServerException e) {
			e.printStackTrace();
		}
	}

	public synchronized static void configure(String anAccessToken, String anApplicationKey) {
		access_token = anAccessToken;
		application_key = anApplicationKey;
	}
	
	public static boolean isConfigured() {
		return access_token != null;
	}
	
	public synchronized JSONObject getDocument (String type, String key) {
		try {
			String doc = esClient.getDocument(index, type, key);
			JSONObject jobj = new JSONObject(doc);
			return jobj;
		} catch (EsServerException e) {
			return null;
		} catch (EsDocumentDoesNotExistException e) {
			return null;
		} catch (JSONException e) {
			return null;
		}
	}

	public synchronized void cacheDocument(String type, String id, JSONObject jobj, long shelfLife) {
		try {
			setCacheExpiration(jobj,shelfLife);
			esClient.putDocument(index, type, id, jobj.toString());
			esClient.sendBulkInserts();
		} catch (EsServerException e) {

		}
	}
	
	public synchronized JSONObject[] queryCache(String query) {
		EsCursor cursor = esClient.getDocuments(index, query);
		
		ArrayList<JSONObject> list = new ArrayList<JSONObject>();
		while (cursor.hasNext()) {
			try { list.add(cursor.nextJsonObject()); } catch (JSONException e) { }
		}
		JSONObject data[] = new JSONObject[list.size()];
		list.toArray(data);
		return data;
	}

	public JSONObject getSingle(String url) {

		SofIterator iter = getMany(auth(url));
		if (iter.hasNext()) {
			return iter.next();
		}
		return null;
		
	}

	public synchronized SofIterator getMany(String url) {
		return new SofIterator(soClient, auth(url));
	}
	
	private void setCacheExpiration(JSONObject jobj, long shelfLife) {
		long exp = System.currentTimeMillis() + shelfLife;
		try { 
			jobj.put("_cg_exp", exp);
		} catch (Exception e) { }
	}
	
	public boolean isExpired(JSONObject jobj) {
		try { 
			long exp = jobj.getLong("_cg_exp");
			return System.currentTimeMillis() >= exp;
		} catch (JSONException e) { }
		return true;
	}
	
	private String auth(String url) {
		if (url.indexOf("&access_token") > -1) {
			return url;
		}
		return url + "&access_token="+access_token+"&key="+application_key;
	}

}
