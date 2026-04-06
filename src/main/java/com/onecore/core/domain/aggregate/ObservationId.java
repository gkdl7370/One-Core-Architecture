package com.onecore.core.domain.aggregate;

import java.util.Objects;

public class ObservationId {

    private final String value;

    public ObservationId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ObservationId는 비어있을 수 없습니다.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObservationId)) return false;
        return value.equals(((ObservationId) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}