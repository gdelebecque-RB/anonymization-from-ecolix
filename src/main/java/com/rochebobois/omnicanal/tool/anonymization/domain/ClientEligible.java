package com.rochebobois.omnicanal.tool.anonymization.domain;

import java.util.Random;

import com.rochebobois.omnicanal.domain.SalutationEnum;
import com.rochebobois.omnicanal.tool.anonymization.tools.StringAnonymizer;

public class ClientEligible {

	Random rdm = new Random();
	
    private String codeClient;
    private String uuid;

    private SalutationEnum salutation = SalutationEnum.Mx;
    private String nom;
    private String formerNom;
    private String prenom;
    private String formerPrenom;
    private String email;
    private String formerEmail;

    private String raisonSociale;
    
    private String telephone1;
    private String formerTelephone1;
    private String telephone2;
    private String formerTelephone2;
    private String telephone3;

    private String telephone1Livraison;
    private String FormerTelephone1Livraison;
    private String telephone2Livraison;
    private String FormerTelephone2Livraison;
    private String telephone3Livraison;

    private String listeSociete;

    private String AdressStreetLine1 = null;
    private String AdressStreetLine2 = null;
    private String AdressCity = null;
    private String AdressPostalCode = null;    

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
    	setFormerNom(nom);
    	this.nom = StringAnonymizer.anonymize(nom,nom.length());
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
    	setFormerPrenom(prenom);
        this.prenom = StringAnonymizer.anonymize(prenom, prenom.length());
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
    	setFormerEmail(email);
        this.email = StringAnonymizer.anonymizeEmail(email);
    }

    public String getTelephone1() {
        return telephone1;
    }

    public void setTelephone1(String telephone1) {
    	setFormerTelephone1(telephone1);
        this.telephone1 = StringAnonymizer.anonymize(telephone1, telephone1.length());
    }

    public String getTelephone2() {
        return telephone2;
    }

    public void setTelephone2(String telephone2) {
    	setFormerTelephone2(telephone2);
        this.telephone2 = StringAnonymizer.anonymize(telephone2, telephone2.length());
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
    	setFormerTelephone1Livraison(telephone1Livraison);
        this.telephone1Livraison = StringAnonymizer.anonymize(telephone1Livraison, telephone1Livraison.length());
    }

    public String getTelephone2Livraison() {
        return telephone2Livraison;
    }

    public void setTelephone2Livraison(String telephone2Livraison) {
    	setFormerTelephone2Livraison(telephone2Livraison);
        this.telephone2Livraison = StringAnonymizer.anonymize(telephone2Livraison, telephone2Livraison.length());
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

	public String getFormerNom() {
		return formerNom;
	}

	public void setFormerNom(String formerNom) {
		this.formerNom = formerNom;
	}

	public String getFormerPrenom() {
		return formerPrenom;
	}

	public void setFormerPrenom(String formerPrenom) {
		this.formerPrenom = formerPrenom;
	}

	public String getFormerTelephone1() {
		return formerTelephone1;
	}

	public void setFormerTelephone1(String formerTelephone1) {
		this.formerTelephone1 = formerTelephone1;
	}

	public String getFormerTelephone2() {
		return formerTelephone2;
	}

	public void setFormerTelephone2(String formerTelephone2) {
		this.formerTelephone2 = formerTelephone2;
	}

	public String getFormerTelephone1Livraison() {
		return FormerTelephone1Livraison;
	}

	public void setFormerTelephone1Livraison(String formerTelephone1Livraison) {
		FormerTelephone1Livraison = formerTelephone1Livraison;
	}

	public String getFormerTelephone2Livraison() {
		return FormerTelephone2Livraison;
	}

	public void setFormerTelephone2Livraison(String formerTelephone2Livraison) {
		FormerTelephone2Livraison = formerTelephone2Livraison;
	}

	public String getFormerEmail() {
		return formerEmail;
	}

	public void setFormerEmail(String formerEmail) {
		this.formerEmail = formerEmail;
	}

	public SalutationEnum getSalutation() {
		return salutation;
	}

	public String getAdressStreetLine1() {
		if (AdressStreetLine1==null) {
			AdressStreetLine1 = StringAnonymizer.anonymize( getNom() + getPrenom());
		}
		return AdressStreetLine1;
	}

	public String getAdressStreetLine2() {
		if (AdressStreetLine2==null) {
			AdressStreetLine2 = StringAnonymizer.anonymize( getPrenom() + getNom());
		}
		return AdressStreetLine2;
	}

	public String getAdressCity() {
		if (AdressCity==null) {
			AdressCity = StringAnonymizer.anonymize( getNom(), getNom().length());
		}		
		return AdressCity;
	}

	public String getAdressPostalCode() {
		if (AdressPostalCode==null) {
			int length = 4 + rdm.nextInt(2);
			AdressPostalCode = StringAnonymizer.anonymize( getPrenom(), length);
		}		

		return AdressPostalCode;
	}

	public String getRaisonSociale() {
		raisonSociale = getNom() + getPrenom();
		return raisonSociale;
	}
    
}