package com.rochebobois.omnicanal.tool.anonymization.anonimizer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rochebobois.omnicanal.api.Environment;
import com.rochebobois.omnicanal.api.exception.ApiClientException;
import com.rochebobois.omnicanal.api.httpclient.HttpStatus;
import com.rochebobois.omnicanal.api.httpclient.ResponseEntity;
import com.rochebobois.omnicanal.domain.AnonymizationStatusEnum;
import com.rochebobois.omnicanal.domain.SalutationEnum;
import com.rochebobois.omnicanal.sfsc.api.SfscEndpointParameters;
import com.rochebobois.omnicanal.sfsc.api.SfscInstance;
import com.rochebobois.omnicanal.sfsc.api.SfscRestClient;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCase;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomer;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomerHelper;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomerOrder;
import com.rochebobois.omnicanal.sfsc.domain.SFSCOpportunity;
import com.rochebobois.omnicanal.sfsc.domain.SFSCQuote;
import com.rochebobois.omnicanal.sfsc.domain.SFSCSupplierOrder;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientPartiel;
import com.rochebobois.omnicanal.tool.anonymization.domain.sfsc.SFItemObject;
import com.rochebobois.omnicanal.tool.anonymization.report.Report;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportBook;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportGravity;
import com.rochebobois.omnicanal.tool.anonymization.tools.CompositeItem;

public class SalesForceAnonymizer {
	private static final Logger logger = LoggerFactory.getLogger(SalesForceAnonymizer.class);

	private static SfscRestClient sfscClient = null;
	private static ObjectMapper mapper = new ObjectMapper();

	private Environment currentEnv = null;
	private LocalDate referenceDate = null;
	private ClientEligible currentCustomer = null;
	
	private Map<String, List<SFItemObject>> objectsItems = new HashMap<String, List<SFItemObject>>();

	private List<String> customerOrderUuids = new ArrayList<String>();
	private List<String> supplierOrderIds = new ArrayList<String>();

	public SalesForceAnonymizer(Environment currentEnv, LocalDate referenceDate) throws ApiClientException {
		setCurrentEnv(currentEnv);
		setReferenceDate(referenceDate);
		
		SfscEndpointParameters sfscParam = SfscInstance.environment(getCurrentEnv()).getEndpointParameters();
		sfscClient = new SfscRestClient(sfscParam);
	}
	
	public static final String CustomerOrdersReference = "CustomerOrder__c";
	public static final String QuotesReference = "Quote__c";
	public static final String OpportunitiesReference = "Opportunity";
	public static final String CasesReference = "Cases";
	public static final String HistoryObject = "AccountHistory";
	public static final String EmailReference = "EmailMessage";
	public static final String CommunicationReference = "Communication__c";

	private void setCurrentEnv(Environment currentEnv) {
		this.currentEnv = currentEnv;
	}
	public Environment getCurrentEnv() {
		return currentEnv;
	}
	
	public void setReferenceDate(LocalDate referenceDate) {
		this.referenceDate = referenceDate;
	}
	
	
	public void AnonymizeCustomer (ClientEligible client) throws JsonMappingException, JsonProcessingException {
		currentCustomer = client;
		
		logger.info ("Salesforce - Search Customer : " + client.getCodeClient() + " - " + client.getNom() + " - " + client.getEmail() + " - " + client.getUuid());
		
		Callable<ResponseEntity<String>> job = sfscClient.getCustomer(client.getUuid());
		
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			logger.debug ("---- Salesforce Response - " + "Search Customer" + " - FAILED - " + e.getMessage());
			ReportBook.getInstance().addReport (new Report(client, "Salesforce" ,  "Customer", client.getCodeClient(), client.getUuid(), ReportGravity.ERROR, e.getMessage()));
			e.printStackTrace();
		}
		if (response.getStatusCode()==HttpStatus.OK ) {
			logger.info ("---- Salesforce Response - " + "Search Customer" + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
		} else {
			logger.error ("---- Salesforce Response - " + "Search Customer" + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );
			ReportBook.getInstance().addReport (new Report(client, "Salesforce" , "Customer", client.getCodeClient(), client.getUuid(), ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "")));
		}
				
		if (response.getStatusCode()==HttpStatus.OK) {
			
			logger.debug (response.getBody());
			JsonNode sfscGetCustomerResponseNode = mapper.readTree(response.getBody());
			
			SFSCCustomer sfscCustomer = SFSCCustomerHelper.getInstance().getSFSCCustomer(sfscGetCustomerResponseNode);
			
//			unsubsciptionToDotDigital.add(sfscCustomer.getPersonEmail());
			
//			String oldFirstName = null;
//			String oldLastName = null;
//			
//			oldFirstName = sfscCustomer.getFirstName();
//			oldLastName = sfscCustomer.getLastName();
					
			sfscCustomer.setSalutation(client.getSalutation());
			sfscCustomer.setFirstName(client.getPrenom());
			sfscCustomer.setLastName(client.getNom());
			sfscCustomer.setDayOfBirth(null);
			sfscCustomer.setMonthOfBirth(null);
			sfscCustomer.setYearOfBirth(null);
			
			sfscCustomer.setBillingStreet(client.getAdressStreetLine1() + "\n" + client.getAdressStreetLine2());
			sfscCustomer.setBillingCity(client.getAdressCity());
			sfscCustomer.setBillingFloorNumber(null);
			sfscCustomer.setBillingHomeType(null);
			sfscCustomer.setBillingLatitude(null);
			sfscCustomer.setBillingLongitude(null);
			sfscCustomer.setBillingPostalCode(client.getAdressPostalCode());
			sfscCustomer.setBillingSpecificConstraints(null);
			
			sfscCustomer.setShippingSalutation(client.getSalutation());
			sfscCustomer.setShippingFirstName(client.getPrenom());
			sfscCustomer.setShippingLastName(client.getNom());
			sfscCustomer.setShippingCompany(client.getNom());

			sfscCustomer.setShippingStreet(client.getAdressStreetLine1() + "\n" + client.getAdressStreetLine2());
			sfscCustomer.setShippingCity(client.getAdressCity());
			sfscCustomer.setShippingFloorNumber(null);
			sfscCustomer.setShippingHomeType(null);
			sfscCustomer.setShippingLatitude(null);
			sfscCustomer.setShippingLongitude(null);
			sfscCustomer.setShippingPostalCode(client.getAdressPostalCode());
			sfscCustomer.setShippingSpecificConstraints(null);
			sfscCustomer.setShippingAccessibility(null);

			sfscCustomer.setPersonEmail(client.getEmail());
			sfscCustomer.setPersonMobilePhone(client.getTelephone1());
			sfscCustomer.setPersonHomePhone(client.getTelephone2());
			sfscCustomer.setShippingMainPhone(client.getTelephone1Livraison());
			sfscCustomer.setShippingOtherPhone(client.getTelephone2Livraison());

			sfscCustomer.setFiscalIdentifier(client.getNom() + client.getPrenom());
			sfscCustomer.setCompanyName(client.getNom());
			sfscCustomer.setNationalRegistrationNumber(null);


			sfscCustomer.setAnonymizationDate(referenceDate);
			sfscCustomer.setAnonymizationStatus(AnonymizationStatusEnum.Anonymized);
			sfscCustomer.setAnonymizationTargetDate(referenceDate);
			sfscCustomer.setVATNumber(null);
			
			sfscCustomer.setOptinCall(false);
			sfscCustomer.setOptinEmail(false);
			sfscCustomer.setOptinNewsletter(false);
			sfscCustomer.setOptinPartner(false);
			sfscCustomer.setOptinPostal(false);
			sfscCustomer.setOptinSMS(false);

			sfscCustomer.setOptinCallModifiedDate(LocalDate.now());
			sfscCustomer.setOptinEmailModifiedDate(LocalDate.now());
			sfscCustomer.setOptinNewsletterModifiedDate(LocalDate.now());
			sfscCustomer.setOptinPartnerModifiedDate(LocalDate.now());
			sfscCustomer.setOptinPostalModifiedDate(LocalDate.now());
			sfscCustomer.setOptinSMSModifiedDate(LocalDate.now());
			

			
			logger.info ("- Salesforce - Update Customer");
			logger.debug (sfscCustomer.toString());
			job = sfscClient.putCustomer(sfscCustomer);
			response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				logger.debug ("---- Salesforce Response - " + "Update Customer" + " - FAILED - " + e.getMessage());
				ReportBook.getInstance().addReport (new Report(client,  "Salesforce" , "Customer", client.getCodeClient(), client.getUuid(), ReportGravity.ERROR, e.getMessage()));
				e.printStackTrace();
			}
			
