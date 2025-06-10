package com.mycompany.kviziki;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.poi.xwpf.usermodel.*;

public class KvizImport {

    public static void main(String[] args) throws Exception {
        // Spoji se na bazu (koristi root bez lozinke za XAMPP)
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/kviz", "root", "");

        // Učitaj pitanja iz Word dokumenta
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream("pitanja.docx");
             XWPFDocument document = new XWPFDocument(fis)) {
            for (XWPFParagraph para : document.getParagraphs()) {
                String line = para.getText().trim();
                if (!line.isEmpty()) lines.add(line);
            }
        }

        int questionCounter = 1;
        int answerCounter = 1;

        // Očisti stare podatke da ne puca na duplikatima
        clearExistingData(conn);

        // Prođi sve i unesi u bazu
        for (int i = 0; i < lines.size(); i += 5) {
            String questionText = lines.get(i);
            String correctAnswer = lines.get(i + 1);
            List<String> incorrectAnswers = Arrays.asList(lines.get(i + 2), lines.get(i + 3), lines.get(i + 4));

            insertPitanje(conn, questionCounter, questionText);
            insertOdgovor(conn, answerCounter++, questionCounter, correctAnswer, true);

            for (String netocan : incorrectAnswers) {
                insertOdgovor(conn, answerCounter++, questionCounter, netocan, false);
            }

            questionCounter++;
        }

        conn.close();
        System.out.println("✅ Import završen!");
    }

    public static void clearExistingData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM odgovor");
            stmt.executeUpdate("DELETE FROM pitanje");
        }
    }

    public static void insertPitanje(Connection conn, int sifra, String tekst) throws SQLException {
        String sql = "INSERT INTO pitanje (sifra, tekst) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sifra);
            stmt.setString(2, tekst);
            stmt.executeUpdate();
        }
    }

    public static void insertOdgovor(Connection conn, int sifra, int pitanjeSifra, String tekst, boolean tocan) throws SQLException {
        // ⬇️ USKLAĐENO: koristi `pitanje_sifra`, jer tako se zove stupac u tvojoj bazi
        String sql = "INSERT INTO odgovor (sifra, pitanje_sifra, tekst, tocan) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sifra);
            stmt.setInt(2, pitanjeSifra);
            stmt.setString(3, tekst);
            stmt.setBoolean(4, tocan);
            stmt.executeUpdate();
        }
    }
}
