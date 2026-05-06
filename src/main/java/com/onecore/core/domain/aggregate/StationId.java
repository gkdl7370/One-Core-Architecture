package com.onecore.core.domain.aggregate;

import java.util.Objects;

public class StationId {

    private final String code;

    public StationId(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("StationId must not be blank.");
        }
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StationId)) return false;
        return code.equals(((StationId) o).code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
