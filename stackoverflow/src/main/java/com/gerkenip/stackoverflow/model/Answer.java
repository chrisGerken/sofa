package com.gerkenip.stackoverflow.model;

import org.codehaus.jettison.json.JSONObject;

public class Answer extends AbstractModelObject {

	public Answer(JSONObject jobj) {
		super(jobj);
	}

	public long getQuestionId() {
		return getLongValue("question_id");  
	}

	public long getAnswerId() {
		return getLongValue("answer_id");  
	}

	public long getCreationDate() {
		return getLongValue("creation_date");  
	}

	public boolean isAccepted() {
		return getBooleanValue("is_accepted");  
	}

	public int getScore() {
		return getIntValue("score");  
	}

	public void cache() {
		cache("answer",String.valueOf(getQuestionId()),DURATION_DAYS_0007);
	}
	
	public Question getQuestion() {
		return Question.get(String.valueOf(getQuestionId()));
	}
	
	public int getUpVotes() {
		return getIntValue("up_vote_count");
	}
	
	public int getDownVotes() {
		return getIntValue("down_vote_count");
	}
	
	public int calculateRepEarned() {
		int accepted = 0;
		if (isAccepted()) { accepted = 15; }
		return (getUpVotes() * 10) - (getDownVotes() * 2) + accepted;
	}
	
	@Override
	public String asHtmlLink() {
		return "<a href=\"http://stackoverflow.com/questions/"+getAnswerId()+"\" ref=\"nofollow\">"+getAnswerId()+"</a>";
	}

}
