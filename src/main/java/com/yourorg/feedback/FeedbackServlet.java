package com.library.feedback;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@WebServlet("/submitFeedback")
public class FeedbackServlet extends HttpServlet {

    // ⚠️ REPLACE WITH YOUR ACTUAL SHEET ID
    private static final String SPREADSHEET_ID = "1sqUXsA9tvolzs5s7mhaZAS5L2E__mJC7zZde2mGRpwU";
    // ⚠️ REPLACE WITH YOUR ACTUAL SHEET NAME AND RANGE (e.g., Sheet1!A:E)
    private static final String RANGE = "Sheet1!A:E"; 
    
    // ⚠️ REPLACE WITH THE URL WHERE YOUR REACT APP IS RUNNING (e.g., "http://localhost:3000")
    private static final String REACT_ORIGIN = "http://localhost:5173"; 
    
    // Relative path to the key within the deployed WAR file's WEB-INF folder
    private static final String KEY_FILE_RELATIVE_PATH = "WEB-INF/service-account-key.json";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        
        // --- 1. CORS Setup ---
        response.setHeader("Access-Control-Allow-Origin", REACT_ORIGIN); 
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        // --- 2. Dynamically Resolve Key Path ---
        String keyFilePath = getServletContext().getRealPath(KEY_FILE_RELATIVE_PATH);
        if (keyFilePath == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\", \"message\":\"Service account key file not found.\"}");
            return;
        }

        // --- 3. Extract Data from React Request ---
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String rating = request.getParameter("rating");
        String comments = request.getParameter("comments");
        String timestamp = new Date().toString(); 

        try {
            // --- 4. Google Sheets Service Initialization ---
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(keyFilePath))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/spreadsheets"));

            Sheets sheetsService = new Sheets.Builder(
                new NetHttpTransport(), 
                JacksonFactory.getDefaultInstance(), 
                credential)
                .setApplicationName("Library Feedback System")
                .build();

            // --- 5. Prepare and Append Data (Columns: Name, Email, Rating, Comments, Timestamp) ---
            List<Object> rowData = Arrays.asList(name, email, rating, comments, timestamp);
            List<List<Object>> values = Collections.singletonList(rowData);
            ValueRange body = new ValueRange().setValues(values);

            sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

            // --- 6. Success Response ---
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"success\", \"message\":\"Feedback submitted successfully.\"}");

        } catch (Exception e) {
            // --- 7. Error Response ---
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\", \"message\":\"Failed to submit feedback: " + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }
    
    // Handle CORS pre-flight OPTIONS requests
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", REACT_ORIGIN); 
        resp.setHeader("Access-Control-Allow-Methods", "POST");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}