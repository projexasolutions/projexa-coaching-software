package com.projexa.coaching.social.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class SocialContentAiService {
    private final RestClient client = RestClient.create();
    @Value("${app.ai.openai-api-key:}") private String apiKey;
    @Value("${app.ai.openai-model:gpt-4o-mini}") private String model;

    public Map<String,String> generate(String title,String category,String format,String instituteName,String targetAudience,String brandTone,String callToAction) {
        if(apiKey==null||apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI caption generation is not configured. Set OPENAI_API_KEY on the API server.");
        String prompt = """
            Create one original social-media caption for an Indian coaching institute.
            Topic/title: %s
            Content category: %s
            Creative format: %s
            Institute name (if supplied): %s
            Target audience (if supplied): %s
            Brand tone (if supplied): %s
            Preferred call to action (if supplied): %s

            Requirements:
            - Use clear, natural English suitable for Indian students and parents.
            - Keep the caption useful, engaging and concise (80-130 words).
            - Add a gentle call to action, but do not invent phone numbers, fees, offers, results, rankings, faculty credentials or guarantees.
            - Do not claim admission deadlines or seat scarcity unless present in the supplied title.
            - Return only the caption followed by a newline and 5-8 relevant hashtags.
            """.formatted(title,category,format,instituteName==null?"":instituteName,targetAudience==null?"":targetAudience,brandTone==null?"Friendly and professional":brandTone,callToAction==null?"":callToAction);
        try {
            Map response=client.post().uri("https://api.openai.com/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization","Bearer "+apiKey)
                .body(Map.of("model",model,"temperature",0.7,"messages",List.of(
                    Map.of("role","system","content","You are a careful social media copywriter for education businesses. Never fabricate institute facts."),
                    Map.of("role","user","content",prompt))))
                .retrieve().body(Map.class);
            List choices=(List)response.get("choices");
            Map first=(Map)choices.get(0); Map message=(Map)first.get("message");
            String content=String.valueOf(message.get("content")).trim();
            String[] parts=content.split("\\R(?=#)",2);
            String caption=parts[0].trim();
            String hashtags=parts.length>1?parts[1].trim():"#Education #Learning #StudyTips";
            if(caption.isBlank()) throw new IllegalStateException("AI returned an empty caption");
            return Map.of("caption",caption,"hashtags",hashtags);
        } catch(ResponseStatusException e) { throw e; }
        catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"AI caption generation failed. Check the server's AI provider configuration and try again."); }
    }
}
