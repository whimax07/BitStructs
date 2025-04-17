package org.example;

public class Utils {

    public static byte[] bs(int... ints) {
        final byte[] bytes = new byte[ints.length];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) ints[i];
        return bytes;
    }

}
