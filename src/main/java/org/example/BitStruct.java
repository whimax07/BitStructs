package org.example;

import org.example.BitDetails.ByteOrdering;
import org.example.Endian.BBI;
import org.example.Endian.LBI;
import org.example.Endian.Ray;

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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * Implement this interface to add serialization and deserialization for fields marked with {@link BitVal}.
 *
 * @author Whimax07
 */
public interface BitStruct {

    // =================================================================================================================
    // ===> Public interface.

    /**
     * Serialize fields marked with {@link BitVal} according to the parameter defined by {@link BitDetails} recursively.
     */
    default byte[] encode() {
        return encodeImpl(this);
    }

    /**
     * Deserialize fields marked with {@link BitVal} according to the parameter defined by {@link BitDetails}
     * recursively.
     */
    static <T extends BitStruct> T decode(Class<T> clazz, byte[] bytes) {
        return decodeImpl(clazz, bytes);
    }



    // =================================================================================================================
    // ===> Private implementation.

    private static byte[] encodeImpl(BitStruct self) {
        final Class<? extends BitStruct> clazz = self.getClass();

        final List<Field> bitValFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(not(BitStruct::isStatic))
                .filter(BitStruct::hasBitValAnnotation)
                .toList();

        final int size = getByteArraySize(clazz, bitValFields);

        return combineFields(self, bitValFields, size);
    }

    private static boolean isStatic(Field field) {
        return field.accessFlags().contains(AccessFlag.STATIC);
    }

    private static boolean hasBitValAnnotation(Field field) {
        return Arrays.stream(field.getAnnotations()).anyMatch(BitVal.class::isInstance);
    }

    private static boolean notConst(Field field) {
        return Arrays.stream(field.getAnnotations())
                .filter(BitVal.class::isInstance)
                .map(BitVal.class::cast)
                .noneMatch(BitVal::constant);
    }

    private static int getByteArraySize(Class<? extends BitStruct> clazz, List<Field> bitValFields) {
        final BitDetails annotation = clazz.getDeclaredAnnotation(BitDetails.class);
        if (annotation != null && annotation.len() != BitDetails.UNSET) {
            return annotation.len();
        }

        // Deduce how bit the struct takes up by finding the largest BitVal field.
        final int numBits = bitValFields.stream()
                .map(field -> field.getDeclaredAnnotation(BitVal.class))
                .filter(Objects::nonNull)
                .mapToInt(bitVal -> bitVal.first() + bitVal.len())
                .max()
                .orElseThrow(() -> new RuntimeException("No BitVal fields found."));

        return (numBits + 7) / 8;
    }



