package com.rochebobois.omnicanal.tool.anonymization.domain;

public class ClientPartiel extends ClientEligible{

    private String codeClient;

    private String uuidCc;
    private String noCommandeClient;

    private String uuidDevis;
    private String noDevis;

    private String societe;

    public String getCodeClient() {
        return codeClient;
    }

    public void setCodeClient(String codeClient) {
        this.codeClient = codeClient;
    }

    public String getUuidCc() {
        return uuidCc;
    }

    public void setUuidCc(String uuidCc) {
        this.uuidCc = uuidCc;
    }

    public String getNoCommandeClient() {
        return noCommandeClient;
    }

    public void setNoCommandeClient(String noCommandeClient) {
        this.noCommandeClient = noCommandeClient;
    }

    public String getUuidDevis() {
        return uuidDevis;
    }

    public void setUuidDevis(String uuidDevis) {
        this.uuidDevis = uuidDevis;
    }

    public String getNoDevis() {
        return noDevis;
    }

    public void setNoDevis(String noDevis) {
        this.noDevis = noDevis;
    }

    public String getSociete() {
        return societe;
    }

    public void setSociete(String societe) {
        this.societe = societe;
    }
}