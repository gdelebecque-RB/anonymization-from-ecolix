package com.rochebobois.omnicanal.tool.anonymization.report;

import java.util.HashMap;
import java.util.Map;

public class ReportBook {

	static ReportBook instance = null;
	
	public static ReportBook getInstance() {
		if (instance == null) {
			instance = new ReportBook();
		}
		return instance;
	}

	static Map<String, Report> customers = new HashMap<String, Report>();
	
	public static Report Customer(String ecolixId) {
		Report report = customers.get(ecolixId);
		if (report==null) {
			report = new Report(ReportType.Customer, ecolixId);
			customers.put(ecolixId, report);
		}
		return report;
	}
	
	
}
