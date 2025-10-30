package com.yourorg.feedback;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class FeedbackServlet extends HttpServlet {
    private static final String SPREADSHEET_ID = "1sqUXsA9tvolzs5s7mhaZAS5L2E__mJC7zZde2mGRpwU";
    private static final String RANGE = "Sheet1!A:E";

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String rating = request.getParameter("rating");
        String comments = request.getParameter("comments");
        String timestamp = new java.util.Date().toString();

        List<Object> rowData = Arrays.asList(name, email, rating, comments, timestamp);
        List<List<Object>> values = Collections.singletonList(rowData);
        ValueRange body = new ValueRange().setValues(values);

        try (InputStream keyStream = getClass().getClassLoader().getResourceAsStream("service-account-key.json")) {
            GoogleCredential credential = GoogleCredential.fromStream(keyStream)
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/spreadsheets"));

            Sheets sheetsService = new Sheets.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Library Feedback Form").build();

            sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"success\", \"message\":\"Feedback submitted successfully.\"}");
            out.flush();
            out.close();
        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"error\", \"message\":\"Failed to submit feedback: " + e.getMessage() + "\"}");
            out.flush();
            out.close();
            e.printStackTrace();
        }
    }
}