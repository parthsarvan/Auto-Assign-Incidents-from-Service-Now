package com.example.backend.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class ServiceNowReferenceDeserializer extends JsonDeserializer<ServiceNowReference> {
    @Override
    public ServiceNowReference deserialize(JsonParser parser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }

        ServiceNowReference reference = new ServiceNowReference();
        if (node.isTextual()) {
            reference.setValue(node.asText());
            reference.setDisplayValue(node.asText());
            return reference;
        }

        if (node.isObject()) {
            JsonNode valueNode = node.get("value");
            JsonNode displayNode = node.get("display_value");
            if (valueNode != null && valueNode.isTextual()) {
                reference.setValue(valueNode.asText());
            }
            if (displayNode != null && displayNode.isTextual()) {
                reference.setDisplayValue(displayNode.asText());
            }
            return reference;
        }

        return null;
    }
}