			if (response.getStatusCode()!=HttpStatus.OK) {
				logger.error ("---- Salesforce Response - " + "Update Customer" + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );
				ReportBook.getInstance().addReport (new Report(client,  "Salesforce" , "Customer", client.getCodeClient(), client.getUuid(), ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "")));
			} else {
				logger.info ("-> Salesforce Response - " + "Update Customer" + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
				ReportBook.getInstance().addReport (new Report(client,  "Salesforce" , "Customer", client.getCodeClient(), client.getUuid(), ReportGravity.OK, null));
			}
			{
				JsonNode sfscCustomerUpdateResponseNode = mapper.readTree(response.getBody());
				
				JsonNode SfAccountIdNode = sfscCustomerUpdateResponseNode.get("SfAccountId");
				
				if ((SfAccountIdNode!=null)&&!(SfAccountIdNode.isNull())) {
					String CustomerAccountId = SfAccountIdNode.asText();
					
					Map<String, CompositeItem> compositeItems = new HashMap<String, CompositeItem>();
					
					compositeItems.put(CustomerOrdersReference, new CompositeItem("Customer Order", CustomerOrdersReference,
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM CustomerOrder__c WHERE Account__c='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \"" + CustomerOrdersReference + "\"\r\n"
							+ "    }"
							+ "   ]\r\n"
							+ "}"));
					
					compositeItems.put(QuotesReference, new CompositeItem("Quote__c", QuotesReference, 
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM Quote__c WHERE Account__c='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \""+QuotesReference+"\"\r\n"
							+ "    }\r\n"
							+ "   ]\r\n"
							+ "}"));
					
					compositeItems.put(OpportunitiesReference, new CompositeItem("Opportunity", OpportunitiesReference, 
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM Opportunity WHERE AccountID='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \""+OpportunitiesReference+"\"\r\n"
							+ "    }\r\n"
							+ "   ]\r\n"
							+ "}"));

					compositeItems.put(CasesReference, new CompositeItem("Case", CasesReference, 
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM Case WHERE AccountID='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \""+CasesReference+"\"\r\n"
							+ "    }\r\n"
							+ "   ]\r\n"
							+ "}"));

					compositeItems.put(HistoryObject, new CompositeItem("AccountHistory", HistoryObject, 
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id, Field, OldValue, NewValue, CreatedDate FROM AccountHistory WHERE AccountId = '" + CustomerAccountId + "'  AND Field in ('FirstName', 'LastName', 'Salutation', 'PersonEmail', 'Email', 'BillingStreet', 'BillingCity', 'BillingPostalCode', 'ShippingStreet', 'ShippingCity', 'ShippingPostalCode', 'PersonHomePhone', 'PersonMobilePhone')\",\r\n"
							+ "      \"referenceId\": \""+HistoryObject+"\"\r\n"
							+ "    }\r\n"
							+ "   ]\r\n"
							+ "}"));

					compositeItems.put(EmailReference, new CompositeItem("EmailMessage", EmailReference, 
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id, Subject, FromAddress, ToAddress, Incoming, MessageDate, ParentId, Parent.CaseNumber FROM EmailMessage WHERE Parent.AccountId = '" + CustomerAccountId + "' ORDER BY MessageDate DESC\",\r\n"
							+ "      \"referenceId\": \""+EmailReference+"\"\r\n"
							+ "    }\r\n"
							+ "   ]\r\n"
							+ "}"));
					
					compositeItems.put(CommunicationReference, new CompositeItem("Communication__c", CommunicationReference, 
							"{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM Communication__c WHERE Account__c='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \""+CommunicationReference+"\"\r\n"
							+ "    }\r\n"
							+ "   ]\r\n"
							+ "}"));
					
					for (Entry<String, CompositeItem> requestEntry : compositeItems.entrySet()) {
						job = sfscClient.callComposite(requestEntry.getValue().getRequest());
						response = null;
						try {
							response = job.call();
						} catch (Exception e) {
							logger.error ("---- Salesforce Response - Get " + requestEntry.getKey() + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );
							ReportBook.getInstance().addReport (new Report(client, "Salesforce" ,  "Customer", client.getCodeClient(), client.getUuid(), ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "")));
							e.printStackTrace();
						}
						
						
						if (response.getStatusCode()!=HttpStatus.OK) {
							logger.error ("-- Salesforce Response - Get " + requestEntry.getKey() + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );
							ReportBook.getInstance().addReport (new Report(client, "Salesforce" ,  "Customer", requestEntry.getKey(), "", ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "")));
						} else {
							logger.info ("-- Salesforce Response - Get " + requestEntry.getKey() + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );
							
							JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
							
							JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
							for (JsonNode linkedObjectList : compositeResponseNode) {
								JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
								if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
									JsonNode bodyNode = linkedObjectList.get("body"); 
									if ((bodyNode!=null)&&(!bodyNode.isNull())) {
										JsonNode recordsNode = bodyNode.get("records"); 
										if ((recordsNode!=null)&&(!recordsNode.isNull())) {
											for (JsonNode recordNode : recordsNode) {
												
												SFItemObject crObject = null;
												try {
													crObject = SFItemObject.getResponseObject(recordNode);
												} catch (Exception e) {
													e.printStackTrace();
												}
												if (crObject!=null) {
													List<SFItemObject> list = objectsItems.get(crObject.getType());
													if (list==null) {
														list = new LinkedList<SFItemObject>();
														objectsItems.put(crObject.getType(), list);
													}
													list.add (crObject);													
												}
												
												
												
//												JsonNode idNode = recordNode.get("Id");
//												String url = null;
//												if ((idNode!=null)&&(!idNode.isNull())) {
//													JsonNode attributeNode = recordNode.get("attributes");
//													if ((attributeNode!=null)&&(!attributeNode.isNull())) {
//														JsonNode urlNode = attributeNode.get("url");
//														if ((urlNode!=null)&&(!urlNode.isNull())) {
//															url = urlNode.asText();
//														}
//													}
//													if (referenceObjectNode.asText().equals(CustomerOrdersReference)) {
//														customerOrderIds.put(idNode.asText(),url);
//													}
//													if (referenceObjectNode.asText().equals(QuotesReference)) {
//														quoteIds.put(idNode.asText(),url);
//													}
//													if (referenceObjectNode.asText().equals(OpportunitiesReference)) {
//														opportunityIds.put(idNode.asText(),url);
//													}
//													if (referenceObjectNode.asText().equals(CasesReference)) {
//														caseIds.put(idNode.asText(),url);
//													}
////													if (referenceObjectNode.asText().equals(HistoryObject)) {
////														historyIds.put(idNode.asText(),url);
////													}
//													if (referenceObjectNode.asText().equals(EmailReference)) {
//														emailIds.put(idNode.asText(),url);
//													}
//													if (referenceObjectNode.asText().equals(CommunicationReference)) {
//														communicationIds.put(idNode.asText(),url);
//													}
//												}
											}
										}
									}
								}
							}						
						}						
					}
					
					List<SFItemObject> sfcolist = objectsItems.get(CustomerOrdersReference);
					List <SFSCCustomerOrder> customerOrderList = getAllCustomerOrders (sfcolist);
					anonymiseCustomerOrders (client, customerOrderList);
					
					List<SFItemObject> qlist = objectsItems.get(QuotesReference);
					List <SFSCQuote> quoteList = getAllQuotes (qlist);
					anonymiseQuotes (client, quoteList);

					List<SFItemObject> olist = objectsItems.get(OpportunitiesReference);
					List <SFSCOpportunity> opportunityList = getAllOpportunities (olist);
					anonymiseOpportunities (client, opportunityList);
					
					List<SFItemObject> clist = objectsItems.get(CasesReference);
					List <SFSCCase> caseList = getAllCases (clist);
					anonymiseCases (caseList);
					
					fillSupplierOrderList (customerOrderList);
					anonymiseSupplierOrders(client, supplierOrderIds);
					
					
					List<SFItemObject> historyList = objectsItems.get(HistoryObject);
					deleteHistory (historyList);
					
					List<SFItemObject> emailList = objectsItems.get(EmailReference);
					deleteEmails (emailList);
					
					List<SFItemObject> communicationList = objectsItems.get(CommunicationReference);					
					deleteCommunications (communicationList);

				}
			}
			
		} else {
			logger.error ("Erreur de récupération du client SFSC");
		}
	}
	
	private void deleteHistory (List<SFItemObject> historyIds) throws JsonMappingException, JsonProcessingException {
		if (historyIds==null) return;
		if (historyIds.size()==0) return;
		
		logger.info ("--- Salesforce - Delete " + historyIds.size() + " History");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();

		for (SFItemObject entry : historyIds) {

			compositeCalls.put  (entry.getId() , new CompositeItem (HistoryObject , entry.getId(), entry ,
					"{"
					+ "\"method\": \"DELETE\"," 
					+ "\"url\": \"" + entry.getUrl() + "\","
					+ "\"referenceId\": \"" + entry.getId() + "\""
					+ "}"));
		}

		if (compositeCalls.size()>0) {
			callCompositeItems(compositeCalls, 24, "History" );
		}
	}
	
	private void deleteEmails(List<SFItemObject> sfItemObjectList) throws JsonMappingException, JsonProcessingException {
		if (sfItemObjectList==null) return;
		if (sfItemObjectList.size()==0) return;
		
		logger.info ("--- Salesforce - Delete " + sfItemObjectList.size() + " Emails");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		for (SFItemObject entry : sfItemObjectList) {
			
			compositeCalls.put  (entry.getId() , new CompositeItem ("Emails" , entry.getId() , entry,
					"{"
					+ "\"method\": \"DELETE\"," 
					+ "\"url\": \"" + entry.getUrl() + "\","
					+ "\"referenceId\": \"" + entry.getId() + "\""
					+ "}"));
		}

		callCompositeItems(compositeCalls, 24, "Emails");

	}
	
