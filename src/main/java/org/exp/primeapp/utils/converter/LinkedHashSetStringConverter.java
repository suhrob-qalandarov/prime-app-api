package org.exp.primeapp.utils.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class LinkedHashSetStringConverter implements AttributeConverter<LinkedHashSet<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(LinkedHashSet<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error converting LinkedHashSet to JSON", e);
        }
    }

    @Override
    public LinkedHashSet<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        try {
            Set<String> set = objectMapper.readValue(dbData, new TypeReference<Set<String>>() {});
            return new LinkedHashSet<>(set);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to LinkedHashSet", e);
        }
    }
}

