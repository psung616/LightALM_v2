package com.lightalm.baseline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 베이스라인 스냅샷 Map &lt;-&gt; JSON 텍스트(baseline_items.snapshot jsonb) &lt;-&gt; {@link JsonNode} 변환.
 * 생성(저장)·상세 조회·diff가 모두 같은 ObjectMapper 설정을 거치도록 한곳에 모은다 — 스냅샷과 현재 값을
 * 같은 경로로 정규화해야 숫자 타입(Long vs Integer) 차이 같은 직렬화 아티팩트가 "변경"으로 잡히지 않는다.
 */
@Component
@RequiredArgsConstructor
public class BaselineSnapshotJsonCodec {

    private final ObjectMapper objectMapper;

    public String write(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("베이스라인 스냅샷 직렬화에 실패했습니다.", e);
        }
    }

    public JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("베이스라인 스냅샷 역직렬화에 실패했습니다.", e);
        }
    }

    /** 현재 값 Map을 저장된 스냅샷과 동일한 경로(JSON 텍스트 → 트리)로 정규화한다. */
    public JsonNode normalize(Map<String, Object> snapshot) {
        return read(write(snapshot));
    }
}
