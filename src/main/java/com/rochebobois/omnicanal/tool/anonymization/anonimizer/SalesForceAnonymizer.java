package com.rochebobois.omnicanal.tool.anonymization.anonimizer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.rochebobois.omnicanal.sfsc.api.SfscEndpointParameters;
import com.rochebobois.omnicanal.sfsc.api.SfscInstance;
import com.rochebobois.omnicanal.sfsc.api.SfscRestClient;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomer;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomerHelper;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomerOrder;
import com.rochebobois.omnicanal.sfsc.domain.SFSCOpportunity;
import com.rochebobois.omnicanal.sfsc.domain.SFSCQuote;
import com.rochebobois.omnicanal.tool.anonymization.AnonymizationFromEcolix;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientPartiel;

public class SalesForceAnonymizer {
	private static final Logger logger = LoggerFactory.getLogger(AnonymizationFromEcolix.class);

	private static SfscRestClient sfscClient = null;
	private static ObjectMapper mapper = new ObjectMapper();

	private Environment currentEnv = null;
	private LocalDate referenceDate = null;
	private List<String> unsubsciptionToDotDigital = new ArrayList<String>();

	private Map<String,String> customerOrderIds = new HashMap<String, String>();
	private Map<String,String> quoteIds = new HashMap<String, String>();
	private Map<String,String> opportunityIds = new HashMap<String, String>();
	private List<String> customerOrderUuids = new ArrayList<String>();

	public SalesForceAnonymizer(Environment currentEnv, LocalDate referenceDate) throws ApiClientException {
		setCurrentEnv(currentEnv);
		setReferenceDate(referenceDate);
		
		SfscEndpointParameters sfscParam = SfscInstance.environment(getCurrentEnv()).getEndpointParameters();
		sfscClient = new SfscRestClient(sfscParam);
	}
	
	public static final String CustomerOrdersReference = "CustomerOrders";
	public static final String QuotesReference = "Quotes";
	public static final String OpportunitiesReference = "Opportunities";

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
		logger.info (client.getCodeClient() + " - " + client.getNom() + " - " + client.getEmail() + " - " + client.getUuid());
		
		Callable<ResponseEntity<String>> job = sfscClient.getCustomer(client.getUuid());
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (response.getStatusCode()==HttpStatus.OK) {
			
			logger.debug (response.getBody());
			JsonNode sfscGetCustomerResponseNode = mapper.readTree(response.getBody());
			
			SFSCCustomer sfscCustomer = SFSCCustomerHelper.getInstance().getSFSCCustomer(sfscGetCustomerResponseNode);
			
			unsubsciptionToDotDigital.add(sfscCustomer.getPersonEmail());
			
//			String oldFirstName = null;
//			String oldLastName = null;
//			
//			oldFirstName = sfscCustomer.getFirstName();
//			oldLastName = sfscCustomer.getLastName();
					
			sfscCustomer.setFirstName(client.getPrenom());
			sfscCustomer.setLastName(client.getNom());
			
			sfscCustomer.setShippingFirstName(client.getPrenom());
			sfscCustomer.setShippingLastName(client.getNom());
			
			sfscCustomer.setPersonEmail(client.getEmail());
			sfscCustomer.setPersonMobilePhone(client.getTelephone1());
			sfscCustomer.setPersonHomePhone(client.getTelephone2());
			sfscCustomer.setShippingMainPhone(client.getTelephone1Livraison());
			sfscCustomer.setShippingOtherPhone(client.getTelephone2Livraison());
			
			sfscCustomer.setAnonymizationDate(referenceDate);
			sfscCustomer.setAnonymizationStatus(AnonymizationStatusEnum.Anonymized);
			sfscCustomer.setAnonymizationTargetDate(referenceDate);
			
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

			logger.debug (sfscCustomer.toString());
			job = sfscClient.putCustomer(sfscCustomer);
			response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (response.getStatusCode()==HttpStatus.OK) {
				logger.debug (response.getBody());
				JsonNode sfscCustomerUpdateResponseNode = mapper.readTree(response.getBody());
				
				JsonNode SfAccountIdNode = sfscCustomerUpdateResponseNode.get("SfAccountId");
				if ((SfAccountIdNode!=null)&&!(SfAccountIdNode.isNull())) {
					String CustomerAccountId = SfAccountIdNode.asText();
					
					String getIdsRequest = "{\r\n"
							+ "    \"allOrNone\": false,\r\n"
							+ "    \"compositeRequest\": [\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM CustomerOrder__c WHERE Account__c='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \"" + CustomerOrdersReference + "\"\r\n"
							+ "    },\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM Quote__c WHERE Account__c='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \""+QuotesReference+"\"\r\n"
							+ "    },\r\n"
							+ "{\r\n"
							+ "      \"method\": \"GET\",\r\n"
							+ "      \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM Opportunity WHERE AccountID='" + CustomerAccountId + "'\",\r\n"
							+ "      \"referenceId\": \""+OpportunitiesReference+"\"\r\n"
							+ "    }\r\n"
							+ "\r\n"
							+ "   ]\r\n"
							+ "}";
					
					
					logger.debug (getIdsRequest);
					job = sfscClient.callComposite(getIdsRequest);
					response = null;
					try {
						response = job.call();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					if (response.getStatusCode()==HttpStatus.OK) {
						logger.debug (response.getBody());
						
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
											JsonNode idNode = recordNode.get("Id");
											String url = null;
											if ((idNode!=null)&&(!idNode.isNull())) {
												JsonNode attributeNode = recordNode.get("attributes");
												if ((attributeNode!=null)&&(!attributeNode.isNull())) {
													JsonNode urlNode = attributeNode.get("url");
													if ((urlNode!=null)&&(!urlNode.isNull())) {
														url = urlNode.asText();
													}
												}
												if (referenceObjectNode.asText().equals(CustomerOrdersReference)) {
													customerOrderIds.put(idNode.asText(),url);
												}
												if (referenceObjectNode.asText().equals(QuotesReference)) {
													quoteIds.put(idNode.asText(),url);
												}
												if (referenceObjectNode.asText().equals(OpportunitiesReference)) {
													opportunityIds.put(idNode.asText(),url);
												}
											}
										}
									}
								}
							}
						}
						
