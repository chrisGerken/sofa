package com.gerkenip.stackoverflow.model;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gerkenip.stackoverflow.SofIterator;
import com.gerkenip.stackoverflow.elasticsearch.EsCursor;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsIndexDoesNotExistException;

public class User extends AbstractModelObject {

	public User(JSONObject jobj) {
		super(jobj);
	}
	
	public static User get(String id) {
		
		String getUrl = baseUrl + "/users/" + id + "?site=stackoverflow";

		JSONObject jobj =  getUnderlyingData("user", id, getUrl, DURATION_DAYS_0007);

		if (jobj == null) {
			return null;
		}
		
		return new User(jobj);
	}

	public String getId() {
		return getStringValue("user_id");
	}

	public String getDisplayName() {
		return getStringValue("display_name");
	}
	
	public int getReputation() {
		return getIntValue("reputation");
	}
	
	public Answer[] getAnswers() {
		
		JSONObject relCache = sofAccess.getDocument("rel_user_answer", getId());

		String fromdate = "";
		if (relCache != null) {
			try {
				long minutes = relCache.getLong("fromdate");
				fromdate = String.valueOf(minutes) + "&";
			} catch (JSONException e) { }
		}
		String getUrl = baseUrl + "/users/" + getId() + "/answers?"+fromdate+"site=stackoverflow&filter=withbody";
		
		long latest = 0;
		SofIterator iter = sofAccess.getMany(getUrl);
		while (iter.hasNext()) {
			JSONObject jobj = iter.next();
			Answer answer = new Answer(jobj);
			answer.cache();
			if (latest < answer.getCreationDate()) {
				latest = answer.getCreationDate();
			}
		}
		
		if (latest > 0) {
			try {
				relCache = new JSONObject();
				relCache.put("fromdate",(latest+1));
				sofAccess.cacheDocument("rel_user_answer", getId(), relCache, DURATION_DAYS_1000);
			} catch (JSONException e) { }
		}
		
		String query = "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"answer.owner.user_id\":\""+getId()+"\"}}],\"must_not\":[],\"should\":[]}}}";
		JSONObject[] data = sofAccess.queryCache(query);
		Answer answer[] = new Answer[data.length];
		for (int i = 0; i < answer.length; i++) {
			answer[i] = new Answer(data[i]);
		}
		
		return answer;
	}
	
	public Tag[] getTags() {
		
		String getUrl = baseUrl + "/users/" + getId() + "/tags?site=stackoverflow";
		ArrayList<Tag> tags = new ArrayList<Tag>();
		
		long latest = 0;
		SofIterator iter = sofAccess.getMany(getUrl);
		while (iter.hasNext()) {
			try {
				JSONObject jobj = iter.next();
				String name = jobj.getString("name");
				int count = jobj.getInt("count");
				tags.add(new Tag(name,count));
			} catch (JSONException e) { }
		}
		
		Tag[] tag = new Tag[tags.size()];
		tags.toArray(tag);
		return tag;
	}

	@Override
	public String asHtmlLink() {
		return "<a href=\"http://stackoverflow.com/users/"+getId()+"\" ref=\"nofollow\">"+getDisplayName()+"</a>"+" ("+getReputation()+")";
	}

}

