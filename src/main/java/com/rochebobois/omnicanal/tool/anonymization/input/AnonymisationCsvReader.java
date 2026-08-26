package com.rochebobois.omnicanal.tool.anonymization.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import com.rochebobois.omnicanal.tool.anonymization.domain.ClientEligible;
import com.rochebobois.omnicanal.tool.anonymization.domain.ClientPartiel;

public class AnonymisationCsvReader {

    private static final Charset CSV_CHARSET = Charset.forName("windows-1252");

    /**
     * Lecture du fichier :
     * ANONYMISATION_CLIENT_ELIGIBLE_xxx.csv
     *
     * Clé de la Map : code_client
     */
    public static List<ClientEligible> readClientsEligibles(Path file) throws IOException {

        List<ClientEligible> result = new LinkedList<ClientEligible>();

        try (BufferedReader reader = Files.newBufferedReader(file, CSV_CHARSET)) {

            // En-tête
            String line = reader.readLine();

            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {

                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                String[] values = line.split(";", -1);

                /*
                 * Format normal :
                 *
                 * 0  code_client
                 * 1  uuid
                 * 2  nom
                 * 3  prenom
                 * 4  email
                 * 5  telephone1
                 * 6  telephone2
                 * 7  telephone3
                 * 8  telephone1_livraison
                 * 9  telephone2_livraison
                 * 10 telephone3_livraison
                 * 11 liste_societe
                 */
                if (values.length < 12) {
                    System.err.printf(
                            "Ligne %d ignorée : %d colonnes au lieu de 12%n",
                            lineNumber,
                            values.length
                    );
                    continue;
                }

                /*
                 * Quelques lignes du fichier possèdent une colonne
                 * supplémentaire avant l'adresse email.
                 *
                 * On localise donc l'email plutôt que de se baser
                 * uniquement sur un index fixe.
                 */
                int emailIndex = findEmailIndex(values);

                if (emailIndex < 0) {
                    System.err.printf(
                            "Ligne %d ignorée : email introuvable%n",
                            lineNumber
                    );
                    continue;
                }

                String codeClient = value(values, 0);

                if (codeClient.isBlank()) {
                    continue;
                }

                ClientEligible client = new ClientEligible();

                client.setCodeClient(codeClient);
                client.setUuid(value(values, 1));
                client.setNom(value(values, 2));

                /*
                 * Cas standard :
                 * nom=2, prenom=3, email=4
                 *
                 * Dans certaines lignes :
                 * nom=2, valeurs supplémentaires, email=5
                 *
                 * On conserve ici comme prénom l'ensemble situé
                 * entre le nom et l'email.
                 */
                client.setPrenom(join(values, 3, emailIndex));

                client.setEmail(value(values, emailIndex));
                client.setTelephone1(value(values, emailIndex + 1));
                client.setTelephone2(value(values, emailIndex + 2));
                client.setTelephone3(value(values, emailIndex + 3));

                client.setTelephone1Livraison(value(values, emailIndex + 4));
                client.setTelephone2Livraison(value(values, emailIndex + 5));
                client.setTelephone3Livraison(value(values, emailIndex + 6));

                client.setListeSociete(value(values, emailIndex + 7));

                result.add(client);
            }
        }

        return result;
    }


    /**
     * Lecture du fichier :
     * ANONYMISATION_LISTE_PARTIELLE_xxx.csv
     *
     * Clé de la Map : Code_client
     */
    public static List<ClientPartiel> readClientsPartiels(Path file) throws IOException {

        List<ClientPartiel> result = new LinkedList<ClientPartiel>();

        try (BufferedReader reader = Files.newBufferedReader(file, CSV_CHARSET)) {

            // En-tête
            String line = reader.readLine();

            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {

                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                String[] values = line.split(";", -1);

                /*
                 * Format normal :
                 *
                 * 0  UUID
                 * 1  Code_client
                 * 2  Nom
                 * 3  prenom
                 * 4  Mail
                 * 5  Telephone_1
                 * 6  telephone_2
                 * 7  Telephone_3
                 * 8  telephone_1_livraison
                 * 9  Telephone_2_livraison
                 * 10 Telephone_3_livraison
                 * 11 uuid_cc
                 * 12 no_commande_client
                 * 13 uuid_devis
                 * 14 no_devis
                 * 15 société
                 */
                if (values.length < 16) {
                    System.err.printf(
                            "Ligne %d ignorée : %d colonnes au lieu de 16%n",
                            lineNumber,
                            values.length
                    );
                    continue;
                }

                int emailIndex = findEmailIndex(values);

                if (emailIndex < 0) {
                    System.err.printf(
                            "Ligne %d ignorée : email introuvable%n",
                            lineNumber
                    );
                    continue;
                }

                String codeClient = value(values, 1);

                if (codeClient.isBlank()) {
                    continue;
                }

                ClientPartiel client = new ClientPartiel();

                client.setUuid(value(values, 0));
                client.setCodeClient(codeClient);
                client.setNom(value(values, 2));
                client.setPrenom(join(values, 3, emailIndex));

                client.setEmail(value(values, emailIndex));

                client.setTelephone1(value(values, emailIndex + 1));
                client.setTelephone2(value(values, emailIndex + 2));
                client.setTelephone3(value(values, emailIndex + 3));

                client.setTelephone1Livraison(value(values, emailIndex + 4));
                client.setTelephone2Livraison(value(values, emailIndex + 5));
                client.setTelephone3Livraison(value(values, emailIndex + 6));

                client.setUuidCc(value(values, emailIndex + 7));
                client.setNoCommandeClient(value(values, emailIndex + 8));
                client.setUuidDevis(value(values, emailIndex + 9));
                client.setNoDevis(value(values, emailIndex + 10));
                client.setSociete(value(values, emailIndex + 11));

                result.add(client);

            }
        }

        return result;
    }


    private static int findEmailIndex(String[] values) {

        for (int i = 0; i < values.length; i++) {

            String value = values[i];

            if (value != null && value.contains("@")) {
                return i;
            }
        }

        return -1;
    }


    private static String value(String[] values, int index) {

        if (index < 0 || index >= values.length) {
            return "";
        }

        return values[index] == null
                ? ""
                : values[index].trim();
    }


    private static String join(String[] values, int fromInclusive, int toExclusive) {

        if (fromInclusive >= toExclusive) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = fromInclusive; i < toExclusive; i++) {

            String value = value(values, i);

            if (value.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(value);
        }

        return result.toString();
    }
}