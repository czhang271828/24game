// Fraction.java (修改后)
package com.example.twentyfourgame;

public class Fraction implements Number {
    // 您已有的 num 和 den 字段
    public final long num;
    public final long den;

    public Fraction(long num, long den) {
        // 您已有的构造函数逻辑
        if (den == 0) {
            throw new IllegalArgumentException("Denominator cannot be zero");
        }
        long common = gcd(num, den);
        this.num = num / common;
        this.den = den / common;
    }

    public double toDouble() {
        return (double) num / den;
    }

    @Override
    public Number add(Number other) {
        if (other instanceof ComplexNumber) {
            return new ComplexNumber(this.toDouble(), 0).add(other);
        }
        Fraction f = (Fraction) other;
        return new Fraction(this.num * f.den + f.num * this.den, this.den * f.den);
    }

    @Override
    public Number subtract(Number other) {
        if (other instanceof ComplexNumber) {
            return new ComplexNumber(this.toDouble(), 0).subtract(other);
        }
        Fraction f = (Fraction) other;
        return new Fraction(this.num * f.den - f.num * this.den, this.den * f.den);
    }

    @Override
    public Number multiply(Number other) {
        if (other instanceof ComplexNumber) {
            return new ComplexNumber(this.toDouble(), 0).multiply(other);
        }
        Fraction f = (Fraction) other;
        return new Fraction(this.num * f.num, this.den * f.den);
    }

    @Override
    public Number divide(Number other) {
        if (other instanceof ComplexNumber) {
            return new ComplexNumber(this.toDouble(), 0).divide(other);
        }
        Fraction f = (Fraction) other;
        if (f.num == 0) {
            // 返回一个特殊标记或抛出异常，这里我们返回null让solver忽略此路径
            return null;
        }
        return new Fraction(this.num * f.den, this.den * f.num);
    }

    @Override
    public boolean isEqualTo(double target) {
        return Math.abs(this.toDouble() - target) < 1e-9;
    }

    // 您已有的 isValue(24) 可以改为 isEqualTo(24)
    public boolean isValue(int target) {
        return this.num == target * this.den;
    }

    @Override
    public String toDisplayString() {
        if (den == 1) return String.valueOf(num);
        return String.format("(%d/%d)", num, den);
    }

    // 确保您有 gcd 辅助函数
    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    // 您原有的 toString() 可能需要调整
    @Override
    public String toString() {
        return toDisplayString();
    }
}