//	private void deleteEmails(Map<String, String> emailIds) throws JsonMappingException, JsonProcessingException {
//		if (emailIds.size()==0) return;
//		
//		logger.info ("---- Salesforce - Delete " + emailIds.size() + " Emails");
//		
//		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
//		for (Entry<String, String> entry : emailIds.entrySet()) {
//			
//			compositeCalls.put  (entry.getKey() , new CompositeItem ("Emails" , entry.getKey() , 
//					"{"
//					+ "\"method\": \"DELETE\"," 
//					+ "\"url\": \"" + entry.getValue() + "\","
//					+ "\"referenceId\": \"" + entry.getKey() + "\""
//					+ "}"));
//		}
//
//		callCompositeItems(compositeCalls, 24, "Emails");
//
//	}
//	
	
	private void deleteCommunications(List<SFItemObject> sfItemObjectList) throws JsonMappingException, JsonProcessingException {
		if (sfItemObjectList==null) return;
		if (sfItemObjectList.size()==0) return;
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		logger.info ("---- Salesforce Request - DELETE " + sfItemObjectList.size() + " Emails");
		for (SFItemObject entry : sfItemObjectList) {
			
			compositeCalls.put(entry.getId(), new CompositeItem ("Communication", entry.getId(), entry,
					"{"
					+ "\"method\": \"DELETE\"," 
					+ "\"url\": \"" + entry.getUrl() + "\","
					+ "\"referenceId\": \"" + entry.getId() + "\""
					+ "}"));
		}
		
		callCompositeItems (compositeCalls, 24, "Communication");

	}
	
	
