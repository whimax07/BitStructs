package org.example;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        final List<Field> bitValFields = Arrays.stream(clazz.getFields())
                .filter(not(BitStruct::isStatic))
                .filter(BitStruct::hasBitValAnnotation)
                .toList();
        final List<String> fieldNames = bitValFields.stream().map(Field::getName).toList();


        final Constructor<?>[] constructors = clazz.getConstructors();
        final Constructor<?> constructor = Arrays.stream(constructors)
                .filter(allFieldConstructor(fieldNames))
                .findAny()
                .orElseThrow(() -> new RuntimeException(
                        "No \"all args\" constructor for BitVals found. " +
                                "Note constructor parameter names and BitVal annotated field names must match."
                ));

        final Object[] constructorArgs = parseBytes(bitValFields, bytes);
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

    private static Object[] parseBytes(List<Field> bitValFields, byte[] bytes) {
        throw new InaccessibleObjectException();
    }

}
