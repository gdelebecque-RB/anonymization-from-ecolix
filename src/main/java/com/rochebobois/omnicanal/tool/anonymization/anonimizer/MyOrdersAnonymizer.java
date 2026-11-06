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
import com.rochebobois.omnicanal.canonical.domain.OtherId;
import com.rochebobois.omnicanal.canonical.domain.customer.CustomerAddress;
import com.rochebobois.omnicanal.canonical.domain.customerorder.AnonymizationReasonEnum;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrder;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderBilling;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderContact;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderCustomer;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderDelivery;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderInvoice;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderSupplierOrderLine;
import com.rochebobois.omnicanal.canonical.domain.customerorder.CustomerOrderSystem;
import com.rochebobois.omnicanal.canonical.domain.stock.Stock;
import com.rochebobois.omnicanal.canonical.domain.suplierorder.SupplierOrder;
import com.rochebobois.omnicanal.canonical.domain.suplierorder.SupplierOrderLine;
import com.rochebobois.omnicanal.myorders.api.MyOrdersEndpointParameters;
import com.rochebobois.omnicanal.myorders.api.MyOrdersInstance;
import com.rochebobois.omnicanal.myorders.api.MyOrdersRestClient;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersCustomerOrderAPI;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersHelper;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersInvoice;
import com.rochebobois.omnicanal.myorders.domain.api.MyOrdersSupplierOrderAPI;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.report.Report;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportBook;
import com.rochebobois.omnicanal.tool.anonymization.report.ReportGravity;

public class MyOrdersAnonymizer {
	private static final Logger logger = LoggerFactory.getLogger(MyOrdersAnonymizer.class);
	
	private static MyOrdersRestClient myOrdersClient = null;
	private static ObjectMapper mapper = new ObjectMapper();

	private Environment currentEnv = null;
	private LocalDate referenceDate = null;
	private ClientEligible currentCustomer = null;
	DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private List<String> supplierOrderMyOrdersId = null;
	
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
		currentCustomer = client;
		
