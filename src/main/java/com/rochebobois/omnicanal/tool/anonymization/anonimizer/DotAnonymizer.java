package com.rochebobois.omnicanal.tool.anonymization.anonimizer;

import java.time.LocalDate;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rochebobois.omnicanal.api.Environment;
import com.rochebobois.omnicanal.api.exception.ApiClientException;
import com.rochebobois.omnicanal.api.httpclient.ResponseEntity;
import com.rochebobois.omnicanal.dotdigotal.api.DotDigitalEndpointParameters;
import com.rochebobois.omnicanal.dotdigotal.api.DotDigitalInstance;
import com.rochebobois.omnicanal.dotdigotal.api.DotDigitalRestClient;

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

	public void unsubscribe(String email) {
		logger.debug("trying to unsubscribe email : " + email);
		
		Callable<ResponseEntity<String>> job = dotClient.unsubscribe(email);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("Dot Responde : " + response.getBody());

	}
}
