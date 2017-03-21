package org.epos.mq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Handler 
{
	public JsonObject jsonParser(String jsonString) {
		JsonParser parser = new JsonParser();
		return parser.parse(jsonString).getAsJsonObject();
	}
	
	public String handle(String message){
		return message;}
	
	public void handlerExecution(){}
	
	public void jsonRefiner(){}
	
	public void updateSourceTarget(){}
}
