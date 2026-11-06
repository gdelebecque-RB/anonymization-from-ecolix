package com.rochebobois.omnicanal.tool.anonymization.domain.sfsc;

import com.fasterxml.jackson.databind.JsonNode;

public class SFItemHistory extends SFItemObject {

	String field = null;
	public SFItemHistory(JsonNode recordNode) {
		super(recordNode);
		
		JsonNode fieldNode = recordNode.get("Field");
		if ((fieldNode!=null)&&(!fieldNode.isNull())) {
			setField(fieldNode.asText());
		}
	}
	
	public String getField() {
		return field;
	}
	
	public void setField(String field) {
		this.field = field;
	}

	@Override
	public String getReference() {
		return getField();
	}
	
}
