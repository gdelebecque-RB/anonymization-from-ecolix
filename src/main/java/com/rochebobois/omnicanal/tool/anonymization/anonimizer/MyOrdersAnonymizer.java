package com.rochebobois.omnicanal.tool.anonymization.anonimizer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import com.rochebobois.omnicanal.canonical.domain.customer.CustomerAddress;
import com.rochebobois.omnicanal.canonical.domain.customerorder.AnonymizationReasonEnum;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrder;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderBilling;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderContact;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderCustomer;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderDelivery;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderInvoice;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderSystem;
import com.rochebobois.omnicanal.myorders.api.MyOrdersEndpointParameters;
import com.rochebobois.omnicanal.myorders.api.MyOrdersInstance;
import com.rochebobois.omnicanal.myorders.api.MyOrdersRestClient;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersCustomerOrderAPI;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersHelper;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersInvoice;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;

public class MyOrdersAnonymizer {
	private static final Logger logger = LoggerFactory.getLogger(MyOrdersAnonymizer.class);
	
	private static MyOrdersRestClient myOrdersClient = null;
	private static ObjectMapper mapper = new ObjectMapper();

	private Environment currentEnv = null;
	private LocalDate referenceDate = null;
	
	DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	public MyOrdersAnonymizer(Environment currentEnv, LocalDate referenceDate) throws ApiClientException {
		setCurrentEnv(currentEnv);
		setReferenceDate(referenceDate);
		
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

	public void AnonymizeCustomerOrders(ClientEligible client, List<String> customerOrderIds) throws JsonMappingException, JsonProcessingException {
		
		for (String customerOrderId : customerOrderIds) {
			
			Callable<ResponseEntity<String>> job = myOrdersClient.getAllCustomerOrderByFilter("uuid", customerOrderId);
			ResponseEntity<String> response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (response.getStatusCode()==HttpStatus.OK) {
				JsonNode customerOrderResultNode = mapper.readTree(response.getBody());
				
				JsonNode totalNode = customerOrderResultNode.get("total");
				if ((totalNode!=null)&&(!totalNode.isNull())) {
					int nbResult = totalNode.asInt();
					if (nbResult==0) {
						logger.warn("MyOrders Customer Order : " + customerOrderId + " not found ");
					} else if (nbResult>1) {
						logger.error ("MyOrders Customer Order : " + customerOrderId + " Multiple occurence found - No occurence procedeed");
					} else {	
						JsonNode dataNode = customerOrderResultNode.get("data");
						if ((dataNode!=null)&&(!dataNode.isNull())) {
							for (JsonNode dataOccurenceNode : dataNode) {
								String id = dataOccurenceNode.get("id").asText();

								AnonymizeCustomerOrder (client, id);
//								job = myOrdersClient.getCustomerOrder(id, "lines,lines.discountType,lines.orderType,lines.status,lines.supplierConfirmation,lines.stock.stockStatus,lines.stock.supplierLines.supplierOrder,lines.stock.customerOrderLine.order,deliveryAddress,deliveryAddress.civilite,deliveryAddress.housingType,deliveryAddress.country,deliveryAddress.state,billingAddress,billingAddress.housingType,billingAddress.country,billingAddress.state,store,store.brand,type,status,currency,saleNature,discountType,cancellationReason,creditNoteReason,channel,saleStore,saleType,countryLanguage,orderPayments,orderPayments.reglementType,orderPayments.paymentType,customerType,zoneStore");
//								response = null;
//								try {
//									response = job.call();
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//								System.out.println(response.getBody());
//								if (response.getStatusCode()==HttpStatus.OK) {
//									JsonNode bodyNode = mapper.readTree(response.getBody());
//									MyOrdersCustomerOrderAPI moCustomer = new MyOrdersCustomerOrderAPI(bodyNode);
//									CustomerOrder customerOrder =  MyOrdersHelper.getCanonicalCustomerOrder (moCustomer);
//
//									customerOrder.addOtherId("MyOrders", id);
//									
//									job = myOrdersClient.getCustomerOrderInvoices(id);
//									response = null;
//									try {
//										response = job.call();
//									} catch (Exception e) {
//										e.printStackTrace();
//									}
//								
//									if (response.getStatusCode()==HttpStatus.OK) {
//										JsonNode invoicesNode = mapper.readTree(response.getBody());
//										List<CustomerOrderInvoice> invoices = new ArrayList<CustomerOrderInvoice>();
//										
//										for (JsonNode invoiceNode : invoicesNode) {
//											MyOrdersInvoice moInvoice = new MyOrdersInvoice (invoiceNode);
//											CustomerOrderInvoice invoice = MyOrdersHelper.getCanonicalCustomerInvoice (moInvoice, moCustomer.getCode_base_ecolix());
//											invoices.add(invoice);
//										}
//										
//										customerOrder.setInvoice(invoices);
//									}
//
//									System.out.println(customerOrder);
//									AnonymiseCustomerOrder (customerOrder, client);
//									System.out.println(customerOrder);
//								
////									job = myOrdersClient.putCustomerOrder(id, customerOrder);
//									job = myOrdersClient.anonymizeCustomerOrder(customerOrder);
//									response = null;
//									try {
//										response = job.call();
//									} catch (Exception e) {
//										e.printStackTrace();
//									}
//									
//									System.out.println(response.getStatusCode());
//									System.out.println(response.getBody());
//									System.out.println("----------------------------------- Customer Order Anonymization Ends ----------------------------------- ");									
//								}
							}
						}
					}
				}
			} else {
				logger.error("Mule Erreur " + response.getStatusCode() + " " + customerOrderId);
			}
		}	
	}
	
	private void AnonymizeCustomerOrder (ClientEligible client, String customerOrderId) throws JsonMappingException, JsonProcessingException {
		Callable<ResponseEntity<String>> job = myOrdersClient.getCustomerOrder(customerOrderId, "lines,lines.discountType,lines.orderType,lines.status,lines.supplierConfirmation,lines.stock.stockStatus,lines.stock.supplierLines.supplierOrder,lines.stock.customerOrderLine.order,deliveryAddress,deliveryAddress.civilite,deliveryAddress.housingType,deliveryAddress.country,deliveryAddress.state,billingAddress,billingAddress.housingType,billingAddress.country,billingAddress.state,store,store.brand,type,status,currency,saleNature,discountType,cancellationReason,creditNoteReason,channel,saleStore,saleType,countryLanguage,orderPayments,orderPayments.reglementType,orderPayments.paymentType,customerType,zoneStore");
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(response.getBody());
		if (response.getStatusCode()==HttpStatus.OK) {
			JsonNode bodyNode = mapper.readTree(response.getBody());
			MyOrdersCustomerOrderAPI moCustomer = new MyOrdersCustomerOrderAPI(bodyNode);
			CustomerOrder customerOrder =  MyOrdersHelper.getCanonicalCustomerOrder (moCustomer);

			customerOrder.addOtherId("MyOrders", customerOrderId);
			
			job = myOrdersClient.getCustomerOrderInvoices(customerOrderId);
			response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
		
			if (response.getStatusCode()==HttpStatus.OK) {
				JsonNode invoicesNode = mapper.readTree(response.getBody());
				List<CustomerOrderInvoice> invoices = new ArrayList<CustomerOrderInvoice>();
				
				for (JsonNode invoiceNode : invoicesNode) {
					MyOrdersInvoice moInvoice = new MyOrdersInvoice (invoiceNode);
					CustomerOrderInvoice invoice = MyOrdersHelper.getCanonicalCustomerInvoice (moInvoice, moCustomer.getCode_base_ecolix());
					invoices.add(invoice);
				}
				
				customerOrder.setInvoice(invoices);
			}

			System.out.println(customerOrder);
			ApplyAnonymisation (customerOrder, client);
			System.out.println(customerOrder);
		
//			job = myOrdersClient.putCustomerOrder(id, customerOrder);
			job = myOrdersClient.anonymizeCustomerOrder(customerOrder);
			response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println(response.getStatusCode());
			System.out.println(response.getBody());
			System.out.println("----------------------------------- Customer Order Anonymization Ends ----------------------------------- ");									
		}
	}

	private void ApplyAnonymisation(CustomerOrder customerOrder, ClientEligible client) {
		CustomerOrderBilling billing = customerOrder.getBilling();
		CustomerAddress address = billing.getAddress();
		address.setAddressFirstLine(client.getAdressStreetLine1());
		address.setAddressSecondLine(client.getAdressStreetLine2());
		address.setPostalCode(client.getAdressPostalCode());
		address.setCity(client.getAdressCity());
		address.setState(null);
				
		CustomerOrderCustomer customer = customerOrder.getCustomer();
		customer.setSalutation(client.getSalutation());
		customer.setFirstName(client.getPrenom());
		customer.setLastName(client.getNom());
		customer.setCompanyName(client.getRaisonSociale());
		
		CustomerOrderContact contact = customer.getContact();
		contact.setEmail(client.getEmail());
		contact.setPreferedPhoneNumber(client.getTelephone1());
		contact.setSecondPhoneNumber(client.getTelephone2());
		
		CustomerOrderDelivery delivery = customerOrder.getDelivery();
		delivery.setReceiverSalutation(client.getSalutation());
		delivery.setReceiverFirstName(client.getPrenom());
		delivery.setReceiverLastName(client.getPrenom());
		delivery.setReceiverCompanyName(client.getRaisonSociale());
		address = delivery.getAddress();
		address.setAddressFirstLine(client.getAdressStreetLine1());
		address.setAddressSecondLine(client.getAdressStreetLine2());
		address.setPostalCode(client.getAdressPostalCode());
		address.setCity(client.getAdressCity());
		address.setState(null);
		
		contact = delivery.getContact();
		contact.setEmail(client.getEmail());
		contact.setPreferedPhoneNumber(client.getTelephone1());
		contact.setSecondPhoneNumber(client.getTelephone2());
		
		CustomerOrderSystem system = customerOrder.getSystem();
		system.setAnonymizationDate(LocalDateTime.now());
		system.setAnonymizationReason (AnonymizationReasonEnum.byExpiration);
		
	}
	
	public void AnonymizeCustomerCustomerOrders(ClientEligible client) throws JsonMappingException, JsonProcessingException {
		
		Callable<ResponseEntity<String>> job = myOrdersClient.getAllCustomerOrderByCustomerId(client.getCodeClient());
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (response.getStatusCode()==HttpStatus.OK) {
			JsonNode customerOrderResultNode = mapper.readTree(response.getBody());
			
			JsonNode totalNode = customerOrderResultNode.get("total");
			if ((totalNode!=null)&&(!totalNode.isNull())) {
				int nbResult = totalNode.asInt();
				if (nbResult==0) {
					logger.warn("MyOrders Customer : " + client.getCodeClient() + " no customer order found");
				} else {	
					logger.warn("MyOrders Customer : " + client.getCodeClient() + " " + nbResult + " order(s) found");
					JsonNode dataNode = customerOrderResultNode.get("data");
					if ((dataNode!=null)&&(!dataNode.isNull())) {
						for (JsonNode dataOccurenceNode : dataNode) {
							String id = dataOccurenceNode.get("id").asText();

							AnonymizeCustomerOrder (client, id);

						}
					}
				}
			}
		} else {
			logger.error("Mule Erreur " + response.getStatusCode() + " " + client.getCodeClient());
		}
	}
}
