package com.rochebobois.omnicanal.tool.anonymization.tools;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class StringAnonymizer {

	private static final String ALGORITHM = "HmacSHA256";

	// Secret généré aléatoirement à chaque lancement de l'application
	private static final byte[] SECRET = generateSecret();

	/**
	 * Anonymise une chaîne avec HMAC-SHA-256. Le résultat est limité à 20
	 * caractères.
	 */
	public static String anonymize(String input) {
		return anonymize(input, 20);
	}

	/**
	 * Anonymise une chaîne avec HMAC-SHA-256.
	 *
	 * @param input  chaîne à anonymiser
	 * @param length longueur du résultat souhaitée (1 à 64)
	 * @return chaîne anonymisée
	 */
	public static String anonymize(String input, int length) {

		if (input == null) {
			return null;
		}

		if (length < 1 || length > 64) {
			throw new IllegalArgumentException("La longueur doit être comprise entre 1 et 64");
		}

		try {
			Mac mac = Mac.getInstance(ALGORITHM);

			SecretKeySpec secretKey = new SecretKeySpec(SECRET, ALGORITHM);

			mac.init(secretKey);

			byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));

			StringBuilder result = new StringBuilder();

			for (byte b : hash) {
				result.append(String.format("%02x", b));
			}

			return result.substring(0, length);

		} catch (Exception e) {
			throw new RuntimeException("Erreur lors de l'anonymisation", e);
		}
	}

	/**
	 * Génère un secret aléatoire de 256 bits.
	 */
	private static byte[] generateSecret() {
		byte[] secret = new byte[32];

		SecureRandom random = new SecureRandom();
		random.nextBytes(secret);

		return secret;
	}

	/**
	 * Exemple d'utilisation.
	 */
	public static void main(String[] args) {

		System.out.println("Jean Dupont : " + anonymize("Jean Dupont"));

		System.out.println("Jean Dupont : " + anonymize("Jean Dupont"));

		System.out.println("Pierre Martin : " + anonymize("Pierre Martin"));

		System.out.println("12 rue de Paris : " + anonymize("12 rue de Paris"));

		System.out.println("Pierre.dupont@acme.com : " + anonymizeEmail("Pierre.dupont@acme.com"));
	}

	public static String anonymizeEmail(String email) {
		if (email == null)
			return null;
		int index = email.indexOf("@");
		if (index == -1) {
			return anonymize(email, email.length());
		} else {
			String anoEmail = email.substring(0, index);
			anoEmail = anonymize(anoEmail, anoEmail.length());
			anoEmail = anoEmail + "@" + anonymize(email.substring(index), email.substring(index).length());
			anoEmail = anoEmail + "." + "anonymized";
			return anoEmail;
		}
	}
}
