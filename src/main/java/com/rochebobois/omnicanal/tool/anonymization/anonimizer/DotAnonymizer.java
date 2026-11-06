package com.rochebobois.omnicanal.tool.anonymization.anonimizer;

import java.time.LocalDate;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rochebobois.omnicanal.api.Environment;
import com.rochebobois.omnicanal.api.exception.ApiClientException;
import com.rochebobois.omnicanal.api.httpclient.HttpStatus;
import com.rochebobois.omnicanal.api.httpclient.ResponseEntity;
import com.rochebobois.omnicanal.dotdigotal.api.DotDigitalEndpointParameters;
import com.rochebobois.omnicanal.dotdigotal.api.DotDigitalInstance;
import com.rochebobois.omnicanal.dotdigotal.api.DotDigitalRestClient;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.report.Report;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportBook;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportGravity;

public class DotAnonymizer {
	private static final Logger logger = LoggerFactory.getLogger(DotAnonymizer.class);

	private static DotDigitalRestClient dotClient = null;

	private Environment currentEnv = null;
	private LocalDate referenceDate = null;
	
	public DotAnonymizer(Environment currentEnv, LocalDate referenceDate) throws ApiClientException {
		setCurrentEnv(currentEnv);
		setReferenceDate(referenceDate);
		
		DotDigitalInstance dotAppInstance = DotDigitalInstance.environment(currentEnv);
		DotDigitalEndpointParameters dotParam = dotAppInstance.getEndpointParameters();
		dotClient = new DotDigitalRestClient(dotParam);
				
	}
	
	public Environment getCurrentEnv() {
		return currentEnv;
	}
	public void setCurrentEnv(Environment currentEnv) {
		this.currentEnv = currentEnv;
	}
	public LocalDate getReferenceDate() {
		return referenceDate;
	}
	public void setReferenceDate(LocalDate referenceDate) {
		this.referenceDate = referenceDate;
	}

	public void unsubscribe(ClientEligible client) {
		String email = client.getFormerEmail().trim();
		logger.debug ("- DotDigital Response - " + "trying to unsubscribe email" + " - " + email);
		
		Callable<ResponseEntity<String>> job = dotClient.unsubscribe(email);
//		ResponseEntity<String> response = null;
//		try {
//			response = job.call();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		logger.debug("Dot Response : " + response.getStatusCode() + " - " + response.getBody());

		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			logger.debug ("- DotDigital Response - " + "email unsubscribtion" + " - FAILED - " + e.getMessage());
			ReportBook.getInstance().addReport (new Report(client,  "DotDigital" , "NewsLetter", email, "", ReportGravity.ERROR, e.getMessage()));
			e.printStackTrace();
		}
		
		if (response.getStatusCode()!=HttpStatus.OK) {
			logger.error ("-> DotDigital Response - " + "email unsubscribtion" + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );
			ReportBook.getInstance().addReport (new Report(client,  "DotDigital" , "NewsLetter", email, "", ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "")));
		} else {
			logger.info ("-> DotDigital Response - " + "email unsubscribtion" + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
			ReportBook.getInstance().addReport (new Report(client,  "DotDigital" , "NewsLetter", email, "Unsubscribed", ReportGravity.OK, null));
		}

	}
}
