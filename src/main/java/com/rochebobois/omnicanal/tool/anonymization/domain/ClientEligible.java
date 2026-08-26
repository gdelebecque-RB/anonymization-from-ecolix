package com.rochebobois.omnicanal.tool.anonymization.domain;

public class ClientEligible {

    private String codeClient;
    private String uuid;

    private String nom;
    private String prenom;
    private String email;

    private String telephone1;
    private String telephone2;
    private String telephone3;

    private String telephone1Livraison;
    private String telephone2Livraison;
    private String telephone3Livraison;

    private String listeSociete;


    public String getCodeClient() {
        return codeClient;
    }

    public void setCodeClient(String codeClient) {
        this.codeClient = codeClient;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone1() {
        return telephone1;
    }

    public void setTelephone1(String telephone1) {
        this.telephone1 = telephone1;
    }

    public String getTelephone2() {
        return telephone2;
    }

    public void setTelephone2(String telephone2) {
        this.telephone2 = telephone2;
    }

    public String getTelephone3() {
        return telephone3;
    }

    public void setTelephone3(String telephone3) {
        this.telephone3 = telephone3;
    }

    public String getTelephone1Livraison() {
        return telephone1Livraison;
    }

    public void setTelephone1Livraison(String telephone1Livraison) {
        this.telephone1Livraison = telephone1Livraison;
    }

    public String getTelephone2Livraison() {
        return telephone2Livraison;
    }

    public void setTelephone2Livraison(String telephone2Livraison) {
        this.telephone2Livraison = telephone2Livraison;
    }

    public String getTelephone3Livraison() {
        return telephone3Livraison;
    }

    public void setTelephone3Livraison(String telephone3Livraison) {
        this.telephone3Livraison = telephone3Livraison;
    }

    public String getListeSociete() {
        return listeSociete;
    }

    public void setListeSociete(String listeSociete) {
        this.listeSociete = listeSociete;
    }
}