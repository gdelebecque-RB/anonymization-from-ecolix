package com.rochebobois.omnicanal.tool.anonymization.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {}

    /**
     * Écrit des lignes dans un fichier CSV (UTF-8).
     *
     * @param path      chemin du fichier de sortie
     * @param header    en-têtes (peut être null)
     * @param rows      lignes de données
     * @param delimiter séparateur (',' en standard, ';' pour Excel en français)
     * @param append    true = ajoute à la fin du fichier, false = écrase
     */
    public static void writeCsv(Path path,
                                List<String> header,
                                List<List<String>> rows,
                                char delimiter,
                                boolean append) throws IOException {

        StandardOpenOption[] options = append
                ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                                           StandardOpenOption.WRITE};

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options)) {
            if (header != null && !header.isEmpty()) {
                writer.write(toCsvLine(header, delimiter));
                writer.write("\r\n");
            }
            for (List<String> row : rows) {
                writer.write(toCsvLine(row, delimiter));
                writer.write("\r\n");
            }
        }
    }

    /** Surcharge pratique : séparateur ',' et écrasement du fichier. */
    public static void writeCsv(Path path, List<String> header, List<List<String>> rows) throws IOException {
        writeCsv(path, header, rows, ',', false);
    }

    private static String toCsvLine(List<String> fields, char delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(escape(fields.get(i), delimiter));
        }
        return sb.toString();
    }

    /** Met entre guillemets si le champ contient le séparateur, un guillemet ou un saut de ligne. */
    private static String escape(String value, char delimiter) {
        if (value == null) return "";
        boolean needsQuotes = value.indexOf(delimiter) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuotes) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Exemple d'utilisation
    public static void main(String[] args) throws IOException {
        List<String> header = List.of("id", "nom", "commentaire");
        List<List<String>> rows = List.of(
                List.of("1", "Dupont", "RAS"),
                List.of("2", "Martin", "Dit \"bonjour\", puis part"),
                List.of("3", "Durand", "Ligne 1\nLigne 2")
        );
        writeCsv(Path.of("sortie.csv"), header, rows, ';', false);
        System.out.println("Fichier sortie.csv écrit.");
    }
}
