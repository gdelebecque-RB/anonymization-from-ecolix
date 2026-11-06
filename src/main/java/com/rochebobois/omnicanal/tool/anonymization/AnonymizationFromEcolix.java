package com.rochebobois.omnicanal.tool.anonymization;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rochebobois.omnicanal.api.Environment;
import com.rochebobois.omnicanal.api.exception.ApiClientException;
import com.rochebobois.omnicanal.tool.anonymization.anonimizer.DotAnonymizer;
import com.rochebobois.omnicanal.tool.anonymization.anonimizer.MyOrdersAnonymizer;
import com.rochebobois.omnicanal.tool.anonymization.anonimizer.SalesForceAnonymizer;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientPartiel;
import com.rochebobois.omnicanal.tool.anonymization.input.AnonymisationCsvReader;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportBook;

public class AnonymizationFromEcolix {
	private static final Logger logger = LoggerFactory.getLogger(AnonymizationFromEcolix.class);

	public static Environment currentEnv = null;
	
	public static void main(String[] args) throws IOException, ApiClientException {
		LocalDate referenceDate = null;
		String ecolixBase = null;
		String filePath = null;
		Boolean fullprocess = null;
		
		for (String arg : args) {
			System.out.println("ARG : " + arg);
			String key = null;
			if (arg.length()>2) {
				key = arg.substring(0, 2);

				switch (key) {
				case "-p":
					
					if (arg.substring(2).equals("Full")) fullprocess = true;
					if (arg.substring(2).equals("Partial")) fullprocess = false;
					
				case "-f":
					filePath = arg.substring(2);
					break;
				case "-d":
					referenceDate = LocalDate.parse(arg.substring(2));
					break;
				case "-b":
					ecolixBase = arg.substring(2);
					break;
				case "-e":
					currentEnv = Environment.valueOf(arg.substring(2));
					break;
	
				default:
					logger.warn("The parameter is not recognized " + arg); 
					break;
				}
			}
		}
		
		if ((currentEnv==null)||(referenceDate==null)||(filePath==null)||(ecolixBase==null)||(fullprocess==null)) {
			logger.info("Missing argument. Use following : \n"
					+ "-p<processType> values : Full / Partial \n"
					+ "-f<FilePath> ex : -fc:\\directory\\filename.ext \n"
					+ "-d<Reference Date> ex : -d2026-01-01\n"
					+ "-b<Ecolix Base> ex : -b64005 (pour cote d'azur Dev)\n"
					+ "-e<Environement> ex : ePreprod (values : Prod, Preprod, Dev)\n"
					+ "");
			System.exit(-1);
		}
		
		logger.info("Progam runs with following parameters : \n"
				+ "Environment : " + currentEnv + "\n"
				+ "Reference Date : " + referenceDate + "\n"
				+ "ProcessType : " + (fullprocess?"Full":"Partial") + "\n"
				+ "File : " + filePath + "\n"
				+ "Base : " + ecolixBase + "\n"
				+ "\n");

		Path eligibleFile = Path.of(filePath);

		SalesForceAnonymizer sfAnonymizer = new SalesForceAnonymizer (currentEnv, referenceDate);
		MyOrdersAnonymizer myOrdersAnonymizer = new MyOrdersAnonymizer(currentEnv, referenceDate);
		DotAnonymizer dqeAnonymizer = new DotAnonymizer(currentEnv, referenceDate);
		
		if (fullprocess) {
			List<ClientEligible> clientsEligibles = AnonymisationCsvReader.readClientsEligibles(eligibleFile);

			for (ClientEligible client : clientsEligibles) {				
				logger.info("Process  " + client.getCodeClient() + " => " + client.getPrenom() + " " + client.getNom());
				sfAnonymizer.AnonymizeCustomer(client);
				
//				myOrdersAnonymizer.AnonymizeCustomerCustomerOrders (client);
				
//				dqeAnonymizer.unsubscribe (client);

			}
			
		} else {
			List<ClientPartiel> clientItems = AnonymisationCsvReader.readClientsPartiels(eligibleFile);
			
			for (ClientPartiel clientItem : clientItems) {
				if (((clientItem.getUuidCc()==null)||(clientItem.getUuidCc().trim().length()==0))&&(!clientItem.getNoCommandeClient().equals("0"))) {
					clientItem.setUuidCc(ecolixBase + "-" + clientItem.getNoCommandeClient());
				}
				if (((clientItem.getUuidDevis()==null)||(clientItem.getUuidDevis().trim().length()==0))&&(!clientItem.getNoDevis().equals("0"))) {
					clientItem.setUuidDevis(ecolixBase + "-" + clientItem.getNoDevis());
				}
				
				if ((clientItem.getUuidDevis()!=null)&&(clientItem.getUuidDevis().trim().length()>0)) {
					logger.info("Process - Anonymize Quote : " + clientItem.getUuidDevis());
					sfAnonymizer.anonymizeQuote (clientItem);
				} else if ((clientItem.getUuidCc ()!=null)&&(clientItem.getUuidCc().trim().length()>0)) {
					logger.info("Process - Anonymize Customer Order : " + clientItem.getUuidCc());
					sfAnonymizer.anonymizeCustomerOrder (clientItem);
					
					List<String> orderIds = new ArrayList<String>();
					orderIds.add(clientItem.getUuidCc());
					myOrdersAnonymizer.AnonymizeCustomerOrders(clientItem, orderIds);
				}
			}
		}
		
        String reportFileName = "./reports/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss")) + ".csv";
		ReportBook.getInstance().writeBook(Path.of(reportFileName));
		
	}


	
}
