package com.rochebobois.omnicanal.tool.anonymization.report;

import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;

public class Report {
	
	private ClientEligible client = null;
	private String application = null;
	private String type = null;
	private String code = null;
	private String uuid = null;
	private String error = null;
	private ReportGravity gravity = null;
	
	public Report(ClientEligible client, String application, String type, String code, String uuid, ReportGravity gravity, String error) {
		setClient(client);
		setType(type);
		setCode(code);
		setUuid(uuid);
		setGravity(gravity);
		setError(error);
		setApplication(application);
	}

	public ClientEligible getClient() {
		return client;
	}

	public void setClient(ClientEligible client) {
		this.client = client;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public ReportGravity getGravity() {
		return gravity;
	}

	public void setGravity(ReportGravity gravity) {
		this.gravity = gravity;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

}
