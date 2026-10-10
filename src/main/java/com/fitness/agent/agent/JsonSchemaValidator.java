package com.fitness.agent.agent;

import com.fitness.agent.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JSON Schema 校验器：强制约束 LLM 的结构化输出，非法时返回错误列表用于修正重试。
 */
public final class JsonSchemaValidator {

    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    private JsonSchemaValidator() {
    }

    /**
     * 校验 JSON 是否符合给定 Schema，返回错误信息列表（空列表表示通过）。
     */
    public static List<String> validate(String json, String schemaJson) {
        JsonNode schemaNode = JsonUtil.readTree(schemaJson);
        JsonNode jsonNode = JsonUtil.readTree(json);
        JsonSchema schema = FACTORY.getSchema(schemaNode);
        Set<ValidationMessage> errors = schema.validate(jsonNode);
        List<String> messages = new ArrayList<>();
        for (ValidationMessage error : errors) {
            if (error != null && error.getMessage() != null) {
                messages.add(error.getMessage());
            }
        }
        return messages;
    }
}
