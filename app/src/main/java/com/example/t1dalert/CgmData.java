package com.example.t1dalert;

public final class CgmData {
    public final int sgv;
    public final String direction;
    public final long timestampMs;

    public CgmData(int sgv, String direction, long timestampMs) {
        this.sgv = sgv;
        this.direction = direction;
        this.timestampMs = timestampMs;
    }
}
