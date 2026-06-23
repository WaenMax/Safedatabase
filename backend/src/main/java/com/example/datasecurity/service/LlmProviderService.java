package com.example.datasecurity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LlmProviderService {
    @Value("${agent.llm.deepseek.api-key:}")
    private String deepseekKey;
    @Value("${agent.llm.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;
    @Value("${agent.llm.deepseek.model:deepseek-v4-flash}")
    private String deepseekModel;
    @Value("${agent.llm.siliconflow.api-key:}")
    private String siliconflowKey;
    @Value("${agent.llm.siliconflow.base-url:https://api.siliconflow.cn/v1}")
    private String siliconflowBaseUrl;
    @Value("${agent.llm.siliconflow.model:deepseek-ai/DeepSeek-V3.2}")
    private String siliconflowModel;

    private final RestTemplate restTemplate = restTemplate();

    public boolean supports(String provider) {
        return "deepseek".equalsIgnoreCase(provider) || "siliconflow".equalsIgnoreCase(provider);
    }

    public String chat(String provider, String question, String context, String overrideModel) {
        ProviderConfig cfg = config(provider, overrideModel);
        if (cfg.apiKey == null || cfg.apiKey.isBlank()) {
            throw new IllegalStateException("未配置 " + provider + " API Key，请设置环境变量。");
        }
        String prompt = """
                你是数据分类分级保护系统内置的数据安全治理 Agent。
                只能围绕数据资产、分类分级、脱敏、访问审批、审计日志和风险整改回答。
                回答必须给出可解释理由和可执行建议，避免编造数据库中不存在的事实。

                系统上下文:
                %s

                用户问题:
                %s
                """.formatted(context, question);

        if (isLangChain4jAvailable()) {
            try {
                return invokeLangChain4j(cfg, prompt);
            } catch (Exception e) {
                // 保证演示系统可用：LangChain4j 失败时自动退回兼容 HTTP 调用。
            }
        }
        return invokeOpenAiCompatibleHttp(cfg, prompt);
    }

    private boolean isLangChain4jAvailable() {
        try {
            Class.forName("dev.langchain4j.model.openai.OpenAiChatModel");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private String invokeLangChain4j(ProviderConfig cfg, String prompt) throws Exception {
        Class<?> modelClass = Class.forName("dev.langchain4j.model.openai.OpenAiChatModel");
        Object builder = modelClass.getMethod("builder").invoke(null);
        callIfExists(builder, "apiKey", String.class, cfg.apiKey);
        callIfExists(builder, "baseUrl", String.class, cfg.baseUrl.replaceAll("/$", ""));
        callIfExists(builder, "modelName", String.class, cfg.model);
        callIfExists(builder, "temperature", Double.class, 0.2d);
        callIfExists(builder, "timeout", Duration.class, Duration.ofSeconds(45));
        Object model = builder.getClass().getMethod("build").invoke(builder);
        Method generate = findMethod(model.getClass(), "generate", String.class);
        if (generate != null) return extractText(generate.invoke(model, prompt));
        Method chat = findMethod(model.getClass(), "chat", String.class);
        if (chat != null) return extractText(chat.invoke(model, prompt));
        throw new IllegalStateException("当前 LangChain4j ChatModel 未找到 generate/chat(String) 方法。");
    }

    private String invokeOpenAiCompatibleHttp(ProviderConfig cfg, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.apiKey);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.model);
        body.put("temperature", 0.2);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        ResponseEntity<Map> response = restTemplate.exchange(
                cfg.baseUrl.replaceAll("/$", "") + "/chat/completions",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        Object choices = response.getBody() == null ? null : response.getBody().get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object message = first.get("message");
            if (message instanceof Map<?, ?> msg && msg.get("content") != null) {
                return String.valueOf(msg.get("content"));
            }
        }
        return "模型已返回响应，但未解析到 content 字段。";
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(45000);
        return new RestTemplate(factory);
    }

    private String extractText(Object response) throws Exception {
        if (response == null) return "";
        if (response instanceof String text) return text;
        Object content = invokeNoArg(response, "content");
        if (content != null) {
            Object text = invokeNoArg(content, "text");
            if (text != null) return String.valueOf(text);
            return String.valueOf(content);
        }
        Object text = invokeNoArg(response, "text");
        if (text != null) return String.valueOf(text);
        return String.valueOf(response);
    }

    private Object invokeNoArg(Object target, String name) throws Exception {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private void callIfExists(Object target, String method, Class<?> type, Object value) throws Exception {
        Method m = findMethod(target.getClass(), method, type);
        if (m != null) m.invoke(target, value);
    }

    private Method findMethod(Class<?> type, String name, Class<?> parameterType) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && wrap(method.getParameterTypes()[0]).isAssignableFrom(wrap(parameterType))) {
                return method;
            }
        }
        return null;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == double.class) return Double.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        return type;
    }

    private ProviderConfig config(String provider, String overrideModel) {
        if ("siliconflow".equalsIgnoreCase(provider)) {
            return new ProviderConfig(siliconflowKey, siliconflowBaseUrl, blankOr(overrideModel, siliconflowModel));
        }
        return new ProviderConfig(deepseekKey, deepseekBaseUrl, blankOr(overrideModel, deepseekModel));
    }

    private String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ProviderConfig(String apiKey, String baseUrl, String model) {}
}