//	private void deleteCommunications(Map<String, String> emailIds) throws JsonMappingException, JsonProcessingException {
//		if (emailIds.size()==0) return;
//		
//		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
//		logger.info ("---- Salesforce Request - DELETE " + emailIds.size() + " Emails");
//		for (Entry<String, String> entry : emailIds.entrySet()) {
//			
//			compositeCalls.put(entry.getKey(), new CompositeItem ("Communication", entry.getKey(), "{"
//					+ "\"method\": \"DELETE\"," 
//					+ "\"url\": \"" + entry.getValue() + "\","
//					+ "\"referenceId\": \"" + entry.getKey() + "\""
//					+ "}"));
//		}
//		
//		callCompositeItems (compositeCalls, 24, "Communication");
//
//	}
//	
	
	private void anonymiseCases(List<SFSCCase> caseList) throws JsonMappingException, JsonProcessingException {
		if (caseList==null) return;
		if (caseList.size()==0) return;
		
		logger.info ("---- Salesforce - Anonymise " + caseList.size() + " Cases");
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();

		for (SFSCCase sfscCase : caseList) {
			sfscCase.setSubject(currentCustomer.getPrenom() + currentCustomer.getNom());
			
			compositeCalls.put(sfscCase.getId(), new CompositeItem ( "Case" ,sfscCase.getId() , sfscCase,
					"{"
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/Case/" + sfscCase.getId() + "\","
					+ "\"referenceId\": \"" + sfscCase.getId() + "\","
					+ "\"body\": {"
					+ "\"Subject\":\""+sfscCase.getSubject()+"\""
					+ "}"
					+ "}"));
		}
		
		callCompositeItems(compositeCalls, 24, "Cases");

	}
	private void anonymiseOpportunities(ClientEligible client, List<SFSCOpportunity> opportunityList) throws JsonMappingException, JsonProcessingException {
		if (opportunityList.size()==0) return;
		
		logger.info ("--- Salesforce - Anonymise " + opportunityList.size() + " Opportunities");
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();

		for (SFSCOpportunity sfscOpportunity : opportunityList) {
			sfscOpportunity.setName(client.getPrenom() + client.getNom());
			
			compositeCalls.put(sfscOpportunity.getId(), new CompositeItem ( "Opportunity" ,sfscOpportunity.getId() , sfscOpportunity , 
					"{"
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/Opportunity/" + sfscOpportunity.getId() + "\","
					+ "\"referenceId\": \"" + sfscOpportunity.getId() + "\","
					+ "\"body\": {"
					+ "\"Name\":\""+sfscOpportunity.getName()+"\""
					+ "}"
					+ "}") );
		}
		
		callCompositeItems(compositeCalls, 24, "Opportunities");

	}

	public void anonymizeQuote(ClientPartiel item) throws JsonMappingException, JsonProcessingException {
		SFSCQuote sfscQuote = getQuote (item.getUuidDevis());
		if (sfscQuote!=null) {
			List<SFSCQuote> quoteList = new ArrayList<SFSCQuote>();
			quoteList.add(sfscQuote);
			anonymiseQuotes (item, quoteList);			
		}
	}
		
	private void anonymiseQuotes(ClientEligible client, List<SFSCQuote> quoteList) throws JsonMappingException, JsonProcessingException {
		if (quoteList.size()==0) return;
		
		logger.info ("--- Salesforce - Anonymise " + quoteList.size() + " Quotes");
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();

		for (SFSCQuote sfscQuote : quoteList) {
			sfscQuote.setInternalObservations__c("");
			sfscQuote.setShippingMainPhone__c (client.getTelephone1());
			sfscQuote.setShippingSecondPhone__c (client.getTelephone2());
			sfscQuote.setShippingCompanyName__c ("");
			sfscQuote.setShippingFirstName__c (client.getPrenom());
			sfscQuote.setShippingLastName__c (client.getNom());
			sfscQuote.setShippingSpecificConstrains__c ("");
			sfscQuote.setObservations__c("");
			sfscQuote.setPaymentLink__c(null);;
			sfscQuote.setName("Anonymized Quote " + client.getNom()+" "+client.getPrenom());
			
			sfscQuote.setInternalObservations__c("");
			sfscQuote.setObservations__c("");
			
			sfscQuote.setBillingSecondPhone__c(client.getTelephone1Livraison());
			sfscQuote.setBillingMainPhone__c(client.getTelephone2Livraison());
			sfscQuote.setBillingSalutation__c(SalutationEnum.Mx);
			sfscQuote.setBillingLastName__c(client.getNom());
			sfscQuote.setBillingFirstName__c(client.getPrenom());
			sfscQuote.setBillingEmail__c(client.getEmail());
			sfscQuote.setBillingAddressFirstLine__c(client.getAdressStreetLine1());
			sfscQuote.setBillingAddressSecondLine__c(client.getAdressStreetLine2());
			sfscQuote.setBillingCity__c(client.getAdressCity());
			sfscQuote.setBillingPostalCode__c(client.getAdressPostalCode());
			sfscQuote.setBillingSecondPhone__c(client.getTelephone1Livraison());
			sfscQuote.setBillingMainPhone__c(client.getTelephone2Livraison());

			sfscQuote.setShippingSalutation__c(SalutationEnum.Mx);
			sfscQuote.setShippingFirstName__c (client.getPrenom());
			sfscQuote.setShippingLastName__c (client.getNom());
			sfscQuote.setShippingAddressFirstLine__c(client.getAdressStreetLine1());
			sfscQuote.setShippingAddressSecondLine__c(client.getAdressStreetLine2());
			sfscQuote.setShippingCity__c(client.getAdressCity());
			sfscQuote.setShippingPostalCode__c(client.getAdressPostalCode());
			sfscQuote.setShippingSpecificConstrains__c ("");
			sfscQuote.setShippingMainPhone__c (client.getTelephone1());
			sfscQuote.setShippingSecondPhone__c (client.getTelephone2());
			sfscQuote.setShippingCompanyName__c ("");
			sfscQuote.setShippingAccessibility__c("");			
			
			sfscQuote.setObservations__c("");
			sfscQuote.setInternalObservations__c("");

			compositeCalls.put(sfscQuote.getId(), new CompositeItem ( "Case" ,sfscQuote.getId() , sfscQuote,
					"{"
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/Quote__c/" + sfscQuote.getId() + "\","
					+ "\"referenceId\": \"" + sfscQuote.getId() + "\","
					+ "\"body\": "
					+ sfscQuote.toPayload()
					+ "}"));
		}
		
		callCompositeItems(compositeCalls, 24, "Cases");

//			composite += ",{"
//					
//					+ "\"method\": \"PATCH\"," 
//					+ "\"url\": \"/services/data/v61.0/sobjects/Quote__c/" + sfscQuote.getId() + "\","
//					+ "\"referenceId\": \"" + sfscQuote.getId() + "\","
//					+ "\"body\": "
//					+ sfscQuote.toPayload()
//					+ "}";
//		}
//		
//		composite = "{\r\n"
//				+ "    \"allOrNone\": false,\r\n"
//				+ "    \"compositeRequest\": [\r\n"
//				+ composite.substring(1)
//				+ "   ]\r\n"
//				+ "}";
//
//		logger.debug ("---- Salesforce - Anonymise Quotes Request : " + composite.replace("\n", "").replace("\r", ""));
//		Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
//		ResponseEntity<String> response = null;
//		try {
//			response = job.call();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		logger.debug ("---- Salesforce Response - Anonymise Quote - End - " + response.getStatusCode() + " - " + response.getBody());		
	}


	private void anonymiseCustomerOrders(ClientEligible client, List<SFSCCustomerOrder> customerOrderList) throws JsonMappingException, JsonProcessingException {
		if (customerOrderList.size()==0) {
			logger.info ("---> Salesforce No Customer Orders to Anonymize");
			return;
		}
		
		logger.info ("--- Salesforce Anonymize " + customerOrderList.size() + " Customer Orders");
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		
		for (SFSCCustomerOrder sfscCustomerOrder : customerOrderList) {

			customerOrderUuids.add(sfscCustomerOrder.getExternalId__c());
			
			sfscCustomerOrder.setAnonymizationDate__c (LocalDateTime.now());
			
			sfscCustomerOrder.setInternalObservations__c("");
			sfscCustomerOrder.setObservations__c("");
			
			sfscCustomerOrder.setBillingSecondPhone__c(client.getTelephone1Livraison());
			sfscCustomerOrder.setBillingMainPhone__c(client.getTelephone2Livraison());
			sfscCustomerOrder.setBillingSalutation__c(client.getSalutation());
			sfscCustomerOrder.setBillingLastName__c(client.getNom());
			sfscCustomerOrder.setBillingFirstName__c(client.getPrenom());
			sfscCustomerOrder.setBillingEmail__c(client.getEmail());
			sfscCustomerOrder.setBillingAccessibility__c(null);
			sfscCustomerOrder.setBillingAddressFirstLine__c(client.getAdressStreetLine1());
			sfscCustomerOrder.setBillingAddressSecondLine__c(client.getAdressStreetLine2());
			sfscCustomerOrder.setBillingCity__c(client.getAdressCity());
			sfscCustomerOrder.setBillingFloorNumber__c(null);
			sfscCustomerOrder.setBillingHomeType__c(null);
			sfscCustomerOrder.setBillingPostalCode__c(client.getAdressPostalCode());
			sfscCustomerOrder.setBillingSpecificConstrains__c("");

			sfscCustomerOrder.setShippingSalutation__c(SalutationEnum.Mx);
			sfscCustomerOrder.setShippingFirstName__c (client.getPrenom());
			sfscCustomerOrder.setShippingLastName__c (client.getNom());
			sfscCustomerOrder.setShippingAddressFirstLine__c(client.getAdressStreetLine1());
			sfscCustomerOrder.setShippingAddressSecondLine__c(client.getAdressStreetLine2());
			sfscCustomerOrder.setShippingCity__c(client.getAdressCity());
			sfscCustomerOrder.setShippingPostalCode__c(client.getAdressPostalCode());
			sfscCustomerOrder.setShippingSpecificConstrains__c ("");
			sfscCustomerOrder.setShippingTrackingLink__c("");
			sfscCustomerOrder.setShippingMainPhone__c (client.getTelephone1());
			sfscCustomerOrder.setShippingSecondPhone__c (client.getTelephone2());
			sfscCustomerOrder.setShippingCompanyName__c ("");
			sfscCustomerOrder.setShippingAccessibility__c("");
			
			
			sfscCustomerOrder.setObservations__c("");
			sfscCustomerOrder.setInternalObservations__c("");
			
			compositeCalls.put(sfscCustomerOrder.getId(), new CompositeItem ( "Customer Order" ,sfscCustomerOrder.getId() , sfscCustomerOrder,
					"{"
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/CustomerOrder__c/" + sfscCustomerOrder.getId() + "\","
					+ "\"referenceId\": \"" + sfscCustomerOrder.getId() + "\","
					+ "\"body\": "
					+ sfscCustomerOrder.toPayload()
					+ "}"));
			
		}

		callCompositeItems(compositeCalls, 24, "Customer Order");
		
	}


	private void anonymiseSupplierOrders(ClientEligible client, List<String> supplierOrderIds) throws JsonMappingException, JsonProcessingException {
		logger.info ("--- Salesforce Anonymize " + supplierOrderIds.size() + " Supplier Orders");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();

		for (String supplierOrderId : supplierOrderIds) {
			compositeCalls.put (supplierOrderId , new CompositeItem ("SupplierOrder" , supplierOrderId ,
					"        {\r\n"
					+ "            \"method\": \"GET\",\r\n"
					+ "            \"url\": \"/services/data/v61.0/sobjects/SupplierOrder__c/" + supplierOrderId + "\",\r\n"
					+ "            \"referenceId\": \""+supplierOrderId+"\"\r\n"
					+ "        }"));
		}
		
		if (compositeCalls.size()>0) {
			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Supplier Orders", true);
		
			List<SFSCSupplierOrder> supplierOrders = new ArrayList<SFSCSupplierOrder>();
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("----> Salesforce Get Supplier Orders - Response : " + response.getBody().replace("\n", "").replace("\r", ""));

					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
					
					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
					for (JsonNode linkedObjectList : compositeResponseNode) {
						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
							JsonNode bodyNode = linkedObjectList.get("body"); 
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								
								SFSCSupplierOrder supplierOrder = new SFSCSupplierOrder (bodyNode);
								supplierOrders.add(supplierOrder);
							}
						}
					}
				}
			}
			
			compositeCalls = new LinkedHashMap<String, CompositeItem>();
			
			for (SFSCSupplierOrder sfscSupplierOrder : supplierOrders) {
				sfscSupplierOrder.setName(client.getPrenom() + client.getNom());
				
				compositeCalls.put (sfscSupplierOrder.getId(), new CompositeItem ("SupplierOrder" , sfscSupplierOrder.getId() , sfscSupplierOrder,
						"{"
						+ "\"method\": \"PATCH\"," 
						+ "\"url\": \"/services/data/v61.0/sobjects/SupplierOrder__c/" + sfscSupplierOrder.getId() + "\","
						+ "\"referenceId\": \"" + sfscSupplierOrder.getId() + "\","
						+ "\"body\": {"
						+ "\"Name\":\""+sfscSupplierOrder.getName()+"\""
						+ "}"
						+ "}"));
			}
			
			callCompositeItems(compositeCalls, 24 , "Supplier Order");
		}
	}

	private SFSCQuote getQuote (String quoteId) throws JsonMappingException, JsonProcessingException {
		logger.info ("---- Salesforce - get Quotes");

		Callable<ResponseEntity<String>> job = sfscClient.getQuoteByUuid(quoteId);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (response.getStatusCode()==HttpStatus.OK) {
			logger.debug ("Salesforce response - get Quotes : \n" + response.getBody());
			
			JsonNode bodyNode = mapper.readTree(response.getBody());

			if ((bodyNode!=null)&&(!bodyNode.isNull())) {
				SFSCQuote quote = SFSCCustomerHelper.getInstance().getSFSCQuote(bodyNode);
				return quote;
			}
		} else {
			logger.error("---- Salesforce - Quote Search Error : " + response.getStatusCode() + " : " + quoteId);
		}
		return null;
	}
	
	private List<SFSCQuote> getAllQuotes(List<SFItemObject> sfItemObjectList) throws JsonMappingException, JsonProcessingException {
		if (sfItemObjectList==null) {
			logger.info ("--- Salesforce No Quote found");
			return new ArrayList<SFSCQuote>();
		}
		logger.info ("--- Salesforce Get " + sfItemObjectList.size() + " Quotes");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		List<SFSCQuote> quoteList = new ArrayList<SFSCQuote>();
		for (SFItemObject quoteItem : sfItemObjectList) {
			compositeCalls.put(quoteItem.getId(), new CompositeItem ("Quote" , quoteItem.getId() , quoteItem,
					"{\"method\": \"GET\",\"url\": \""+quoteItem.getUrl()+"\",\"referenceId\": \""+ quoteItem.getId() +"\"}"));
		}
		
		if (compositeCalls.size()==0) {
			return quoteList;
		} else {
			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Quote", true);
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("---> Salesforce Get Quotes Response : " + response.getBody());
					
					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
					
					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
					for (JsonNode linkedObjectList : compositeResponseNode) {
						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
							JsonNode bodyNode = linkedObjectList.get("body"); 
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								SFSCQuote quote = SFSCCustomerHelper.getInstance().getSFSCQuote(bodyNode);
								quoteList.add(quote);
							}
						}
					}
				}			
			}
		}
		return quoteList;		
	}

	private List<SFSCCase> getAllCases(List<SFItemObject> sfItemObjectList) throws JsonMappingException, JsonProcessingException {
		if (sfItemObjectList==null) {
			logger.info ("--- Salesforce No Case found");
			return new ArrayList<SFSCCase>();
		}
		logger.info ("--- Salesforce Get " + sfItemObjectList.size() + " Cases");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		List<SFSCCase> caseList = new ArrayList<SFSCCase>();
		for (SFItemObject caseItem : sfItemObjectList) {
			compositeCalls.put(caseItem.getId(), new CompositeItem ("Case" , caseItem.getId() , caseItem,
					"{\"method\": \"GET\",\"url\": \""+caseItem.getUrl()+"\",\"referenceId\": \""+ caseItem.getId() +"\"}"));
		}
		
		if (compositeCalls.size()==0) {
			return caseList;
		} else {
			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Case", true);
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("---> Salesforce Get Case Response : " + response.getBody());
					
					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
					
					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
					for (JsonNode linkedObjectList : compositeResponseNode) {
						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
							JsonNode bodyNode = linkedObjectList.get("body"); 
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								SFSCCase customerorder = SFSCCustomerHelper.getInstance().getSFSCCase (bodyNode);
								caseList.add(customerorder);
							}
						}
					}
				}			
			}
		}
		return caseList;
	}

	private List<SFSCOpportunity> getAllOpportunities(List<SFItemObject> sfItemObjectList) throws JsonMappingException, JsonProcessingException {	
		if (sfItemObjectList==null) {
			logger.info ("--- Salesforce No Opportunity found");
			return new ArrayList<SFSCOpportunity>();
		}
		logger.info ("--- Salesforce Get " + sfItemObjectList.size() + " Customer Orders");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		List<SFSCOpportunity> opportunityList = new ArrayList<SFSCOpportunity>();
		for (SFItemObject opportunityItem : sfItemObjectList) {
			compositeCalls.put(opportunityItem.getId(), new CompositeItem ("Opportunity" , opportunityItem.getId() , opportunityItem ,
					"{\"method\": \"GET\",\"url\": \""+opportunityItem.getUrl()+"\",\"referenceId\": \""+ opportunityItem.getId() +"\"}"));
		}
		
		if (compositeCalls.size()==0) {
			return opportunityList;
		} else {
			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Opportunity", true);
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("---> Salesforce Get Opportunity Response : " + response.getBody());
					
					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
					
					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
					for (JsonNode linkedObjectList : compositeResponseNode) {
						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
							JsonNode bodyNode = linkedObjectList.get("body"); 
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								SFSCOpportunity opportunity = SFSCCustomerHelper.getInstance().getSFSCOpportunity(bodyNode);
								opportunityList.add(opportunity);
								
								//TODO ajouter la recherche des email d'opportunité
							}
						}
					}
				}			
			}
		}
		return opportunityList;
		