    private static <T extends BitStruct> T decodeImpl(Class<T> clazz, byte[] bytes) {
        if (clazz.isEnum()) throw new RuntimeException("Can't populate Enums classes.");

        final ArrayList<Field> bitValFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(not(BitStruct::isStatic))
                .filter(BitStruct::hasBitValAnnotation)
                .filter(BitStruct::notConst)
                .collect(Collectors.toCollection(ArrayList::new));
        final List<String> fieldNames = bitValFields.stream().map(Field::getName).toList();

        final int size = getByteArraySize(clazz, bitValFields);
        if (bytes.length < size) throw new RuntimeException("Passed in byte array is to small. Required size: " + size);

        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        final Constructor<?> constructor = Arrays.stream(constructors)
                .filter(allFieldConstructor(fieldNames))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        "No \"all args\" constructor for BitVals found. " +
                                "Note constructor parameter names and BitVal annotated field names must match."
                ));

        bitValFields.sort(constructorOrdering(constructor));

        final ByteOrdering ordering = getByteOrdering(clazz);
        final byte[] orderedBytes = (ordering == ByteOrdering.BIG) ? bytes : flip(bytes);
        final byte[] truncatedBytes = Arrays.copyOfRange(orderedBytes, bytes.length - size, bytes.length);
        final Object[] constructorArgs = constructArgs(bitValFields, truncatedBytes, ordering);
        constructor.setAccessible(true);

        try {
            //noinspection unchecked
            return (T) constructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends BitStruct> byte[] combineFields(T self, List<Field> fields, int size) {
        final ByteOrdering ordering = getByteOrdering(self.getClass());

        if (ordering != ByteOrdering.BIG && ordering != ByteOrdering.LITTLE) {
            throw new IllegalStateException("Unknown byte ordering: " + ordering);
        }

        final Ray ray = (ordering == ByteOrdering.BIG) ?
                combineFieldsBigEndian(self, fields, ordering) :
                combineFieldsLittleEndian(self, fields, ordering);

        return ray.leastSignificant(size);
    }

    private static <T extends BitStruct> LBI combineFieldsLittleEndian(T self, List<Field> fields, ByteOrdering ordering) {
        LBI acculator = LBI.ZERO;

        for (Field field : fields) {
            final BitVal bitVal = field.getAnnotation(BitVal.class);
            assert bitVal != null;
            // This cast is the main reason for LBI and BBI to exist.
            final LBI bitValRay = (LBI) valueOf(self, field, ordering);

            final LBI mask = LBI.ONE.leftShift(bitVal.len()).subtract(LBI.ONE);
            final LBI positionedVal = bitValRay.and(mask).leftShift(bitVal.first());

            final LBI insertMask = mask.leftShift(bitVal.first()).not();
            acculator = acculator.and(insertMask).or(positionedVal);
        }

        return acculator;
    }

    private static <T extends BitStruct> BBI combineFieldsBigEndian(T self, List<Field> fields, ByteOrdering ordering) {
        BBI acculator = BBI.ZERO;

        for (Field field : fields) {
            final BitVal bitVal = field.getAnnotation(BitVal.class);
            assert bitVal != null;
            // This cast is the main reason for LBI and BBI to exist.
            final BBI bitValRay = (BBI) valueOf(self, field, ordering);

            final BBI mask = BBI.ONE.leftShift(bitVal.len()).subtract(BBI.ONE);
            final BBI positionedVal = bitValRay.and(mask).leftShift(bitVal.first());

            final BBI insertMask = mask.leftShift(bitVal.first()).not();
            acculator = acculator.and(insertMask).or(positionedVal);
        }

        return acculator;
    }

    private static <T extends BitStruct> Ray valueOf(T self, Field field, ByteOrdering ordering) {
        field.setAccessible(true);
        final Object object;
        try {
            object = field.get(self);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get value of field.", e);
        }

        final Class<?> type = field.getType();
        if (BitStruct.class.isAssignableFrom(type)) {
            final BitStruct innerStruct = (BitStruct) object;
            final byte[] inner = innerStruct.encode();
            // The constructors expect "Big Endian" ordering.
            final byte[] orderedInner = (ordering == ByteOrdering.BIG) ? inner : flip(inner);
            return (ordering == ByteOrdering.BIG) ? new BBI(orderedInner) : new LBI(orderedInner);
        }

        if (BitEnum.class.isAssignableFrom(type)) {
            assert type.isEnum();
            final BitEnum bitEnum = (BitEnum) object;
            return (ordering == ByteOrdering.BIG) ? new BBI(bitEnum.val()) : new LBI(bitEnum.val());
        }

        if (isIntType(object.getClass())) {
            final Number asNumber = (Number) object;
            final long asLong = asNumber.longValue();
            return (ordering == ByteOrdering.BIG) ? new BBI(asLong) : new LBI(asLong);
        }

        throw new IllegalStateException("Can't extract a value from type. Field=" + field);
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

    private static <T extends BitStruct> ByteOrdering getByteOrdering(Class<T> clazz) {
        final BitDetails bitDetails = clazz.getDeclaredAnnotation(BitDetails.class);
        return (bitDetails != null) ? bitDetails.byteOrdering() : ByteOrdering.BIG;
    }

    private static Object[] constructArgs(List<Field> bitValFields, byte[] bytes, ByteOrdering ordering) {
        final Object[] constructorArgs = new Object[bitValFields.size()];

        for (int i = 0; i < bitValFields.size(); i++) {
            final Field field = bitValFields.get(i);
            final BitVal bitVal = field.getAnnotation(BitVal.class);
            final Class<?> baseType = getBaseType(field.getType());
            final Object extractedVal = extractVal(bitVal, baseType, ordering, bytes);
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
                || BitEnum.class.isAssignableFrom(type)
                || isIntType(type);
        if (isGood) return type;

        throw new IllegalStateException("Unsupported type: " + type.getSimpleName());
    }

    private static boolean isIntType(Class<?> clazz) {
        return Boolean.class.equals(clazz)
                || Byte.class.equals(clazz)
                || Short.class.equals(clazz)
                || Integer.class.equals(clazz)
                || Long.class.equals(clazz);
    }



    private static Object extractVal(BitVal bitVal, Class<?> baseType, ByteOrdering ordering, byte[] bytes) {
        if (BitStruct.class.isAssignableFrom(baseType)) {
            @SuppressWarnings("unchecked") // Safe by if branch condition.
            final Class<? extends BitStruct> bound = (Class<? extends BitStruct>) baseType;

            final byte[] subRange = getSubRange(bitVal, bytes);
            // Restore the byte ordering to big endian so the recursive call does the correct thing.
            final byte[] bigSubRange = (ordering == ByteOrdering.BIG) ? subRange : flip(subRange);
            return BitStruct.decode(bound, bigSubRange);
        }

        if (BitEnum.class.isAssignableFrom(baseType)) {
            assert baseType.isEnum();
            final BitEnum[] enumConstants = (BitEnum[]) baseType.getEnumConstants();
            final long extractedVal = getBigInteger(bitVal, bytes).longValue();

            return Arrays.stream(enumConstants)
                    .filter(bitEnum -> bitEnum.val() == extractedVal)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "No enum constant found. [Type=%s, Value=%s] ", baseType, extractedVal
                    )));
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

        final int neededBytes = (bitVal.len() + 7) / 8;
        final byte[] result = new byte[neededBytes];
        injectEnd(result, packedBytes);
        return result;
    }

    private static BigInteger getBigInteger(BitVal bitVal, byte[] bytes) {
        final BigInteger mask = BigInteger.ONE
                .shiftLeft(bitVal.len())
                .subtract(BigInteger.ONE);

        return new BigInteger(1, bytes)
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

    private static void injectEnd(byte[] base, byte[] toInject) {
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
        final int packedStart = Math.max(toInject.length - base.length, 0);
        final int resultStart = Math.max(base.length - toInject.length, 0);
        final int bytesToCopy = Math.min(toInject.length, base.length);
        System.arraycopy(toInject, packedStart, base, resultStart, bytesToCopy);
    }

}
