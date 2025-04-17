package org.example;

import org.example.BitVal.BitOrdering;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public interface BitStruct {

    default byte[] encode() {
        return null;
    }

    default void decodeInitialize(byte[] bytes) {

    }

    static <T extends BitStruct> T decode(Class<T> clazz, byte[] bytes) {
        if (clazz.isEnum()) throw new RuntimeException("Can't populate Enums classes.");

        final ArrayList<Field> bitValFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(not(BitStruct::isStatic))
                .filter(BitStruct::hasBitValAnnotation)
                .collect(Collectors.toCollection(ArrayList::new));
        final List<String> fieldNames = bitValFields.stream().map(Field::getName).toList();


        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        final Constructor<?> constructor = Arrays.stream(constructors)
                .filter(allFieldConstructor(fieldNames))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        "No \"all args\" constructor for BitVals found. " +
                                "Note constructor parameter names and BitVal annotated field names must match."
                ));

        bitValFields.sort(constructorOrdering(constructor));

        final Object[] constructorArgs = constructArgs(bitValFields, bytes);
        constructor.setAccessible(true);

        try {
            //noinspection unchecked
            return (T) constructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isStatic(Field field) {
        return field.accessFlags().contains(AccessFlag.STATIC);
    }

    private static boolean hasBitValAnnotation(Field field) {
        return Arrays.stream(field.getAnnotations()).anyMatch(BitVal.class::isInstance);
    }

    private static Predicate<Constructor<?>> allFieldConstructor(List<String> fieldNames) {
        return constructor -> allFieldConstructor(constructor, fieldNames);
    }

    private static boolean allFieldConstructor(Constructor<?> constructor, List<String> fieldNames) {
        final HashSet<String> constructParamNames = Arrays.stream(constructor.getParameters())
                .map(Parameter::getName)
                .collect(Collectors.toCollection(HashSet::new));

        return constructParamNames.equals(new HashSet<>(fieldNames));
    }

    private static Comparator<? super Field> constructorOrdering(Constructor<?> constructor) {
        final List<String> paramNames = Arrays.stream(constructor.getParameters())
                .map(Parameter::getName)
                .toList();
        return Comparator.comparingInt(field -> paramNames.indexOf(field.getName()));
    }

    private static Object[] constructArgs(List<Field> bitValFields, byte[] bytes) {
        final Object[] constructorArgs = new Object[bitValFields.size()];

        for (int i = 0; i < bitValFields.size(); i++) {
            final Field field = bitValFields.get(i);
            final BitVal bitVal = field.getAnnotation(BitVal.class);
            final Class<?> baseType = getBaseType(field.getType());
            final Object extractedVal = getBitVal(bitVal, bytes, baseType);
            constructorArgs[i] = extractedVal;
        }

        return constructorArgs;
    }



    private static Class<?> getBaseType(Class<?> type) {
        if (type.isPrimitive()) {
            return switch (type.getSimpleName()) {
                case "boolean" -> Boolean.class;
                case "byte" -> Byte.class;
                case "short" -> Short.class;
                case "int" -> Integer.class;
                case "long" -> Long.class;
                default -> throw new IllegalStateException("Unsupported primitive type: " + type.getSimpleName());
            };
        }

        final boolean isGood = BitStruct.class.isAssignableFrom(type)
                || Boolean.class.equals(type)
                || Byte.class.equals(type)
                || Short.class.equals(type)
                || Integer.class.equals(type)
                || Long.class.equals(type);
        if (isGood) return type;

        throw new IllegalStateException("Unsupported type: " + type.getSimpleName());
    }

    private static Object getBitVal(BitVal bitVal, byte[] bytes, Class<?> baseType) {
        if (BitStruct.class.isAssignableFrom(baseType)) {
            final byte[] subRange = getSubRange(bitVal, bytes);
            @SuppressWarnings("unchecked") // Safe by if branch condition.
            final Class<? extends BitStruct> bound = (Class<? extends BitStruct>) baseType;
            return BitStruct.decode(bound, subRange);
        }

        final BigInteger bigInteger = getBigInteger(bitVal, bytes);
        return switch (baseType.getSimpleName()) {
            case "Boolean" -> bigInteger.intValue() != 0;
            case "Byte" -> bigInteger.byteValue();
            case "Short" -> bigInteger.shortValue();
            case "Integer" -> bigInteger.intValue();
            case "Long" -> bigInteger.longValue();
            default -> throw new IllegalStateException(
                    "Unsupported type at extract phase: " + baseType.getSimpleName()
            );
        };
    }

    private static byte[] getSubRange(BitVal bitVal, byte[] bytes) {
        final BigInteger bigInteger = getBigInteger(bitVal, bytes);
        final byte[] packedBytes = bigInteger.toByteArray();

        final boolean littleEndian = bitVal.ordering() == BitOrdering.LITTLE;
        final byte[] outPackedBytes = littleEndian ? flip(packedBytes) : packedBytes;

        final int neededBytes = (bitVal.len() + 7) / 8;
        final byte[] result = new byte[neededBytes];

        /*
        BigInteger#toByteArray will add a sign bit; this means all 3 of the below situations are possible.
               t:     # # #         t: # # # # #       t: # # # # #
               b: # # # # #         b: # # # # #       b:     # # #
         x, y, z:   0, 2, 3              0, 0, 5            2, 0, 3

         x, y, z -> tStart, bStart, copySize

         tStart = max(len(t) - len(b), 0)
         bStart = max(len(b) - len(t), 0)
         copySize = min(len(t), len(b))
         */
        final int packedStart = Math.max(outPackedBytes.length - neededBytes, 0);
        final int resultStart = Math.max(neededBytes - outPackedBytes.length, 0);
        final int bytesToCopy = Math.min(outPackedBytes.length, neededBytes);
        System.arraycopy(outPackedBytes, packedStart, result, resultStart, bytesToCopy);

        return result;
    }

    private static BigInteger getBigInteger(BitVal bitVal, byte[] bytes) {
        final boolean littleEndian = bitVal.ordering() == BitOrdering.LITTLE;
        final byte[] inBytes = littleEndian ? flip(bytes) : bytes;

        final BigInteger mask = BigInteger.ONE
                .shiftLeft(bitVal.len())
                .subtract(BigInteger.ONE);

        return new BigInteger(1, inBytes)
                .shiftRight(bitVal.first())
                .and(mask);
    }

    private static byte[] flip(byte[] in) {
        final byte[] result = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            result[i] = in[in.length - 1 - i];
        }
        return result;
    }

}
