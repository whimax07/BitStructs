package org.example;

import java.math.BigInteger;
import java.util.Arrays;

public class Endian {

    public interface Ray {
        Ray leftShift(int by);

        Ray rightShift(int by);

        Ray and(Ray other);

        Ray and(Ray other, int resultSizeBytes);

        Ray or(Ray other);

        Ray or(Ray other, int resultSizeBytes);

        Ray swapView();

        byte[] asByteArray();
    }



    public static class LBI implements Ray {
        private final BigInteger value;

        private LBI(BigInteger value) {
            this.value = value;
        }

        public static LBI ONE() {
            return new LBI(BigInteger.ONE);
        }

        private static LBI fromBytes(byte[] bytes) {
            final byte[] flipped = flip(bytes);
            return new LBI(new BigInteger(flipped));
        }

        @Override
        public Ray leftShift(int by) {
            return new LBI(value.shiftLeft(by));
        }

        @Override
        public Ray rightShift(int by) {
            return new LBI(value.shiftRight(by));
        }

        @Override
        public Ray and(Ray other) {
            return new LBI(value.and(((LBI) other).value));
        }

        @Override
        public Ray and(Ray other, int resultSizeBytes) {
            // TODO(Max): Truncate output if to long.
            if (other instanceof LBI) return and(other);

            final byte[] littleBytes = Arrays.copyOf(asByteArray(), resultSizeBytes);

            final byte[] bigBytes = other.asByteArray();
            for (int i = 0; i < Math.max(bigBytes.length, resultSizeBytes); i++) {
                littleBytes[littleBytes.length - 1 - i] |= bigBytes[i];
            }

            return LBI.fromBytes(littleBytes);
        }

        @Override
        public Ray or(Ray other) {
            return new LBI(value.or(((LBI) other).value));
        }

        @Override
        public Ray or(Ray other, int resultSizeBytes) {
            // TODO(Max): Truncate output if to long.
            if (other instanceof LBI) return or(other);

            return null;
        }

        @Override
        public Ray swapView() {
            return BBI.fromBytes(asByteArray());
        }

        @Override
        public byte[] asByteArray() {
            return flip(value.toByteArray());
        }
    }



    public static class BBI implements Ray {
        private final BigInteger value;

        private BBI(BigInteger value) {
            this.value = value;
        }

        public static BBI ONE() {
            return new BBI(BigInteger.ONE);
        }

        private static BBI fromBytes(byte[] bytes) {
            return new BBI(new BigInteger(1, bytes));
        }

        @Override
        public Ray leftShift(int by) {
            return new BBI(value.shiftLeft(by));
        }

        @Override
        public Ray rightShift(int by) {
            return new BBI(value.shiftRight(by));
        }

        @Override
        public Ray and(Ray other) {
            return new BBI(value.and(((BBI) other).value));
        }

        @Override
        public Ray and(Ray other, int resultSizeBytes) {
            // TODO(Max): Truncate output if to long.
            if (other instanceof BBI) return and(other);

            return other.and(this, resultSizeBytes).swapView();
        }

        @Override
        public Ray or(Ray other) {
            return new BBI(value.or(((BBI) other).value));
        }

        @Override
        public Ray or(Ray other, int resultSizeBytes) {
            // TODO(Max): Truncate output if to long.
            if (other instanceof BBI) return or(other);

            return other.or(this, resultSizeBytes).swapView();
        }

        @Override
        public Ray swapView() {
            return LBI.fromBytes(asByteArray());
        }

        @Override
        public byte[] asByteArray() {
            return value.toByteArray();
        }
    }



    private static byte[] flip(byte[] in) {
        final byte[] result = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            result[i] = in[in.length - 1 - i];
        }
        return result;
    }

}
