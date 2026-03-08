package com.mvpiq.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class SupabaseStorageService {

    @ConfigProperty(name = "supabase.bucket.videos")
    String bucket;

    @ConfigProperty(name = "supabase.url")
    String supabaseUrl;

    @ConfigProperty(name = "supabase.service.key")
    String serviceKey;

    public String uploadVideo(File file, String path) {

        try {

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("Content-Type", "video/mp4")
                    .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Upload failed: " + response.body());
            }

            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}