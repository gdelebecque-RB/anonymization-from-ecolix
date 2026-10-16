package com.rochebobois.omnicanal.tool.anonymization.report;

public class Report {

	ReportType type = null;
	String id = null;
	public Report(ReportType type, String id) {
		setType(type);
		setId(id);
	}
	
	public ReportType getType() {
		return type;
	}
	public void setType(ReportType type) {
		this.type = type;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
}
