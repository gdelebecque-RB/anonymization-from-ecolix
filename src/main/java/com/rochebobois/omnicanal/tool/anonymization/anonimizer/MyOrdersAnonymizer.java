package com.rochebobois.omnicanal.tool.anonymization.anonimizer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.rochebobois.omnicanal.api.Environment;
import com.rochebobois.omnicanal.api.exception.ApiClientException;
import com.rochebobois.omnicanal.api.httpclient.HttpStatus;
import com.rochebobois.omnicanal.api.httpclient.ResponseEntity;
import com.rochebobois.omnicanal.mule.api.MuleAppEnum;
import com.rochebobois.omnicanal.mule.api.MuleAppInstance;
import com.rochebobois.omnicanal.mule.api.MuleEndpointParameters;
import com.rochebobois.omnicanal.mule.api.PapiOrderRestClient;
import com.rochebobois.omnicanal.myorders.api.MyOrdersEndpointParameters;
import com.rochebobois.omnicanal.myorders.api.MyOrdersInstance;
import com.rochebobois.omnicanal.myorders.api.MyOrdersRestClient;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;

public class MyOrdersAnonymizer {
	private static final Logger logger = LoggerFactory.getLogger(MyOrdersAnonymizer.class);
	
	private static PapiOrderRestClient papiOrderClient = null;
	private static MyOrdersRestClient myOrdersClient = null;
	private static ObjectMapper mapper = new ObjectMapper();

	private Environment currentEnv = null;
	private LocalDate referenceDate = null;
	
	public MyOrdersAnonymizer(Environment currentEnv, LocalDate referenceDate) throws ApiClientException {
		setCurrentEnv(currentEnv);
		setReferenceDate(referenceDate);
		
		MuleAppInstance muleAppInstance = new MuleAppInstance(currentEnv);
		MuleEndpointParameters muleParam = muleAppInstance.getMuleEndpointParameters(MuleAppEnum.PapiOrder);
		papiOrderClient = new PapiOrderRestClient(muleParam);
		
		MyOrdersInstance myOrdersInstance = MyOrdersInstance.environment(currentEnv);
		MyOrdersEndpointParameters myOrderParam = myOrdersInstance.getEndpointParameters();
		myOrdersClient = new MyOrdersRestClient(myOrderParam);
		
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

	public void AnonymizeCustomer(ClientEligible client, List<String> customerOrderIds) throws JsonMappingException, JsonProcessingException {
		
		for (String customerOrderId : customerOrderIds) {
			Callable<ResponseEntity<String>> job = papiOrderClient.getCustomerOrder(customerOrderId);
			ResponseEntity<String> response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (response.getStatusCode()==HttpStatus.OK) {
				JsonNode customerOrderNode = mapper.readTree(response.getBody());

				ObjectNode customerNode = (ObjectNode) customerOrderNode.get("customer");
				if ((customerNode!=null)&&(!customerNode.isNull())) {
					JsonNode jsonNode = null;
					jsonNode = customerNode.get("lastName");
					if ((jsonNode!=null)&&(!jsonNode.isNull())) {
						customerNode.set("lastName", new TextNode (client.getNom()));						
					}
					jsonNode = customerNode.get("firstName");
					if ((jsonNode!=null)&&(!jsonNode.isNull())) {
						customerNode.set("firstName", new TextNode (client.getPrenom()));
					}
					
					ObjectNode contactNode = (ObjectNode) customerNode.get("contact");
					if ((contactNode!=null)&&(!contactNode.isNull())) {
						jsonNode = customerNode.get("preferedPhoneNumber");
						if ((jsonNode!=null)&&(!jsonNode.isNull())) {
							contactNode.set("preferedPhoneNumber", new TextNode (client.getTelephone1()) );
						}
						jsonNode = customerNode.get("secondPhoneNumber");
						if ((jsonNode!=null)&&(!jsonNode.isNull())) {
							contactNode.set("secondPhoneNumber", new TextNode (client.getTelephone2()) );
						}
						jsonNode = customerNode.get("email");
						if ((jsonNode!=null)&&(!jsonNode.isNull())) {
							contactNode.set("email", new TextNode (client.getEmail()) );
						}
					}
				}
				ObjectNode deliveryNode = (ObjectNode) customerOrderNode.get("delivery");
				if ((deliveryNode!=null)&&(!deliveryNode.isNull())) {
					JsonNode jsonNode = null;
					jsonNode = deliveryNode.get("receiverLastName");
					if ((jsonNode!=null)&&(!jsonNode.isNull())) {
						deliveryNode.set("receiverLastName", new TextNode (client.getNom()));
					}
					jsonNode = deliveryNode.get("receiverFirstName");
					if ((jsonNode!=null)&&(!jsonNode.isNull())) {
						deliveryNode.set("receiverFirstName", new TextNode (client.getPrenom()));
					}
					
					ObjectNode contactNode = (ObjectNode) deliveryNode.get("contact");
					if ((contactNode!=null)&&(!contactNode.isNull())) {
						jsonNode = deliveryNode.get("preferedPhoneNumber");
						if ((jsonNode!=null)&&(!jsonNode.isNull())) {
							contactNode.set("preferedPhoneNumber", new TextNode (client.getTelephone1()) );
						}
						jsonNode = deliveryNode.get("secondPhoneNumber");
						if ((jsonNode!=null)&&(!jsonNode.isNull())) {
							contactNode.set("secondPhoneNumber", new TextNode (client.getTelephone2()) );
						}
						jsonNode = deliveryNode.get("email");
						if ((jsonNode!=null)&&(!jsonNode.isNull())) {
							contactNode.set("email", new TextNode (client.getEmail()) );
						}
					}
				}
				JsonNode systemNode = customerOrderNode.get("system");
				ObjectNode objectNode = (ObjectNode) systemNode;
				objectNode.put("anonymizationReason", "byExpiration");
				DateTimeFormatter DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				objectNode.put("anonymizationDate", referenceDate.format(DateFormat));
				
				job =  myOrdersClient.putCustomerOrder(customerOrderNode.toPrettyString());
				try {
					response = job.call();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (response.getStatusCode()==HttpStatus.OK) {
					logger.debug("MyOrders Customer Order : " + customerOrderId + " Anonymized ");
				} else {
					logger.error("MyOrders Erreur " + response.getStatusCode() + " " + customerOrderId);
				}
				
			} else {
				logger.error("Mule Erreur " + response.getStatusCode() + " " + customerOrderId);
			}
		}
	}
}
