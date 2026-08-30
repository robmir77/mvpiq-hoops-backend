package com.mvpiq.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jboss.logging.Logger;

@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<String, String> {
    
    private static final Logger LOGGER = Logger.getLogger(JsonbConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        // Validate that it's valid JSON before storing
        try {
            objectMapper.readTree(attribute);
            return attribute;
        } catch (Exception e) {
            LOGGER.error("Invalid JSON string: " + attribute, e);
            throw new IllegalArgumentException("Invalid JSON string", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Return the JSON string as-is from the database
        return dbData;
    }
}
