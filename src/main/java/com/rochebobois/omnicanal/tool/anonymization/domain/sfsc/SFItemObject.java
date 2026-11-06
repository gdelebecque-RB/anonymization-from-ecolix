package com.rochebobois.omnicanal.tool.anonymization.domain.sfsc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class SFItemObject {
	private static final Logger logger = LoggerFactory.getLogger(SFItemObject.class);

	String url = null;
	String id = null;
	String type = null;
	JsonNode responseNode = null;
	
	public static SFItemObject getResponseObject (JsonNode recordNode) throws Exception {
		String type = null;
		
		JsonNode attributeNode = recordNode.get("attributes");
		if ((attributeNode!=null)&&(!attributeNode.isNull())) {
			JsonNode typeNode = attributeNode.get("type");
			if ((typeNode!=null)&&(!typeNode.isNull())) {
				type = typeNode.asText();
			}
		}

		if (type==null) {
			logger.error ("record does not contains Type node : " + recordNode);		
			return null;
		} else {
			switch (type) {
			case "AccountHistory":
				return new SFItemHistory (recordNode);
			case "Opportunity":
				return new SFItemOpportunity (recordNode);
			case "Quote__c":
				return new SFItemQuote (recordNode);
			case "SupplierOrder__c":
				return new SFItemSupplierOrder (recordNode);
			case "CustomerOrder__c":
				return new SFItemCustomerOrder (recordNode);
			case "Case":
				return new SFItemCase (recordNode);
			case "Communication__c":
				return new SFItemCommunication (recordNode);
			case "EmailMessage":
				return new SFItemEmailMessage (recordNode);

			default:
				logger.error ("record does not contains Type or Type '" + type + "' is unknown : " + recordNode);			
				throw new Exception("Unrecognized type " + type);
			}				
		}
	}
	
	public SFItemObject(JsonNode recordNode) {
		responseNode = recordNode;
		
		JsonNode attributeNode = recordNode.get("attributes");
		if ((attributeNode!=null)&&(!attributeNode.isNull())) {
			JsonNode urlNode = attributeNode.get("url");
			if ((urlNode!=null)&&(!urlNode.isNull())) {
				url = urlNode.asText();
				this.setUrl(url);
			}
			JsonNode typeNode = attributeNode.get("type");
			if ((typeNode!=null)&&(!typeNode.isNull())) {
				type = typeNode.asText();
				this.setType(type);
			}
		}
		
		JsonNode idNode = recordNode.get("Id");
		if ((idNode!=null)&&(!idNode.isNull())) {
			setId(idNode.asText());
		}
	}

	public String getUrl() {
		return url;
	}

	private void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	private void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public String getReference() {
		return getId();
	};	
	
}
