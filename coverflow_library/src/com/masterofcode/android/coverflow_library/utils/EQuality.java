package com.masterofcode.android.coverflow_library.utils;

public enum EQuality {
    BAD(256),
    GOOD(512),
    BEST(1024);

    private int _value;

    EQuality(int Value) {
        this._value = Value;
    }

    public int getValue() {
        return _value;
    }
}
