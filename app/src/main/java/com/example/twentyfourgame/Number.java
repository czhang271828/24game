// Number.java
package com.example.twentyfourgame;

public interface Number {
    Number add(Number other);
    Number subtract(Number other);
    Number multiply(Number other);
    Number divide(Number other);
    boolean isEqualTo(double target);
    String toDisplayString();
}
