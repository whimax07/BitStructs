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



    int len() default UNSET;

    /** Byte order. */
    ByteOrdering byteOrdering() default ByteOrdering.BIG;



    enum ByteOrdering {
        LITTLE,
        BIG
    }

}
