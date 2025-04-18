package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BitDetails {

    /**
     * This is the default for {@link BitDetails#len()}, and indicates that the class should deduce the size of the
     * struct.
     */
    int UNSET = -1;



    /**
     * Size in bytes the annotated class takes up when in its bit form. If not set, the value is calculated is the
     * smallest number of whole bytes needed to fit all fields annotated with {@link BitVal}.
     */
    int len() default UNSET;

    /** Byte order of the class when in bit form. */
    ByteOrdering byteOrdering() default ByteOrdering.BIG;

    // TODO(Max): Add word size?



    enum ByteOrdering {
        LITTLE,
        BIG
    }

}
