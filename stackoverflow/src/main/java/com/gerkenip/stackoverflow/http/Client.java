package com.gerkenip.stackoverflow.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.codec.binary.Base64;

public class Client {

	private HashMap<String, String> props = new HashMap<String,String>();
    private String charset   = "UTF-8";
    
    private int httpResponseCode = 0;
    private byte[] response = new byte[0];
    private long lastCall = 0;
	
	public Client() {
		setContentType("text/json");
	}

    public void setAuthentication(String username, String password) {
    	String auth = username + ":" + password;
    	auth = "Basic " +  new String(Base64.encodeBase64(auth.getBytes()));
    	props.put("Authorization", auth);
    }

	public void setContentType(String contentType) {
		props.put("Content-Type", contentType);
	}

	public void setProperty(String name, String value) {
		props.put(name, value);
	}

	public int doPost(String postUrl) throws IOException {
		return doPost(postUrl,new byte[0]);
	}

	public int doPost(String postUrl, byte[] content) throws IOException {

		HttpURLConnection conn = getConnection("POST",postUrl);
		conn.setRequestProperty("Accept-Charset", charset);

        Iterator<String> keys = props.keySet().iterator();
        while (keys.hasNext()) {
        	String key = keys.next();
        	String value = props.get(key);
            conn.setRequestProperty(key,value);
        }

        conn.setDoOutput(true);
		conn.getOutputStream().write(content);
        getResponse(conn);
        return httpResponseCode;
	}
	
	public int doGet(String getUrl) throws IOException {

		HttpURLConnection conn = getConnection("GET", getUrl);
		getResponse(conn);
        return httpResponseCode;

	}

	public byte[] getResponse() {
		return response;
	}

	private void getResponse(HttpURLConnection connection) throws IOException {
		
		response = new byte[0];
		InputStream is = null;
		
		is = connection.getInputStream();
		httpResponseCode = connection.getResponseCode();
		if ((200 <= httpResponseCode) && (httpResponseCode <= 299)) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();			
			byte b[] = new byte[8000];
			int read = is.read(b);
			while (read > -1) {
				os.write(b, 0, read);
				read = is.read(b);
			}
			response = os.toByteArray();
		}
        
        try { is.close(); } catch (Throwable t) { }

	}
	
    protected HttpURLConnection getConnection(String verb, String urlString) throws IOException {
    	if (minTime() > 0) {
    		long now = System.currentTimeMillis();
    		long msToWait = lastCall + minTime() - now;
    		if (msToWait > 0) {
    			try { Thread.sleep(msToWait); } catch (InterruptedException e) { }
    		}
    		lastCall = System.currentTimeMillis();
    	}
    	URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(verb);
        connection.setReadTimeout(1000 * 60 * 60);
        connection.setConnectTimeout(1000 * 10);

        return connection;
    }

	protected long minTime() {
		return 0;
	}
	
}
