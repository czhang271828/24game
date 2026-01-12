// ComplexNumber.java
package com.example.twentyfourgame;

public class ComplexNumber implements Number {
    private final double real;
    private final double imag;
    private static final double TOLERANCE = 1e-6;

    public ComplexNumber(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    // 将其他 Number 类型转换为 ComplexNumber
    private ComplexNumber from(Number other) {
        if (other instanceof ComplexNumber) {
            return (ComplexNumber) other;
        } else if (other instanceof Fraction) {
            return new ComplexNumber(((Fraction) other).toDouble(), 0);
        }
        // 可以根据需要添加其他类型
        throw new IllegalArgumentException("Unsupported Number type");
    }

    @Override
    public Number add(Number other) {
        ComplexNumber c = from(other);
        return new ComplexNumber(this.real + c.real, this.imag + c.imag);
    }

    @Override
    public Number subtract(Number other) {
        ComplexNumber c = from(other);
        return new ComplexNumber(this.real - c.real, this.imag - c.imag);
    }

    @Override
    public Number multiply(Number other) {
        ComplexNumber c = from(other);
        double newReal = this.real * c.real - this.imag * c.imag;
        double newImag = this.real * c.imag + this.imag * c.real;
        return new ComplexNumber(newReal, newImag);
    }

    @Override
    public Number divide(Number other) {
        ComplexNumber c = from(other);
        double denominator = c.real * c.real + c.imag * c.imag;
        if (Math.abs(denominator) < TOLERANCE) {
            // 返回一个特殊值或抛出异常来表示除以零
            return new ComplexNumber(Double.NaN, Double.NaN);
        }
        double newReal = (this.real * c.real + this.imag * c.imag) / denominator;
        double newImag = (this.imag * c.real - this.real * c.imag) / denominator;
        return new ComplexNumber(newReal, newImag);
    }

    // 检查是否为 NaN
    public boolean isNaN() {
        return Double.isNaN(this.real) || Double.isNaN(this.imag);
    }

    @Override
    public boolean isEqualTo(double target) {
        // 目标值为24，当且仅当实部为24且虚部为0
        return Math.abs(this.real - target) < TOLERANCE && Math.abs(this.imag) < TOLERANCE;
    }

    @Override
    public String toDisplayString() {
        if (Math.abs(imag) < TOLERANCE) {
            return String.valueOf(real);
        }
        if (imag > 0) {
            return String.format("(%s+%si)", real, imag);
        }
        return String.format("(%s%si)", real, imag);
    }
}
