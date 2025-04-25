package org.example;

import java.math.BigInteger;
import java.util.Arrays;

public class Endian {

    public interface Ray {
        Ray not();

        Ray leftShift(int by);

        Ray rightShift(int by);

        Ray and(Ray other, int resultSizeBytes);

        Ray or(Ray other, int resultSizeBytes);

        byte[] asByteArray();

        byte[] leastSignificant(int length);
    }



    public static class LBI implements Ray {
        public static final LBI ZERO = new LBI(BigInteger.ZERO);
        public static final LBI ONE = new LBI(BigInteger.ONE);

        private final BigInteger value;



        private LBI(BigInteger value) {
            this.value = value;
        }

        public LBI(byte[] bytes) {
            this(new BigInteger(bytes));
        }

        public LBI(long val) {
            this(BigInteger.valueOf(val));
        }



        public LBI and(LBI other) {
            return new LBI(value.and(other.value));
        }

        public LBI or(LBI other) {
            return new LBI(value.or(other.value));
        }

        public LBI add(LBI other) {
            return new LBI(value.add(other.value));
        }

        public LBI subtract(LBI other) {
            return new LBI(value.subtract(other.value));
        }

        public BBI swapView() {
            return new BBI(asByteArray());
        }

        @Override
        public LBI not() {
            return new LBI(value.not());
        }

        @Override
        public LBI leftShift(int by) {
            return new LBI(value.shiftLeft(by));
        }

        @Override
        public LBI rightShift(int by) {
            return new LBI(value.shiftRight(by));
        }

        @Override
        public Ray and(Ray other, int resultSizeBytes) {
            if (other instanceof LBI otherLbi) {
                final LBI mask = ONE.leftShift(resultSizeBytes * 8).subtract(ONE);
                return and(otherLbi).and(mask);
            }

            final byte[] littleBytes = Arrays.copyOf(asByteArray(), resultSizeBytes);

            final byte[] bigBytes = other.asByteArray();
            for (int i = 0; i < Math.min(bigBytes.length, resultSizeBytes); i++) {
                littleBytes[littleBytes.length - 1 - i] &= bigBytes[i];
            }

            return new LBI(littleBytes);
        }

        @Override
        public Ray or(Ray other, int resultSizeBytes) {
            if (other instanceof LBI otherLbi) {
                final LBI mask = ONE.leftShift(resultSizeBytes * 8).subtract(ONE);
                return or(otherLbi).and(mask);
            }

            final byte[] littleBytes = Arrays.copyOf(asByteArray(), resultSizeBytes);

            final byte[] bigBytes = other.asByteArray();
            for (int i = 0; i < Math.min(bigBytes.length, resultSizeBytes); i++) {
                littleBytes[littleBytes.length - 1 - i] |= bigBytes[i];
            }

            return new LBI(littleBytes);
        }

        @Override
        public byte[] asByteArray() {
            return flip(value.toByteArray());
        }

        @Override
        public byte[] leastSignificant(int length) {
            final byte[] bytes = asByteArray();
            return Arrays.copyOf(bytes, length);
        }
    }



    public static class BBI implements Ray {
        public static final BBI ZERO = new BBI(BigInteger.ZERO);
        public static final BBI ONE = new BBI(BigInteger.ONE);

        private final BigInteger value;



        private BBI(BigInteger value) {
            this.value = value;
        }

        public BBI(byte[] bytes) {
            this(new BigInteger(bytes));
        }

        public BBI(long val) {
            this(BigInteger.valueOf(val));
        }



        public BBI and(BBI other) {
            return new BBI(value.and(other.value));
        }

        public BBI or(BBI other) {
            return new BBI(value.or(other.value));
        }

        public BBI add(BBI other) {
            return new BBI(value.add(other.value));
        }

        public BBI subtract(BBI other) {
            return new BBI(value.subtract(other.value));
        }

        public LBI swapView() {
            return new LBI(asByteArray());
        }

        @Override
        public BBI not() {
            return new BBI(value.not());
        }

        @Override
        public BBI leftShift(int by) {
            return new BBI(value.shiftLeft(by));
        }

        @Override
        public BBI rightShift(int by) {
            return new BBI(value.shiftRight(by));
        }

        @Override
        public Ray and(Ray other, int resultSizeBytes) {
            if (other instanceof BBI otherBbi) {
                final BBI mask = ONE.leftShift(resultSizeBytes * 8).subtract(ONE);
                return and(otherBbi).and(mask);
            }

            final Ray anded = other.and(this, resultSizeBytes);
            if (anded instanceof LBI andedLbi) return andedLbi.swapView();

            throw new IllegalStateException("other should only be either an LBI or a BBI.");
        }

        @Override
        public Ray or(Ray other, int resultSizeBytes) {
            if (other instanceof BBI otherBbi) {
                final BBI mask = ONE.leftShift(resultSizeBytes * 8).subtract(ONE);
                return or(otherBbi).and(mask);
            }

            final Ray ored = other.or(this, resultSizeBytes);
            if (ored instanceof LBI oredLbi) return oredLbi.swapView();

            throw new IllegalStateException("other should only be either an LBI or a BBI.");
        }

        @Override
        public byte[] asByteArray() {
            return value.toByteArray();
        }

        @Override
        public byte[] leastSignificant(int length) {
            final byte[] bytes = asByteArray();

            if (bytes.length == length) return bytes;
            if (bytes.length > length) return Arrays.copyOfRange(bytes, bytes.length - length, bytes.length);

            final byte[] out = new byte[length];
            System.arraycopy(bytes, 0, out, length - bytes.length, bytes.length);
            return out;
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
