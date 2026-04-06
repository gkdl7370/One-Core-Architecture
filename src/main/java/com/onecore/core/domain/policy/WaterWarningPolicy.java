package com.onecore.core.domain.policy;

import com.onecore.core.domain.aggregate.WaterLevel;

/**
 * 수위 경보 정책 인터페이스
 * 지역마다 다른 경보 기준을 Strategy Pattern으로 분리
 * 새 지역 추가 시 이 인터페이스 구현체만 추가 (OCP)
 */
public interface WaterWarningPolicy {
    boolean isDangerous(WaterLevel level);
}