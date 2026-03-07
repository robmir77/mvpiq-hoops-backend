package com.mvpiq.service.storage;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@ApplicationScoped
public class SupabaseStorageService {

    private static final String SUPABASE_URL = "https://gnjwgcronnnzxokmuqlw.supabase.co";

    public File downloadVideo(String videoUrl) {

        try {

            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();

            File tempFile = File.createTempFile("analysis-", ".mp4");

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

            }

            return tempFile;

        } catch (Exception e) {
            throw new RuntimeException("Error downloading video", e);
        }
    }

    public void deleteVideo(String videoUrl) {

        try {

            String filePath = videoUrl.split("/object/public/")[1];

            String deleteUrl = SUPABASE_URL + "/storage/v1/object/" + filePath;

            HttpURLConnection connection = (HttpURLConnection) new URL(deleteUrl).openConnection();

            connection.setRequestMethod("DELETE");

            connection.setRequestProperty("apikey", System.getenv("SUPABASE_SERVICE_KEY"));
            connection.setRequestProperty("Authorization", "Bearer " + System.getenv("SUPABASE_SERVICE_KEY"));

            connection.getResponseCode();

        } catch (Exception e) {
            throw new RuntimeException("Error deleting video", e);
        }
    }
}