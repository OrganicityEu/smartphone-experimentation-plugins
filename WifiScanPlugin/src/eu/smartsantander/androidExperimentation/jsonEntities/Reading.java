package eu.smartsantander.androidExperimentation.jsonEntities;

import java.io.Serializable;

import com.google.gson.Gson;

public class Reading implements Serializable {
	public static enum Datatype {Integer,Float,String};
	
	private String context;
	private String value;	
	private Datatype type;
	private long timestamp;
	
	public Reading(Datatype t, String val, String context){
		this.type=t;
		this.value=val;
		this.context=context;
		this.timestamp=System.currentTimeMillis();
	}
	
	public String getContext() {
		return context;
	}
	public void setContext(String context) {
		this.context = context;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Datatype getType() {
		return type;
	}
	
	public void setType(Datatype t) {
		this.type = t;
	}
	
	public String toJson(){
		return (new Gson()).toJson(this);
	}
	
	public static Reading fromJson(String json){
		return (new Gson()).fromJson(json, Reading.class);
	}
}