//							if ((attributesNode!=null)&&(!attributesNode.isNull())) {
//								JsonNode typeNode = linkedObjectList.get("type");
//								if ((typeNode!=null)&&(!typeNode.isNull())) {
//									type = typeNode.asText();
//								}
//								JsonNode urlNode = linkedObjectList.get("url");
//								if ((urlNode!=null)&&(!urlNode.isNull())) {
//									url = urlNode.asText();
//								}
//							}
//							
//							if (type!=null) {
//								if (type.equals("Opportunity")) {
//									SFSCOpportunity opportunity = SFSCCustomerHelper.getInstance().getSFSCOpportunity (bodyNode);
//									opportunityList.add(opportunity);								
//								}
//								if (type.equals("EmailMessage")) {
////									crObject = SFItemObject.getResponseObject(recordNode);
//
//									JsonNode idNode = linkedObjectList.get("Id");
//									if ((idNode!=null)&&(!idNode.isNull())) {
//										String id = idNode.asText();
//										emailIds.put(id, url);
//									}

	}
	
//	private List<SFSCOpportunity> getAllOpportunities(Map<String, String> opportunitiesIds) throws JsonMappingException, JsonProcessingException {		
//		logger.info ("---- Salesforce Get " + opportunitiesIds.size() + " Opportunities");
//		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
//
//		List<SFSCOpportunity> opportunityList = new ArrayList<SFSCOpportunity>();
//		for (Entry<String, String> opportunityItem : opportunitiesIds.entrySet()) {
//			compositeCalls.put(opportunityItem.getKey(), new CompositeItem ( "Opportunity" , opportunityItem.getKey() , 
//					"{\"method\": \"GET\",\"url\": \""+opportunityItem.getValue()+"\",\"referenceId\": \""+ opportunityItem.getKey() +"\"}"));
//		}
//		
//		if (compositeCalls.size()==0) {
//			return opportunityList;
//		} 
//		
//		List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Opportunities");
//
//		for (ResponseEntity<String> response : responses) {
//			if (response.getStatusCode()==HttpStatus.OK) {
//				logger.debug ("Salesforce response - Opportunity : " + response.getBody());
//				
//				JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
//				
//				JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
//				for (JsonNode linkedObjectList : compositeResponseNode) {
//					JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
//					if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
//						JsonNode bodyNode = linkedObjectList.get("body"); 
//						if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//							String type = null;
//							String url = null;
//							JsonNode attributesNode = linkedObjectList.get("attributes");
//							if ((attributesNode!=null)&&(!attributesNode.isNull())) {
//								JsonNode typeNode = linkedObjectList.get("type");
//								if ((typeNode!=null)&&(!typeNode.isNull())) {
//									type = typeNode.asText();
//								}
//								JsonNode urlNode = linkedObjectList.get("url");
//								if ((urlNode!=null)&&(!urlNode.isNull())) {
//									url = urlNode.asText();
//								}
//							}
//							
//							if (type!=null) {
//								if (type.equals("Opportunity")) {
//									SFSCOpportunity opportunity = SFSCCustomerHelper.getInstance().getSFSCOpportunity (bodyNode);
//									opportunityList.add(opportunity);								
//								}
//								if (type.equals("EmailMessage")) {
////									crObject = SFItemObject.getResponseObject(recordNode);
//
//									JsonNode idNode = linkedObjectList.get("Id");
//									if ((idNode!=null)&&(!idNode.isNull())) {
//										String id = idNode.asText();
//										emailIds.put(id, url);
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return opportunityList;
//	}
//	
	private List<SFSCCustomerOrder> getAllCustomerOrders(List<SFItemObject> sfItemObjectList) throws JsonMappingException, JsonProcessingException {
		if (sfItemObjectList==null) {
			logger.info ("--- Salesforce No Customer Order found");
			return new ArrayList<SFSCCustomerOrder>();
		}
		logger.info ("--- Salesforce Get Customer " + sfItemObjectList.size() + " Orders");
		
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		List<SFSCCustomerOrder> customerOrderList = new ArrayList<SFSCCustomerOrder>();
		for (SFItemObject customerOrderItem : sfItemObjectList) {
			compositeCalls.put(customerOrderItem.getId(), new CompositeItem ("Customer Order" , customerOrderItem.getId() , customerOrderItem ,
					"{\"method\": \"GET\",\"url\": \""+customerOrderItem.getUrl()+"\",\"referenceId\": \""+ customerOrderItem.getId() +"\"}"));
		}
		
		if (compositeCalls.size()==0) {
			return customerOrderList;
		} else {
			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Customer Orders", true);
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("---> Salesforce Get Customer Orders Response : " + response.getBody());
					
					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
					
					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
					for (JsonNode linkedObjectList : compositeResponseNode) {
						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
							JsonNode bodyNode = linkedObjectList.get("body"); 
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								SFSCCustomerOrder customerorder = SFSCCustomerHelper.getInstance().getSFSCCustomerOrder(bodyNode);
								customerOrderList.add(customerorder);
							}
						}
					}
				}			
			}
		}
		return customerOrderList;
	}

