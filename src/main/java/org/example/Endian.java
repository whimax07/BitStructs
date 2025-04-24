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

        byte[] leastSignicant(int length);
    }



    public static class LBI implements Ray {
        public static final LBI ZERO = new LBI(BigInteger.ZERO);
        public static final LBI ONE = new LBI(BigInteger.ONE);

        private final BigInteger value;



        private LBI(BigInteger value) {
            this.value = value;
        }

        public LBI(byte[] bytes) {
            this(new BigInteger(flip(bytes)));
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

            return new LBI(littleBytes);
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

        @Override
        public byte[] leastSignicant(int length) {
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
            return new LBI(asByteArray());
        }

        @Override
        public byte[] asByteArray() {
            return value.toByteArray();
        }

        @Override
        public byte[] leastSignicant(int length) {
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
