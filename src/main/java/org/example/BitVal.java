package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BitVal {

    /** First bit of the bit field. */
    int first();
    /** Length of the bit field, in bits. */
    int len();

    BitOrdering ordering() default BitOrdering.BIG;



    enum BitOrdering {
        LITTLE,
        BIG
    }

}