		logger.debug ("- MyOrders - Anonymise " + customerOrderIds.size() + "customer orders ");
		
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
						ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Customer Order", customerOrderId, "", ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
					} else if (nbResult>1) {
						logger.error ("MyOrders Customer Order : " + customerOrderId + " Multiple occurence found - No occurence procedeed");
					} else {	
						JsonNode dataNode = customerOrderResultNode.get("data");
						if ((dataNode!=null)&&(!dataNode.isNull())) {
							for (JsonNode dataOccurenceNode : dataNode) {
								String id = dataOccurenceNode.get("id").asText();

								AnonymizeCustomerOrder (id);

							}
						}
					}
				}
			} else {
				logger.error("Mule Erreur " + response.getStatusCode() + " " + customerOrderId);
			}
		}	
	}
	
	private void AnonymizeCustomerOrder (String customerOrderId) throws JsonMappingException, JsonProcessingException {
		logger.info ("- MyOrders - Anonymise customer orders " + customerOrderId);
		supplierOrderMyOrdersId = new ArrayList<String>();
		
		Callable<ResponseEntity<String>> job = myOrdersClient.getCustomerOrder(customerOrderId, "lines,lines.discountType,lines.orderType,lines.status,lines.supplierConfirmation,lines.stock.stockStatus,lines.stock.supplierLines.supplierOrder,lines.stock.customerOrderLine.order,deliveryAddress,deliveryAddress.civilite,deliveryAddress.housingType,deliveryAddress.country,deliveryAddress.state,billingAddress,billingAddress.housingType,billingAddress.country,billingAddress.state,store,store.brand,type,status,currency,saleNature,discountType,cancellationReason,creditNoteReason,channel,saleStore,saleType,countryLanguage,orderPayments,orderPayments.reglementType,orderPayments.paymentType,customerType,zoneStore");
		ResponseEntity<String> response = null;
		try {
			response = job.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (response.getStatusCode()==HttpStatus.OK) {
			logger.info ("-- MyOrders - Customer orders - getCustomerOrder - Response - OK - " + response.getBody().replace("\n", "").replace("\r","") );

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

			for (CustomerOrderSupplierOrderLine customerOrderLine : customerOrder.getSupplierOrderLines()) {
				if (!supplierOrderMyOrdersId.contains(customerOrderLine.getMyoSupplierOrderId()) ) {
					supplierOrderMyOrdersId.add(customerOrderLine.getMyoSupplierOrderId());
				}
			}
			
			ApplyAnonymisation (customerOrder);
		
			job = myOrdersClient.anonymizeCustomerOrder(customerOrder);
			response = null;
			try {
//				response = job.call();
			} catch (Exception e) {
				logger.error ("-- MyOrders - Supplier orders Anonymization - " + customerOrderId + " - Response - FAILED - " + e.getMessage()  );		
				ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Customer Order", "Anonymize", customerOrderId, ReportGravity.ERROR, "Exception : " + e.getMessage() ));
				e.printStackTrace();
			}

			List<OtherId> otherId = customerOrder.searchOtherId("Ecolix");
			String ecolixId = "";
			if (otherId.size()>0) {
				ecolixId = otherId.get(0).getSystemId() + "-" + otherId.get(0).getLocalId();
			}

//			if (response.getStatusCode()==HttpStatus.OK) {
//				logger.error ("-- MyOrders - Customer orders - " + customerOrderId + " (" + ecolixId + ") - Response - SUCCESS - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", ""));		
//				ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Customer Order", ecolixId, customerOrderId, ReportGravity.OK, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
//			} else {				
//				logger.error ("-- MyOrders - Customer orders - " + customerOrderId + " (" + ecolixId + ") - Response - FAILED - " + response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "")  );		
//				ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Customer Order", ecolixId, customerOrderId, ReportGravity.ERROR, response.getStatusCode() + " - " + response.getBody().replace("\n", "").replace("\r", "") ));
//			}

		}
		
		System.out.println("----------------------------------- Customer Order Anonymization Ends ----------------------------------- ");									
		
		logger.error ("-- MyOrders - Supplier orders - getSupplierOrder - " + supplierOrderMyOrdersId.size() );

		for (String supplierOrderMyOrders : supplierOrderMyOrdersId) {
			job = myOrdersClient.getSupplierOrder(supplierOrderMyOrders, "supplierLines,supplierLines.status,SaleType,store,supplierLines.supplierConfirmation,supplierLines.discountType,supplierLines.stock,supplierLines.stock.supplierLines.supplierOrder,supplierLines.stock.customerOrderLine.order");
			response = null;
			try {
				response = job.call();
			} catch (Exception e) {
				logger.error ("-- MyOrders - Supplier orders - " + supplierOrderMyOrders + " - Response - FAILED - " + e.getMessage()  );		
				ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Supplier Order", supplierOrderMyOrders, "", ReportGravity.ERROR, "Exception : " + e.getMessage() ));
				e.printStackTrace();
			}
			
			if (response.getStatusCode()==HttpStatus.OK) {
				logger.info ("-- MyOrders - Supplier orders - getSupplierOrder - Response - OK - " + response.getBody().replace("\n", "").replace("\r","") );

				JsonNode bodyNode = mapper.readTree(response.getBody());
				MyOrdersSupplierOrderAPI moCustomer = new MyOrdersSupplierOrderAPI(bodyNode);
				SupplierOrder supplierOrder =  MyOrdersHelper.getCanonicalSupplierOrder (moCustomer);
				
				boolean isBackToBack = true;
				String CustomerOrderUuid = null;
				for (SupplierOrderLine orderLine : supplierOrder.getLine()) {
					for (Stock stock : orderLine.getStocks()) {
						if (stock.getCustomerOrderUuid()==null) {
							isBackToBack = false;
						} else {
							if(CustomerOrderUuid==null) {
								CustomerOrderUuid = stock.getCustomerOrderUuid();
							} else {
								if( stock.getCustomerOrderUuid().equals(CustomerOrderUuid)) {
									isBackToBack = false;
								}
							}							
						}
						
					}
					
				}
				if (isBackToBack) {
					supplierOrder.setOrderLabel(currentCustomer.getCodeClient() + " - " + currentCustomer.getNom() + " - " + currentCustomer.getPrenom());
					System.out.println(supplierOrder.toJsonPayload().toPrettyString());
					job = myOrdersClient.putSupplierOrder(supplierOrder);
					response = null;
					try {
						response = job.call();
					} catch (Exception e) {
						logger.error ("-- MyOrders - Supplier orders - Update " + supplierOrder.getUuid() + " - Response - FAILED - " + e.getMessage()  );		
						ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Supplier Order", supplierOrder.getUuid(), supplierOrder.getOtherId("MyOrders").getLocalId(), ReportGravity.ERROR, "Exception : " + e.getMessage() ));
						e.printStackTrace();
					}
					
					if (response.getStatusCode()==HttpStatus.OK) {
						logger.info ("-- MyOrders - Supplier orders - Update SupplierOrder - Response - OK - " + response.getBody().replace("\n", "").replace("\r","") );
						ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Supplier Order", supplierOrder.getUuid(), supplierOrder.getOtherId("MyOrders").getLocalId(), ReportGravity.OK, "" ));
					} else {
						logger.error ("-- MyOrders - Supplier orders - Update " + supplierOrder.getUuid() + " - Response - FAILED - " + response.getStatusCode() + " : " + response.getBody().replace("\n", "").replace("\r", "")  );		
						ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Supplier Order", supplierOrder.getUuid(), supplierOrder.getOtherId("MyOrders").getLocalId(), ReportGravity.ERROR, response.getStatusCode() + " : " + response.getBody().replace("\n", "").replace("\r", "") ));
					}
				} else {
					logger.error ("-- MyOrders - Supplier orders - "  + supplierOrder.getUuid() + "(" + supplierOrderMyOrders + ") Is Not Back to Back" );	
					ReportBook.getInstance().addReport (new Report(currentCustomer, "MyOrders", "Supplier Order", supplierOrder.getUuid(), supplierOrder.getOtherId("MyOrders").getLocalId(), ReportGravity.WARNING, "Supplier order " + supplierOrder.getUuid() + "(" + supplierOrderMyOrders + ") is not back to back. It cannot be anonymized." ));
				}
				
				supplierOrder.addOtherId("MyOrders", customerOrderId);
			}			
		}
		System.out.println("----------------------------------- Supplier Order Anonymization Ends ----------------------------------- ");									
	}

	private void ApplyAnonymisation(CustomerOrder customerOrder) {
		CustomerOrderBilling billing = customerOrder.getBilling();
		CustomerAddress address = billing.getAddress();
		address.setAddressFirstLine(currentCustomer.getAdressStreetLine1());
		address.setAddressSecondLine(currentCustomer.getAdressStreetLine2());
		address.setPostalCode(currentCustomer.getAdressPostalCode());
		address.setCity(currentCustomer.getAdressCity());
		address.setState(null);
				
		CustomerOrderCustomer customer = customerOrder.getCustomer();
		customer.setSalutation(currentCustomer.getSalutation());
		customer.setFirstName(currentCustomer.getPrenom());
		customer.setLastName(currentCustomer.getNom());
		customer.setCompanyName(currentCustomer.getRaisonSociale());
		
		CustomerOrderContact contact = customer.getContact();
		contact.setEmail(currentCustomer.getEmail());
		contact.setPreferedPhoneNumber(currentCustomer.getTelephone1());
		contact.setSecondPhoneNumber(currentCustomer.getTelephone2());
		
		CustomerOrderDelivery delivery = customerOrder.getDelivery();
		delivery.setReceiverSalutation(currentCustomer.getSalutation());
		delivery.setReceiverFirstName(currentCustomer.getPrenom());
		delivery.setReceiverLastName(currentCustomer.getPrenom());
		delivery.setReceiverCompanyName(currentCustomer.getRaisonSociale());
		address = delivery.getAddress();
		address.setAddressFirstLine(currentCustomer.getAdressStreetLine1());
		address.setAddressSecondLine(currentCustomer.getAdressStreetLine2());
		address.setPostalCode(currentCustomer.getAdressPostalCode());
		address.setCity(currentCustomer.getAdressCity());
		address.setState(null);
		
		contact = delivery.getContact();
		contact.setEmail(currentCustomer.getEmail());
		contact.setPreferedPhoneNumber(currentCustomer.getTelephone1());
		contact.setSecondPhoneNumber(currentCustomer.getTelephone2());
		
		CustomerOrderSystem system = customerOrder.getSystem();
		system.setAnonymizationDate(LocalDateTime.now());
		system.setAnonymizationReason (AnonymizationReasonEnum.byExpiration);
		
	}
	
	public void AnonymizeCustomerCustomerOrders(ClientEligible client) throws JsonMappingException, JsonProcessingException {
		currentCustomer = client;
		
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

							AnonymizeCustomerOrder (id);

						}
					}
				}
			}
		} else {
			logger.error("Mule Erreur " + response.getStatusCode() + " " + client.getCodeClient());
		}
	}
}
