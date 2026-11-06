package com.rochebobois.omnicanal.tool.anonymization.report;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.tools.CsvUtils;

public class ReportBook {
	private static final Logger logger = LoggerFactory.getLogger(ReportBook.class);

	static ReportBook instance = null;
	
	public static ReportBook getInstance() {
		if (instance == null) {
			instance = new ReportBook();
		}
		return instance;
	}	
	
	Map<ClientEligible, List<Report>> reports = new LinkedHashMap<ClientEligible, List<Report>>();
	
	public void addReport(Report report) {
		List<Report> clientReportList = reports.get(report.getClient());
		if (clientReportList==null) {
			clientReportList = new LinkedList<Report>();
			reports.put(report.getClient(), clientReportList);
		}
		if (!clientReportList.contains(report)) {
			clientReportList.add(report);
		}
	}
	
	public Map<ClientEligible, List<Report>> getReports() {
		return reports;
	}
	
	
	public void writeBook (Path filePath) throws IOException {
		
        List<String> header = List.of("Type", "EcolixCode", "UUID" , "Applicatif" , "Type" , "EcolixCode", "ID" , "Gravity" , "Error");
        List<List<String>> rows = new LinkedList<List<String>>();
		for (List<Report> clientReportList : reports.values()) {
			for (Report report : clientReportList) {
				List<String> reportItem = new LinkedList<String>();
				reportItem.add("Customer");
				reportItem.add(report.getClient().getCodeClient());
				reportItem.add(report.getClient().getUuid());
				reportItem.add(report.getApplication());
				reportItem.add(report.getType().toString());
				reportItem.add(report.getCode());
				reportItem.add(report.getUuid());
				reportItem.add(report.getGravity().toString());
				reportItem.add(report.getError());
				
				rows.add(reportItem);
			}
		}
        String reportFileName = "./reports/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss")) + ".csv";
        CsvUtils.writeCsv(filePath, header, rows, ';', false);
        
        logger.info("File " + reportFileName + " Writen.");

        System.out.println("File " + reportFileName + " Writen.");

	}
}
