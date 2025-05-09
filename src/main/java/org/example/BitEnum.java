package org.example;

/**
 * Annotate an enum to enable support for enum typed fields marked with {@link BitVal}. <br><br>
 *
 * This class should only be implemented by Enum Classes.
 *
 * @author whimax07
 */
public interface BitEnum {

    /** The value the enum constant represents. */
    long val();

}