						List <SFSCCustomerOrder> customerOrderList = getAllCustomerOrders (customerOrderIds);
						anonymiseCustomerOrders (client, customerOrderList);
						
						List <SFSCQuote> quoteList = getAllQuotes (quoteIds);
						anonymiseQuotes (client, quoteList);

						List <SFSCOpportunity> opportunityList = getAllOpportunities(opportunityIds);
						anonymiseOpportunities (client, opportunityList);
						
					}
				}
				
			} else {
				logger.error ("Erreur de Mise à jour du client SFSC");
			}
			
		} else {
			logger.error ("Erreur de récupération du client SFSC");
		}
	}
	
	private void anonymiseOpportunities(ClientEligible client, List<SFSCOpportunity> opportunityList) throws JsonMappingException, JsonProcessingException {
		if (opportunityList.size()==0) return;
		
		String composite = "";
		for (SFSCOpportunity sfscOpportunity : opportunityList) {
			sfscOpportunity.setName(client.getPrenom() + client.getNom());
			
			composite += ",{"
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/Opportunity/" + sfscOpportunity.getId() + "\","
					+ "\"referenceId\": \"" + sfscOpportunity.getId() + "\","
					+ "\"body\": {"
					+ "\"Name\":\""+sfscOpportunity.getName()+"\""
					+ "}"
					+ "}";
			logger.trace ("anonymiseOpportunities\n" + composite);
		}
		
		composite = "{\r\n"
				+ "    \"allOrNone\": false,\r\n"
				+ "    \"compositeRequest\": [\r\n"
				+ composite.substring(1)
				+ "   ]\r\n"
				+ "}";

		logger.info ("Anonymise Opportunity\n" + composite);
		Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info ("Anonymise Opportunity\n" + response.getStatusCode());		
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
		
		String composite = "";
		for (SFSCQuote sfscQuote : quoteList) {
			sfscQuote.setInternalObservations__c("");
			sfscQuote.setShippingMainPhone__c (client.getTelephone1());
			sfscQuote.setShippingSecondPhone__c (client.getTelephone2());
			sfscQuote.setShippingCompanyName__c ("");
			sfscQuote.setShippingFirstName__c (client.getPrenom());
			sfscQuote.setShippingLastName__c (client.getNom());
			sfscQuote.setShippingSpecificConstrains__c ("");
			sfscQuote.setObservations__c("");
			sfscQuote.setBillingSecondPhone__c(client.getTelephone1Livraison());
			sfscQuote.setBillingMainPhone__c(client.getTelephone2Livraison());
			sfscQuote.setBillingLastName__c(client.getNom());
			sfscQuote.setBillingFirstName__c(client.getPrenom());
			sfscQuote.setBillingEmail__c(client.getEmail());
			
			composite += ",{"
					
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/Quote__c/" + sfscQuote.getId() + "\","
					+ "\"referenceId\": \"" + sfscQuote.getId() + "\","
					+ "\"body\": "
					+ sfscQuote.toPayload()
					+ "}";
		}
		
		composite = "{\r\n"
				+ "    \"allOrNone\": false,\r\n"
				+ "    \"compositeRequest\": [\r\n"
				+ composite.substring(1)
				+ "   ]\r\n"
				+ "}";

		logger.debug ("Anonymise Quote\n" + composite);
		Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info ("Anonymise Quote\n" + response.getStatusCode());		
	}


	private void anonymiseCustomerOrders(ClientEligible client, List<SFSCCustomerOrder> customerOrderList) throws JsonMappingException, JsonProcessingException {
		if (customerOrderList.size()==0) return;
		
		String composite = "";
		for (SFSCCustomerOrder sfscCustomerOrder : customerOrderList) {

			customerOrderUuids.add(sfscCustomerOrder.getExternalId__c());
			
			sfscCustomerOrder.setInternalObservations__c("");
			sfscCustomerOrder.setShippingTrackingLink__c("");
			sfscCustomerOrder.setShippingMainPhone__c (client.getTelephone1());
			sfscCustomerOrder.setShippingSecondPhone__c (client.getTelephone2());
			sfscCustomerOrder.setAnonymizationDate__c (LocalDateTime.now());
			sfscCustomerOrder.setShippingCompanyName__c ("");
			sfscCustomerOrder.setShippingFirstName__c (client.getPrenom());
			sfscCustomerOrder.setShippingLastName__c (client.getNom());
			sfscCustomerOrder.setShippingSpecificConstrains__c ("");
			sfscCustomerOrder.setObservations__c("");
			sfscCustomerOrder.setBillingSecondPhone__c(client.getTelephone1Livraison());
			sfscCustomerOrder.setBillingMainPhone__c(client.getTelephone2Livraison());
			sfscCustomerOrder.setBillingLastName__c(client.getNom());
			sfscCustomerOrder.setBillingFirstName__c(client.getPrenom());
			sfscCustomerOrder.setBillingEmail__c(client.getEmail());
			
			composite += ",{"
					+ "\"method\": \"PATCH\"," 
					+ "\"url\": \"/services/data/v61.0/sobjects/CustomerOrder__c/" + sfscCustomerOrder.getId() + "\","
					+ "\"referenceId\": \"" + sfscCustomerOrder.getId() + "\","
					+ "\"body\": "
					+ sfscCustomerOrder.toPayload()
					+ "}";
		}
		
		composite = "{\r\n"
				+ "    \"allOrNone\": false,\r\n"
				+ "    \"compositeRequest\": [\r\n"
				+ composite.substring(1)
				+ "   ]\r\n"
				+ "}";

		logger.debug (composite);
		Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info ("Anonymise customer orders : " + response.getStatusCode());
		
//		List<String> supplierOrderIds = new ArrayList<String>();
//		fillSupplierOrderList (supplierOrderIds, customerOrderList);
//		anonymiseSupplierOrders (client, supplierOrderIds);
	}


	@SuppressWarnings("unused")
	private void anonymiseSupplierOrders(ClientEligible client, List<String> supplierOrderIds) throws JsonMappingException, JsonProcessingException {
		List<String> compositeCalls = new ArrayList<String>();
		for (String supplierOrderId : supplierOrderIds) {
			compositeCalls.add("        {\r\n"
					+ "            \"method\": \"GET\",\r\n"
					+ "            \"url\": \"/services/data/v61.0/sobjects/SupplierOrder__c/" + supplierOrderId + "\",\r\n"
					+ "            \"referenceId\": \""+supplierOrderId+"\"\r\n"
					+ "        }");
		}
		
		if (compositeCalls.size()>0) {
			List<ResponseEntity<String>> responses = sfscClient.callComposite(compositeCalls);
		
			List<String> customerOrderLineIds = new ArrayList<String>();
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("customerOrderLineIds\n" + response.getBody());
					
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
										JsonNode idNode = record.get("Id");
										if ((idNode!=null)&&(!idNode.isNull())) {
											customerOrderLineIds.add(idNode.asText());											
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

	private SFSCQuote getQuote (String quoteId) throws JsonMappingException, JsonProcessingException {

		Callable<ResponseEntity<String>> job = sfscClient.getQuoteByUuid(quoteId);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (response.getStatusCode()==HttpStatus.OK) {
			logger.debug ("GET Quote response : " + response.getBody());
			
			JsonNode bodyNode = mapper.readTree(response.getBody());

			if ((bodyNode!=null)&&(!bodyNode.isNull())) {
				SFSCQuote quote = SFSCCustomerHelper.getInstance().getSFSCQuote(bodyNode);
				return quote;
			}
		} else {
			logger.error("Quote Search Error : " + response.getStatusCode() + " : " + quoteId);
		}
		return null;
	}
	
	private List<SFSCQuote> getAllQuotes(Map<String, String> quoteIds) throws JsonMappingException, JsonProcessingException {
		String composite = "";
		List<SFSCQuote> quoteList = new ArrayList<SFSCQuote>();
		for (Entry<String, String> customerOrderItem : quoteIds.entrySet()) {
			composite += ",{\"method\": \"GET\",\"url\": \""+customerOrderItem.getValue()+"\",\"referenceId\": \""+ customerOrderItem.getKey() +"\"}";
		}
		
		if (composite.length()==0) {
			return quoteList;
		} else {
			composite = "{\r\n"
					+ "    \"allOrNone\": false,\r\n"
					+ "    \"compositeRequest\": [\r\n"
					+ composite.substring(1)
					+ "   ]\r\n"
					+ "}";
		}
		logger.trace (composite);
		Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (response.getStatusCode()==HttpStatus.OK) {
			logger.debug (response.getBody());
			
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
		return quoteList;
	}

	private List<SFSCOpportunity> getAllOpportunities(Map<String, String> opportunitiesIds) throws JsonMappingException, JsonProcessingException {
		String composite = "";
		List<SFSCOpportunity> opportunityList = new ArrayList<SFSCOpportunity>();
		for (Entry<String, String> customerOrderItem : opportunitiesIds.entrySet()) {
			composite += ",{\"method\": \"GET\",\"url\": \""+customerOrderItem.getValue()+"\",\"referenceId\": \""+ customerOrderItem.getKey() +"\"}";
		}
		
		if (composite.length()==0) {
			return opportunityList;
		} else {
			composite = "{\r\n"
					+ "    \"allOrNone\": false,\r\n"
					+ "    \"compositeRequest\": [\r\n"
					+ composite.substring(1)
					+ "   ]\r\n"
					+ "}";
		}
		logger.debug ("Opportunity : \n" + composite);
		Callable<ResponseEntity<String>> job = sfscClient.callComposite(composite);
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (response.getStatusCode()==HttpStatus.OK) {
			logger.debug ("Opportunity : \n" + response.getBody());
			
			JsonNode sfscLinkedObjectIDsNode = mapper.readTree(response.getBody());
			
			JsonNode compositeResponseNode = sfscLinkedObjectIDsNode.get("compositeResponse");
			for (JsonNode linkedObjectList : compositeResponseNode) {
				JsonNode referenceObjectNode = linkedObjectList.get("referenceId");
				if ((referenceObjectNode!=null)&&(!referenceObjectNode.isNull())) {
					JsonNode bodyNode = linkedObjectList.get("body"); 
					if ((bodyNode!=null)&&(!bodyNode.isNull())) {
						SFSCOpportunity opportunity = SFSCCustomerHelper.getInstance().getSFSCOpportunity (bodyNode);
						opportunityList.add(opportunity);
					}
				}
			}
		}
		return opportunityList;
	}
	
	private List<SFSCCustomerOrder> getAllCustomerOrders(Map<String, String> customerOrderIds) throws JsonMappingException, JsonProcessingException {
		List<String> compositeCalls = new ArrayList<String>();
		List<SFSCCustomerOrder> customerOrderList = new ArrayList<SFSCCustomerOrder>();
		for (Entry<String, String> customerOrderItem : customerOrderIds.entrySet()) {
			compositeCalls.add("{\"method\": \"GET\",\"url\": \""+customerOrderItem.getValue()+"\",\"referenceId\": \""+ customerOrderItem.getKey() +"\"}");
		}
		
		if (compositeCalls.size()==0) {
			return customerOrderList;
		} else {
			List<ResponseEntity<String>> responses = sfscClient.callComposite(compositeCalls);
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug (response.getBody());
					
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

	@SuppressWarnings("unused")
	private void fillSupplierOrderList(List<String> supplierOrderIds, List<SFSCCustomerOrder> customerOrderList) throws JsonMappingException, JsonProcessingException {
		List<String> compositeCalls = new ArrayList<String>();
		for (SFSCCustomerOrder customerOrder : customerOrderList) {
			compositeCalls.add("        {\r\n"
					+ "            \"method\": \"GET\",\r\n"
					+ "            \"url\": \"/services/data/v61.0/query?q=SELECT Id FROM CustomerOrderLine__c WHERE CustomerOrder__c  = '"+customerOrder.getId()+"'\",\r\n"
					+ "            \"referenceId\": \""+customerOrder.getId()+"\"\r\n"
					+ "        }");
		}
		
		if (compositeCalls.size()>0) {
			List<ResponseEntity<String>> responses = sfscClient.callComposite(compositeCalls);
		
			List<String> customerOrderLineIds = new ArrayList<String>();
			
			for (ResponseEntity<String> response : responses) {
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug ("customerOrderLineIds\n" + response.getBody());
					
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
										JsonNode idNode = record.get("Id");
										if ((idNode!=null)&&(!idNode.isNull())) {
											customerOrderLineIds.add(idNode.asText());											
										}
									}
								}
							}
						}
					}
				}
			}
			
			if (customerOrderLineIds.size()>0) {
				compositeCalls = new ArrayList<String>();
				for (String customerOrderLineId : customerOrderLineIds) {
					compositeCalls.add("        {\r\n"
							+ "            \"method\": \"GET\",\r\n"
							+ "            \"url\": \"/services/data/v61.0/query?q=SELECT SupplierOrderLine__c FROM CustomerSupplierOrderLine__c WHERE CustomerOrderLine__c = '"+ customerOrderLineId +"'\",\r\n"
							+ "            \"referenceId\": \""+customerOrderLineId+"\"\r\n"
							+ "        }");
				}

				if (compositeCalls.size()>0) {
					responses = sfscClient.callComposite(compositeCalls);
				
					List<String> suplierOrderLineIds = new ArrayList<String>();
					
					for (ResponseEntity<String> response : responses) {
						if (response.getStatusCode()==HttpStatus.OK) {
							logger.debug ("suplierOrderLineIds \n" + response.getBody());
							
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
												JsonNode idNode = record.get("SupplierOrderLine__c");
												if ((idNode!=null)&&(!idNode.isNull())) {
													suplierOrderLineIds.add(idNode.asText());											
												}
											}
										}
									}
								}
							}
						}
					}
					if (suplierOrderLineIds.size()>0) {
						compositeCalls = new ArrayList<String>();
						for (String suplierOrderLineId : suplierOrderLineIds) {
							compositeCalls.add("        {\r\n"
									+ "            \"method\": \"GET\",\r\n"
									+ "            \"url\": \"/services/data/v61.0/query?q=SELECT SupplierOrder__c FROM SupplierOrderLine__c WHERE Id  = '"+suplierOrderLineId+"'\",\r\n"
									+ "            \"referenceId\": \""+suplierOrderLineId+"\"\r\n"
									+ "        }");
						}
	
						if (compositeCalls.size()>0) {
							responses = sfscClient.callComposite(compositeCalls);
													
							for (ResponseEntity<String> response : responses) {
								if (response.getStatusCode()==HttpStatus.OK) {
									logger.debug ("suplierOrderIds \n" + response.getBody());
									
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
														JsonNode idNode = record.get("SupplierOrder__c");
														if ((idNode!=null)&&(!idNode.isNull())) {
															supplierOrderIds.add(idNode.asText());											
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
				}
			}
		}
	}
	
	private SFSCCustomerOrder getCustomerOrders(String customerOrderId) throws JsonMappingException, JsonProcessingException {
		
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
	
	public List<String> getUnsubsciptionToDotDigital() {
		return unsubsciptionToDotDigital;
	}
	public List<String> getCustomerOrderIds() {
		return customerOrderUuids;
	}
	public void anonymizeCustomerOrder(ClientPartiel clientItem) throws JsonMappingException, JsonProcessingException {
		SFSCCustomerOrder order = getCustomerOrders(clientItem.getUuidCc());
		if (order!=null) {
			List<SFSCCustomerOrder> orderList = new ArrayList<SFSCCustomerOrder>();
			orderList.add(order);
			anonymiseCustomerOrders(clientItem, orderList); 			
		}
	}
	
}
