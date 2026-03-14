package com.mvpiq.dto;

import jakarta.ws.rs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import jakarta.ws.rs.core.MediaType;
import java.io.File;

public class VideoUploadFormDTO {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @FormParam("userId")
    @PartType(MediaType.TEXT_PLAIN)
    public String userId;
}
