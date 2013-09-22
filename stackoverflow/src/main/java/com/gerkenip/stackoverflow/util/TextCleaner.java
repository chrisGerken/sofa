package com.gerkenip.stackoverflow.util;

import java.util.StringTokenizer;

import org.apache.commons.lang3.StringEscapeUtils;

public class TextCleaner {

	public TextCleaner() {
		// TODO Auto-generated constructor stub
	}
	
	public String[] clean(String content) {
		//remove all tags and content for code and pre tags
		// change &amp;  &lt; &gt;
		
		StringBuffer sb = new StringBuffer(content.length());
		boolean write = true;
		boolean inTag = false;
		StringTokenizer st = new StringTokenizer(content.toLowerCase(), "<>", true);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (token.equals("<")) {
				inTag = true;
			} else if (token.equals(">")) {
				inTag = false;
			} else if (inTag && token.trim().equalsIgnoreCase("code")) {
				write = false;
			} else if (inTag && token.trim().equalsIgnoreCase("pre")) {
				write = false;
			} else if (inTag && token.trim().equalsIgnoreCase("/code")) {
				write = true;
			} else if (inTag && token.trim().equalsIgnoreCase("/pre")) {
				write = true;
			} else if (!inTag & write) {
				sb.append(" ");
				sb.append(StringEscapeUtils.unescapeHtml4(token));
			}
			
		}
		
		st = new StringTokenizer(sb.toString()," \t\n\r\f.;:()\"");
		String term[] = new String[st.countTokens()];
		for (int i = 0; i < term.length; i++) {
			term[i] = st.nextToken();
		}
		return term;
	}

}
