package com.gerkenip.stackoverflow;

import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gerkenip.stackoverflow.http.StackoverflowClient;

public class SofIterator implements Iterator<JSONObject> {

	private StackoverflowClient soClient;
	private String url;
	
	private int page = 1;
	private int pagesize = 100;
	private boolean has_more = true;
	private JSONArray jarr = new JSONArray();
	private int offset = 0;
	private boolean paging = true;
	
	public SofIterator(StackoverflowClient soClient, String url) {
		this.soClient = soClient;
		this.url = url;
		nextPage();
	}

	@Override
	public boolean hasNext() {
		if (moreInJarr()) {
			return true;
		}
		if (has_more) {
			nextPage();
		}
		return moreInJarr();
	}

	@Override
	public JSONObject next() {
		if (!moreInJarr()) {
			nextPage();
		}
		JSONObject curr = null;
		try {
			curr = jarr.getJSONObject(offset);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		offset++;
		return curr;
	}

	@Override
	public void remove() {
	}
	
	private boolean moreInJarr() {
		if (offset >= jarr.length()) {
			return false;
		}
		return true;
	}
	
	private void nextPage() {
		String pagedUrl = url + "&page="+page+"&pagesize="+pagesize;
		int rc;
		try {
			rc = soClient.doGet(pagedUrl);
		} catch (IOException e) {
			rc = 500;
			try {
//				String msg = new String(soClient.getResponse());
//				System.out.println(msg);
			} catch (Exception e1) { }
		}
		
		jarr = new JSONArray();
		has_more = false;
		offset = 0;
		
		if (rc < 300) {
			try {
				JSONObject jobj = new JSONObject(new String(soClient.getResponse()));
				if (jobj.has("items")) {
					jarr = jobj.getJSONArray("items");
					offset = 0;
				}
				if (jobj.has("has_more")) {
					has_more = jobj.getBoolean("has_more");
 					page++;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

}
