package com.mvpiq.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvpiq.dto.RawPoseFrame;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jboss.logging.Logger;

import java.util.List;

@Converter(autoApply = false)
public class RawPoseFrameConverter implements AttributeConverter<List<RawPoseFrame>, String> {
    
    private static final Logger LOGGER = Logger.getLogger(RawPoseFrameConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<RawPoseFrame> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            LOGGER.error("Failed to convert RawPoseFrame list to JSON", e);
            throw new IllegalArgumentException("Failed to convert RawPoseFrame list to JSON", e);
        }
    }

    @Override
    public List<RawPoseFrame> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<RawPoseFrame>>() {});
        } catch (Exception e) {
            LOGGER.error("Failed to convert JSON to RawPoseFrame list: " + dbData, e);
            throw new IllegalArgumentException("Failed to convert JSON to RawPoseFrame list", e);
        }
    }
}
