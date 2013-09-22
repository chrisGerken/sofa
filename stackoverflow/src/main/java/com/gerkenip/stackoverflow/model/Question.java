package com.gerkenip.stackoverflow.model;

import org.codehaus.jettison.json.JSONObject;

public class Question extends AbstractModelObject {

	public Question(JSONObject jobj) {
		super(jobj);
	}
	
	public static Question get(long id) {
		return Question.get(String.valueOf(id));
	}
	
	public static Question get(String id) {
		
		String getUrl = baseUrl + "/questions/" + id + "?site=stackoverflow&filter=withbody";

		JSONObject jobj =  getUnderlyingData("question", id, getUrl, DURATION_DAYS_0007);

		if (jobj == null) {
			return null;
		}
		
		return new Question(jobj);
	}
	
	public static Question[] get(String id[]) {
		
		String url1 = baseUrl + "/questions/";
		String url2 = "?site=stackoverflow&filter=withbody";

		JSONObject[] jobj =  getUnderlyingData("question", id, url1, url2, DURATION_DAYS_0007);

		Question question[] = new Question[jobj.length];
		for (int i = 0; i < question.length; i++) {
			question[i] = new Question(jobj[i]);
			question[i].cache();
		}
		
		return question;
	}
	
	public static Question[] get(String tag, long sinceEpochSeconds) {
		
		String url = baseUrl + "/questions?site=stackoverflow&filter=withbody&fromdate="+sinceEpochSeconds+"&tagged="+tag;

		JSONObject[] jobj =  getMultipleUnderlyingData("question", "question_id", url, DURATION_DAYS_0007);

		Question question[] = new Question[jobj.length];
		for (int i = 0; i < question.length; i++) {
			question[i] = new Question(jobj[i]);
			question[i].cache();
		}
		
		return question;
	}
	
	public int getAnswerCount() {
		return getIntValue("answer_count");
	}
	
	public String getBody() {
		return getStringValue("body");
	}
	
	public String getTitle() {
		return getStringValue("title");
	}
	
	public long getId() {
		return getLongValue("question_id");
	}
	
	public int getScore() {
		return getIntValue("score");
	}
	
	public String[] getTags() {
		return getStringArrayValue("tags");
	}

	public boolean hasOwner() {
		return hasValue("owner");
	}
	
	public int getOwnerAcceptRate() {
		return getIntValue("owner/accept_rate");
	}
	
	public int getOwnerReputation() {
		return getIntValue("owner/reputation");
	}
	
	public String getOwnerId() {
		return getStringValue("owner/user_id");
	}
	
	public void cache() {
		cache("question",String.valueOf(getId()),DURATION_DAYS_0007);
	}

	@Override
	public String asHtmlLink() {
		return "<a href=\"http://stackoverflow.com/questions/"+getId()+"\" ref=\"nofollow\">"+getTitle()+"</a>";
	}

}