//	private List<SFSCCustomerOrder> getAllCustomerOrders(Map<String, String> customerOrderIds) throws JsonMappingException, JsonProcessingException {
//		logger.info ("--- Salesforce Get Customer " + customerOrderIds.size() + " Orders");
//		
//		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
//		List<SFSCCustomerOrder> customerOrderList = new ArrayList<SFSCCustomerOrder>();
//		for (Entry<String, String> customerOrderItem : customerOrderIds.entrySet()) {
//			compositeCalls.put(customerOrderItem.getKey(), new CompositeItem ("Customer Order" , customerOrderItem.getKey() , 
//					"{\"method\": \"GET\",\"url\": \""+customerOrderItem.getValue()+"\",\"referenceId\": \""+ customerOrderItem.getKey() +"\"}"));
//		}
//		
//		if (compositeCalls.size()==0) {
//			return customerOrderList;
//		} else {
//			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls, 24 , "Get Customer Orders");
//			
//			for (ResponseEntity<String> response : responses) {
//				if (response.getStatusCode()==HttpStatus.OK) {
//					logger.debug ("---> Salesforce Get Customer Orders Response : " + response.getBody());
//					
//					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
//					
//					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
//					for (JsonNode linkedObjectList : compositeResponseNode) {
//						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
//						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
//							JsonNode bodyNode = linkedObjectList.get("body"); 
//							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//								SFSCCustomerOrder customerorder = SFSCCustomerHelper.getInstance().getSFSCCustomerOrder(bodyNode);
//								customerOrderList.add(customerorder);
//							}
//						}
//					}
//				}			
//			}
//		}
//		return customerOrderList;
//	}
	
	private void fillSupplierOrderList(List<SFSCCustomerOrder> customerOrderList) throws JsonMappingException, JsonProcessingException {
		logger.info ("---- Salesforce Get " + customerOrderList.size() + " Supplier Orders");
		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
		for (SFSCCustomerOrder customerOrder : customerOrderList) {
			compositeCalls.put(customerOrder.getId(), new CompositeItem ( "Supplier Order from Customer Order" , customerOrder.getId() , customerOrder ,
					"        {\r\n"
					+ "            \"method\": \"GET\",\r\n"
					+ "            \"url\": \"/services/data/v61.0/query?q=SELECT id, SupplierOrderLine__c , SupplierOrderLine__r.ExternalId__c, SupplierOrderLine__r.SupplierOrder__c, SupplierOrderLine__r.SupplierOrder__r.ExternalId__c  FROM CustomerSupplierOrderLine__c WHERE CustomerOrderLine__r.CustomerOrder__c  ='"+customerOrder.getId()+"'\",\r\n"
					+ "            \"referenceId\": \""+customerOrder.getId()+"\"\r\n"
					+ "        }"));
		}
		
		if (compositeCalls.size()>0) {
			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls , 24 , "Get Supplier Orders from customer order", true);

			List<String> suppOrderIds = new ArrayList<String>();
			List<String> stockOrderIds = new ArrayList<String>();
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					
					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
					
					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
					for (JsonNode linkedObjectList : compositeResponseNode) {
						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
							JsonNode bodyNode = linkedObjectList.get("body"); 
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								JsonNode bodyRecords = bodyNode.get("records"); 
								if ((bodyRecords!=null)&&(!bodyRecords.isNull())) {
									for (JsonNode record : bodyRecords) {
										JsonNode supplierOrderLine__rNode = record.get("SupplierOrderLine__r");
										if ((supplierOrderLine__rNode!=null)&&(!supplierOrderLine__rNode.isNull())) {
											JsonNode SupplierOrder__cNode = supplierOrderLine__rNode.get("SupplierOrder__c");
											if ((SupplierOrder__cNode!=null)&&(!SupplierOrder__cNode.isNull())) {
												if (!suppOrderIds.contains(SupplierOrder__cNode.asText())) {
													suppOrderIds.add(SupplierOrder__cNode.asText());
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			Map<String, CompositeItem> coCompositeCalls = new LinkedHashMap<String, CompositeItem>();
			for (String supplierOrderId : suppOrderIds) {
				coCompositeCalls.put(supplierOrderId, new CompositeItem ( "Customer Order From Supplier Order" , supplierOrderId , 
						"        {\r\n"
						+ "            \"method\": \"GET\",\r\n"
						+ "            \"url\": \"/services/data/v61.0/query?q=SELECT id, Quantity__c, SupplierOrderLine__r.Quantity__c,  CustomerOrderLine__r.ExternalId__c , CustomerOrderLine__r.CustomerOrder__c, CustomerOrderLine__r.CustomerOrder__r.ExternalId__c, CustomerOrderLine__r.CustomerOrder__r.Account__c  from CustomerSupplierOrderLine__c Where SupplierOrderLine__r.SupplierOrder__c = '"+supplierOrderId+"'\",\r\n"
						+ "            \"referenceId\": \""+supplierOrderId+"\"\r\n"
						+ "        }"));
			}
			
			if (coCompositeCalls.size()>0) {
				List<ResponseEntity<String>> coResponses = callCompositeItems(coCompositeCalls , 24 , "Get Customer Order From Supplier Order", true);
				for (ResponseEntity<String> response : coResponses) {
					if (response.getStatusCode()==HttpStatus.OK) {
						
						JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
						
						JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
						for (JsonNode linkedObjectList : compositeResponseNode) {
							JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
							if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
								String reference = referenceObjectNode.asText();
								
								JsonNode bodyNode = linkedObjectList.get("body"); 
								if ((bodyNode!=null)&&(!bodyNode.isNull())) {
									JsonNode bodyRecords = bodyNode.get("records"); 
									if ((bodyRecords!=null)&&(!bodyRecords.isNull())) {
										for (JsonNode record : bodyRecords) {
											Double stockQuantity = null;
											Double supplierQuantity = null;
											JsonNode stockQuantityNode = record.get("Quantity__c"); 
											if ((stockQuantityNode!=null)&&(!stockQuantityNode.isNull())) {
												stockQuantity = stockQuantityNode.asDouble();
											}
											JsonNode SupplierOrderLine__rNode = record.get("SupplierOrderLine__r"); 
											if ((SupplierOrderLine__rNode!=null)&&(!SupplierOrderLine__rNode.isNull())) {
												JsonNode supplierQuantityNode = SupplierOrderLine__rNode.get("Quantity__c"); 
												if ((supplierQuantityNode!=null)&&(!supplierQuantityNode.isNull())) {
													supplierQuantity = supplierQuantityNode.asDouble();
												}
											}
											if ((stockQuantity!=null) && (supplierQuantity!=null)) {
												if (stockQuantity.doubleValue() == supplierQuantity.doubleValue()) {
													// Les quantité étant égales on peut conclure que c'est une ligne de commande de contremarque
												} else {
													// Les quantité étant différentes on peut conclure que c'est une ligne de commande de Stock
													if (!stockOrderIds.contains(reference)) {
														stockOrderIds.add(reference);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				for (String supplierOrderId : suppOrderIds) {
					if (!stockOrderIds.contains(supplierOrderId)) {
						supplierOrderIds.add(supplierOrderId);
					}
				}
			}
		}
	}
	
//	@Deprecated
//	private void fillSupplierOrderListOld (List<SFSCCustomerOrder> customerOrderList) throws JsonMappingException, JsonProcessingException {
//		logger.info ("---- Salesforce Get " + customerOrderList.size() + " Supplier Orders");
//		Map<String, CompositeItem> compositeCalls = new LinkedHashMap<String, CompositeItem>();
//		for (SFSCCustomerOrder customerOrder : customerOrderList) {
//			compositeCalls.put(customerOrder.getId(), new CompositeItem ( "Customer Order Line" , customerOrder.getId() ,
//					"        {\r\n"
//					+ "            \"method\": \"GET\",\r\n"
//					+ "            \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM CustomerOrderLine__c WHERE CustomerOrder__c  = '"+customerOrder.getId()+"'\",\r\n"
//					+ "            \"referenceId\": \""+customerOrder.getId()+"\"\r\n"
//					+ "        }"));
//		}
//
//		//SELECT id, SupplierOrderLine__c , SupplierOrderLine__r.ExternalId__c, SupplierOrderLine__r.SupplierOrder__c, SupplierOrderLine__r.SupplierOrder__r.ExternalId__c  FROM CustomerSupplierOrderLine__c WHERE CustomerOrderLine__r.CustomerOrder__c  = 'a0Xds000001eGv9EAE'
//		//SELECT id, Quantity__c, SupplierOrderLine__r.Quantity__c,  CustomerOrderLine__r.ExternalId__c , CustomerOrderLine__r.CustomerOrder__c, CustomerOrderLine__r.CustomerOrder__r.ExternalId__c, CustomerOrderLine__r.CustomerOrder__r.Account__c  from CustomerSupplierOrderLine__c Where SupplierOrderLine__r.SupplierOrder__c = 'a0jds000006bnG7AAI'
//		
//		if (compositeCalls.size()>0) {
//			List<ResponseEntity<String>> responses = callCompositeItems(compositeCalls , 24 , "Get Supplier Order Lines", true);
//		
//			List<String> customerOrderLineIds = new ArrayList<String>();
//			
//			for (ResponseEntity<String> response : responses) {
//				if (response.getStatusCode()==HttpStatus.OK) {
//					logger.debug ("----> Salesforce Get Supplier Orders Lines Response : " + response.getBody());
//					
//					JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
//					
//					JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
//					for (JsonNode linkedObjectList : compositeResponseNode) {
//						JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
//						if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
//							JsonNode bodyNode = linkedObjectList.get("body"); 
//							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//								JsonNode bodyRecords = bodyNode.get("records"); 
//								if ((bodyRecords!=null)&&(!bodyRecords.isNull())) {
//									for (JsonNode record : bodyRecords) {
//										JsonNode idNode = record.get("Id");
//										if ((idNode!=null)&&(!idNode.isNull())) {
//											customerOrderLineIds.add(idNode.asText());											
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//			
//			if (customerOrderLineIds.size()>0) {
//				compositeCalls = new LinkedHashMap<String, CompositeItem>();
//				for (String customerOrderLineId : customerOrderLineIds) {
//					compositeCalls.put(customerOrderLineId, new CompositeItem ( "Supplier Order Line" , customerOrderLineId ,
//							"        {\r\n"
//							+ "            \"method\": \"GET\",\r\n"
//							+ "            \"url\": \"/services/data/v61.0/query?q=SELECT SupplierOrderLine__c FROM CustomerSupplierOrderLine__c WHERE CustomerOrderLine__c = '"+ customerOrderLineId +"'\",\r\n"
//							+ "            \"referenceId\": \""+customerOrderLineId+"\"\r\n"
//							+ "        }"));
//				}
//
//				if (compositeCalls.size()>0) {
//					responses = callCompositeItems(compositeCalls, 24 , "Get Supplier Order Line <-> Customer OrderLines", true);
//				
//					List<String> suplierOrderLineIds = new ArrayList<String>();
//					
//					for (ResponseEntity<String> response : responses) {
//						if (response.getStatusCode()==HttpStatus.OK) {
//							logger.debug ("----> Salesforce Get Supplier Orders Lines <-> Customer Orders Lines - Response : " + response.getBody());
//							
//							JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
//							
//							JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
//							for (JsonNode linkedObjectList : compositeResponseNode) {
//								JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
//								if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
//									JsonNode bodyNode = linkedObjectList.get("body"); 
//									if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//										JsonNode bodyRecords = bodyNode.get("records"); 
//										if ((bodyRecords!=null)&&(!bodyRecords.isNull())) {
//											for (JsonNode record : bodyRecords) {
//												JsonNode idNode = record.get("SupplierOrderLine__c");
//												if ((idNode!=null)&&(!idNode.isNull())) {
//													suplierOrderLineIds.add(idNode.asText());											
//												}
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//					if (suplierOrderLineIds.size()>0) {
//						compositeCalls = new LinkedHashMap<String, CompositeItem>();
//						for (String suplierOrderLineId : suplierOrderLineIds) {
//							compositeCalls.put(suplierOrderLineId, new CompositeItem ("Supplier Order" , suplierOrderLineId , 
//									"        {\r\n"
//									+ "            \"method\": \"GET\",\r\n"
//									+ "            \"url\": \"/services/data/v61.0/query?q=SELECT SupplierOrder__c FROM SupplierOrderLine__c WHERE Id  = '"+suplierOrderLineId+"'\",\r\n"
//									+ "            \"referenceId\": \""+suplierOrderLineId+"\"\r\n"
//									+ "        }"));
//						}
//	
//						if (compositeCalls.size()>0) {
//							responses = callCompositeItems(compositeCalls , 24 , "Get Supplier Orders", true);
//								
//							List<String> localSupplierOrderLinesId = new ArrayList<String>();
//							for (ResponseEntity<String> response : responses) {
//								if (response.getStatusCode()==HttpStatus.OK) {
//									logger.debug ("----> Salesforce Get Supplier Orders - Response : " + response.getBody());
//									
//									JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
//									
//									JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
//									for (JsonNode linkedObjectList : compositeResponseNode) {
//										JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
//										if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
//											JsonNode bodyNode = linkedObjectList.get("body"); 
//											if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//												JsonNode bodyRecords = bodyNode.get("records"); 
//												if ((bodyRecords!=null)&&(!bodyRecords.isNull())) {
//													for (JsonNode record : bodyRecords) {
//														JsonNode idNode = record.get("SupplierOrder__c");
//														if ((idNode!=null)&&(!idNode.isNull())) {
//															if (!localSupplierOrderLinesId.contains(idNode.asText())) {
//																localSupplierOrderLinesId.add(idNode.asText());																											
//															}
//														}
//													}
//												}
//											}
//										}
//									}
//								}
//							}
//							Map<String,CompositeItem> howManyCustomerOrderCalls = new LinkedHashMap<String, CompositeItem>();
//							// getting all Customer orders attached to a Supplier Order. 
//							// If Result > 1 => It's a stock Order
//							// If Result = 1 => It's an order on demand (Contremarque)
//							// If Result < 0 => It's Incoherent
//							
//							for (String supplierOrderId : localSupplierOrderLinesId) {
//								howManyCustomerOrderCalls.put(supplierOrderId.trim(), new CompositeItem ( "Supplier Order Line", supplierOrderId.trim() ,
//										"        {\r\n"
//										+ "            \"method\": \"GET\",\r\n"
//										+ "            \"url\": \"/services/data/v61.0/query?q=SELECT Id, SupplierOrderLine__r.SupplierOrder__r.ExternalId__c, CustomerOrderLine__r.CustomerOrder__r.ExternalId__c From CustomerSupplierOrderLine__c where SupplierOrderLine__c in ( SELECT Id FROM SupplierOrderLine__c where SupplierOrder__r.Id = '"+supplierOrderId+"')\",\r\n"
//										+ "            \"referenceId\": \""+supplierOrderId.trim()+"\"\r\n"
//										+ "        }"));
//							}
//								
//							if (howManyCustomerOrderCalls.size()>0) {
//								responses = callCompositeItems(howManyCustomerOrderCalls , 24 , "Count Customer Orders from Supplier Orders", true);
//								
//								for (ResponseEntity<String> response : responses) {
//									if (response.getStatusCode()==HttpStatus.OK) {
//										logger.debug ("----> Salesforce Count Customer Orders from Supplier Orders - Response : " + response.getBody());
//										
//										JsonNode sourceCustomerOrderNode = mapper.readTree(response.getBody());
//										
//										JsonNode compositeResponseNode = sourceCustomerOrderNode.get("compositeResponse");
//										for (JsonNode linkedObjectList : compositeResponseNode) {
//											JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
//											if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
//												JsonNode bodyNode = linkedObjectList.get("body"); 
//												if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//													JsonNode sizeNode = bodyNode.get("totalSize"); 
//													if ((sizeNode!=null)&&(!sizeNode.isNull())) {
//														if (sizeNode.asInt()==1) {
//															supplierOrderIds.add(referenceObjectNode.asText());															
//														} else {
//															String external_Id = "NotFound";
//															JsonNode recordsNode = bodyNode.get("records"); 
//															if ((recordsNode!=null)&&(!recordsNode.isNull())) {
//																JsonNode supplierOrderLineNode = linkedObjectList.get("SupplierOrderLine__r"); 
//																if ((supplierOrderLineNode!=null)&&(!supplierOrderLineNode.isNull())) {
//																	JsonNode supplierOrderNode = supplierOrderLineNode.get("CustomerOrder__r"); 
//																	if ((supplierOrderNode!=null)&&(!supplierOrderNode.isNull())) {
//																		JsonNode supplierExternalIdNode = supplierOrderNode.get("ExternalId__c"); 
//																		if ((supplierExternalIdNode!=null)&&(!supplierExternalIdNode.isNull())) {
//																			external_Id = supplierExternalIdNode.asText();
//																		}
//																	}
//																}
//															}
//															logger.warn("Supplier Order '" + external_Id + "' is linked to multiple customer order. Impossible to anonymized.");
//														}
//													}
//												}
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//	}
	
	private SFSCCustomerOrder getCustomerOrders(String customerOrderId) throws JsonMappingException, JsonProcessingException {
		logger.info ("---- Salesforce - get Customer Orders");
		
		Callable<ResponseEntity<String>> job = sfscClient.getCustomerOrderByUuid (customerOrderId);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (response.getStatusCode()==HttpStatus.OK) {
			logger.debug (response.getBody());
			
			JsonNode bodyNode = mapper.readTree(response.getBody());
			
			if ((bodyNode!=null)&&(!bodyNode.isNull())) {
				SFSCCustomerOrder customerorder = SFSCCustomerHelper.getInstance().getSFSCCustomerOrder(bodyNode);
				return customerorder;
			}
		} else {
			logger.error("Customer Order Search Error : " + response.getStatusCode() + " : " + customerOrderId);
		}
		return null;
	}
	
	public List<String> getCustomerOrderIds() {
		return customerOrderUuids;
	}
	
	public void anonymizeCustomerOrder(ClientPartiel clientItem) throws JsonMappingException, JsonProcessingException {
		SFSCCustomerOrder order = getCustomerOrders(clientItem.getUuidCc());
		if (order==null) {
			logger.warn ("Salesforce - Customer Order Not found");
		} else {
			List<SFSCCustomerOrder> orderList = new ArrayList<SFSCCustomerOrder>();
			orderList.add(order);
			anonymiseCustomerOrders(clientItem, orderList); 			
		}
	}
	
	public List<ResponseEntity<String>> callComposite(Map<String, String> compositeCalls, String label) throws JsonMappingException, JsonProcessingException {
		return callComposite(compositeCalls, 24, label);
	}
	
	public List<ResponseEntity<String>> callComposite(Map<String, String> compositeCalls, int batchSize, String label) throws JsonMappingException, JsonProcessingException {
		int i = 0;
		List<ResponseEntity<String>> results = new ArrayList<ResponseEntity<String>>();
		String composite = "";
		for (Entry<String, String> compositeCallEntry : compositeCalls.entrySet()) {
			composite += "," + compositeCallEntry.getValue();
			i++;
			
			if (i>batchSize) {
				composite = "{\r\n"
						+ "    \"allOrNone\": false,\r\n"
						+ "    \"compositeRequest\": [\r\n"
						+ composite.substring(1)
						+ "   ]\r\n"
						+ "}";
				
				logger.debug("---- Salesforce Request - " + label + " : " +  composite.replace("\n","").replace("\r",""));
				Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
				composite = "";
				i = 0;

				ResponseEntity<String> response = null;
				try {
					response = job.call();
				} catch (Exception e) {
					logger.debug ("---- Salesforce Response - " + label + " - FAILED - " + e.getMessage());
					ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.ERROR, e.getMessage()));
					e.printStackTrace();
				}
				if (response.getStatusCode()==HttpStatus.OK ) {
					logger.info ("---- Salesforce Response - " + label + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
					ReportBook.getInstance().addReport (new Report(currentCustomer, "Salesforce" , label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.OK, null));
				} else {
					logger.error ("---- Salesforce Response - " + label + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );				
					ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
				}
				
				results.add(response);

			}
		}
		if (i>0) {
			composite = "{\r\n"
					+ "    \"allOrNone\": false,\r\n"
					+ "    \"compositeRequest\": [\r\n"
					+ composite.substring(1)
					+ "   ]\r\n"
					+ "}";

			logger.debug("---- Salesforce Request - " + label + " : " +  composite.replace("\n","").replace("\r",""));
			Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
			composite = "";

			ResponseEntity<String> response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				logger.debug ("---- Salesforce Response - " + label + " - FAILED - " + e.getMessage());
				ReportBook.getInstance().addReport (new Report(currentCustomer, "Salesforce" ,  label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.ERROR, e.getMessage()));
				e.printStackTrace();
			}
			if (response.getStatusCode()==HttpStatus.OK ) {
				logger.info ("---- Salesforce Response - " + label + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
				ReportBook.getInstance().addReport (new Report(currentCustomer, "Salesforce" , label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.OK, null));
			} else {
				logger.error ("---- Salesforce Response - " + label + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );				
				ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
			}

			results.add(response);			
		}
		
		for (ResponseEntity<String> responseEntity : results) {
			JsonNode sourceCustomerOrderNode = mapper.readTree(responseEntity.getBody());
			
			JsonNode compositeResponseNode = sourceCustomerOrderNode.get("compositeResponse");
			for (JsonNode linkedObjectList : compositeResponseNode) {
				JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
				String reference = null;
				if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
					reference = referenceObjectNode.asText();
				}
				String statusCode = null;
				JsonNode httpStatusCodeNode = linkedObjectList.get("httpStatusCode");
				if ((httpStatusCodeNode!=null)&&(!httpStatusCodeNode.isNull())) {
					statusCode = httpStatusCodeNode.asText();
					if (statusCode.substring(0, 1).equals("2")) {
						ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, reference, "", ReportGravity.OK, statusCode));
					} else {
						
					}
				}
								
//				JsonNode bodyNode = linkedObjectList.get("body"); 
//				if ((bodyNode!=null)&&(!bodyNode.isNull())) {
//					JsonNode doneNode = bodyNode.get("done"); 
//					if ((doneNode!=null)&&(!doneNode.isNull())) {
//						if (doneNode.asBoolean()) {
//							
//						}
//					}
//				}
			}

		}
		return results;
	}

	public List<ResponseEntity<String>> callCompositeItems(Map<String, CompositeItem> compositeCalls, int batchSize, String label) throws JsonMappingException, JsonProcessingException {
		return callCompositeItems(compositeCalls, batchSize, label, false);
	}
	
	public List<ResponseEntity<String>> callCompositeItems(Map<String, CompositeItem> compositeCalls, int batchSize, String label, boolean bypassReport) throws JsonMappingException, JsonProcessingException {
		int i = 0;
		List<ResponseEntity<String>> results = new ArrayList<ResponseEntity<String>>();
		String composite = "";
		for (Entry<String, CompositeItem> compositeCallEntry : compositeCalls.entrySet()) {
			
			composite += "," + compositeCallEntry.getValue().getRequest();
			i++;
			
			if (i>batchSize) {
				composite = "{\r\n"
						+ "    \"allOrNone\": false,\r\n"
						+ "    \"compositeRequest\": [\r\n"
						+ composite.substring(1)
						+ "   ]\r\n"
						+ "}";
				
				logger.debug("---- Salesforce Request - " + label + " : " +  composite.replace("\n","").replace("\r",""));
				Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
				composite = "";
				i = 0;

				ResponseEntity<String> response = null;
				try {
					response = job.call();
				} catch (Exception e) {
					logger.debug ("---- Salesforce Response - " + label + " - FAILED - " + e.getMessage());
					ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, "", "", ReportGravity.ERROR, e.getMessage()));
					e.printStackTrace();
				}
				if (response.getStatusCode()==HttpStatus.OK ) {
					logger.info ("---- Salesforce Response - " + label + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
				} else {
					logger.error ("---- Salesforce Response - " + label + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );				
					ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, "", "", ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
				}
				
				results.add(response);

			}
		}
		if (i>0) {
			composite = "{\r\n"
					+ "    \"allOrNone\": false,\r\n"
					+ "    \"compositeRequest\": [\r\n"
					+ composite.substring(1)
					+ "   ]\r\n"
					+ "}";

			logger.debug("---- Salesforce Request - " + label + " : " +  composite.replace("\n","").replace("\r",""));
			Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
			composite = "";

			ResponseEntity<String> response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				logger.debug ("---- Salesforce Response - " + label + " - FAILED - " + e.getMessage());
				if (!bypassReport) ReportBook.getInstance().addReport (new Report(currentCustomer, "Salesforce" ,  label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.ERROR, e.getMessage()));
				e.printStackTrace();
			}
			if (response.getStatusCode()==HttpStatus.OK ) {
				logger.info ("---- Salesforce Response - " + label + " - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));							
			} else {
				logger.error ("---- Salesforce Response - " + label + " - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") );				
				if (!bypassReport) ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , label, currentCustomer.getCodeClient(), currentCustomer.getUuid(), ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
			}

			results.add(response);			
		}
		
		
		if (!bypassReport) {
			for (ResponseEntity<String> responseEntity : results) {
				JsonNode sourceCustomerOrderNode = mapper.readTree(responseEntity.getBody());
				
				JsonNode compositeResponseNode = sourceCustomerOrderNode.get("compositeResponse");
				for (JsonNode linkedObjectList : compositeResponseNode) {
					JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
					String reference = null;
					if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
						reference = referenceObjectNode.asText();
					}
					String statusCode = null;
					JsonNode httpStatusCodeNode = linkedObjectList.get("httpStatusCode");
					if ((httpStatusCodeNode!=null)&&(!httpStatusCodeNode.isNull())) {
						statusCode = httpStatusCodeNode.asText();
						CompositeItem item = compositeCalls.get(reference);
						SFItemObject sfObject = item.getSFObject();
						if (sfObject==null) {
							JsonNode bodyNode = linkedObjectList.get("body");
							if ((bodyNode!=null)&&(!bodyNode.isNull())) {
								try {
									sfObject = SFItemObject.getResponseObject(bodyNode);
								} catch (Exception e) {
									e.printStackTrace();
								}
								item.setCrObject(sfObject);
							}
						}
						if (statusCode.substring(0, 1).equals("2")) {
							if (sfObject==null) {
								CompositeItem compositeItem = compositeCalls.get(reference);
								if (compositeItem!=null) {
									ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , item.getObjectName(), compositeItem.getObjectDescription(), compositeItem.getObjectComplementary(), ReportGravity.OK, statusCode));																																
								} else {
									ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , item.getObjectName(), reference, "", ReportGravity.OK, statusCode));																							
								}
							} else {
								ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , item.getObjectName(), sfObject.getReference(), sfObject.getId(), ReportGravity.OK, statusCode));							
							}
						} else {
							if (sfObject==null) {
								ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , item.getObjectName(), httpStatusCodeNode.asText(), "", ReportGravity.ERROR, statusCode));						
							} else {
								ReportBook.getInstance().addReport (new Report(currentCustomer,  "Salesforce" , item.getObjectName(), sfObject.getReference(), sfObject.getId(), ReportGravity.ERROR, statusCode));						
							}
						}
					}
				}
			}
		}
		return results;
	}
}
