package com.mvpiq.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;

@ApplicationScoped
public class StorageService {

    @ConfigProperty(name = "supabase.url")
    String supabaseUrl;

    @ConfigProperty(name = "supabase.service.key")
    String serviceKey;

    public String uploadVideo(File file, String path) {

        String uploadUrl =
                supabaseUrl +
                        "/storage/v1/object/videos/" +
                        path;

        // HTTP PUT upload

        return supabaseUrl +
                "/storage/v1/object/public/videos/" +
                path;
    }
}