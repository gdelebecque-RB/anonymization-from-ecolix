package com.rochebobois.omnicanal.tool.anonymization.tools;

import com.rochebobois.omnicanal.domain.RBDomainObject;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCase;
import com.rochebobois.omnicanal.sfsc.domain.SFSCCustomerOrder;
import com.rochebobois.omnicanal.sfsc.domain.SFSCOpportunity;
import com.rochebobois.omnicanal.sfsc.domain.SFSCQuote;
import com.rochebobois.omnicanal.sfsc.domain.SFSCSupplierOrder;
import com.rochebobois.omnicanal.tool.anonymization.domain.sfsc.SFItemObject;

public class CompositeItem {

	String request = null;
	String objectName = null;
	String reference = null;
	SFItemObject crObject = null;
	RBDomainObject rbDomainObject = null;
	
	public CompositeItem(String object, String reference , RBDomainObject rbDomainObject, String request) {
		setReference(reference);
		setRequest(request);
		setObjectName(object);
		setRbDomainObject(rbDomainObject);
	}
	public CompositeItem(String object, String reference , String request) {
		setReference(reference);
		setRequest(request);
		setObjectName(object);
	}
	public CompositeItem(String object, String reference , SFItemObject crObject , String request) {
		setReference(reference);
		setRequest(request);
		setObjectName(object);
		setCrObject(crObject);
		setRbDomainObject(rbDomainObject);
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String object) {
		this.objectName = object;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public SFItemObject getSFObject() {
		return crObject;
	}

	public void setCrObject(SFItemObject crObject) {
		this.crObject = crObject;
	}

	public void setRbDomainObject(RBDomainObject rbDomainObject) {
		this.rbDomainObject = rbDomainObject;
	}
	
	public RBDomainObject getRbDomainObject() {
		return rbDomainObject;
	}
	
	public String getObjectComplementary() {
		if (rbDomainObject==null) return "";
		if (rbDomainObject instanceof SFSCOpportunity) {
			SFSCOpportunity opport = (SFSCOpportunity) rbDomainObject;
			return opport.getProjectNature__c() + "-" + opport.getProjectSource__c();
		}
		if (rbDomainObject instanceof SFSCCustomerOrder) {
			SFSCCustomerOrder customerOrder = (SFSCCustomerOrder) rbDomainObject;
			return customerOrder.getExternalId__c();
		}
		if (rbDomainObject instanceof SFSCSupplierOrder) {
			SFSCSupplierOrder supplierOrder = (SFSCSupplierOrder) rbDomainObject;
			return supplierOrder.getExternalId__c();
		}
		if (rbDomainObject instanceof SFSCCase) {
			SFSCCase sfscCase = (SFSCCase) rbDomainObject;
			return sfscCase.getCaseNumber();
		}
		if (rbDomainObject instanceof SFSCQuote) {
			SFSCQuote quote = (SFSCQuote) rbDomainObject;
			return quote.getExternalId__c();
		}
		return "";
	}
	public String getObjectDescription() {
		if (rbDomainObject==null) return "";
		if (rbDomainObject instanceof SFSCOpportunity) {
			SFSCOpportunity opport = (SFSCOpportunity) rbDomainObject;
			return opport.getId();
		}
		if (rbDomainObject instanceof SFSCCustomerOrder) {
			SFSCCustomerOrder customerOrder = (SFSCCustomerOrder) rbDomainObject;
			return customerOrder.getId();
		}
		if (rbDomainObject instanceof SFSCSupplierOrder) {
			SFSCSupplierOrder supplierOrder = (SFSCSupplierOrder) rbDomainObject;
			return supplierOrder.getId();
		}
		if (rbDomainObject instanceof SFSCCase) {
			SFSCCase sfscCase = (SFSCCase) rbDomainObject;
			return sfscCase.getId();
		}
		if (rbDomainObject instanceof SFSCQuote) {
			SFSCQuote quote = (SFSCQuote) rbDomainObject;
			return quote.getId();
		}
		return null;
	}
	
	
	
}
