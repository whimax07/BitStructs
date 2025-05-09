package org.example.lombok;

import lombok.Data;
import org.example.BitEnum;
import org.example.BitStruct;
import org.example.BitVal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumParamsTest {

    @Test
    public void simpleCheck() {
        final byte[] bytes = {0x0, 0x0, (byte) 0b0000_0100, (byte) 0b1111_0101};
        final SimpleCheck decoded = BitStruct.decode(SimpleCheck.class, bytes);

        assertEquals(0b0101, decoded.delta1);
        assertEquals(TestEnum.C, decoded.delta2);

        final byte[] encoded = decoded.encode();
        // Can't compare with input because non-zero bits 4 to 7.
        assertArrayEquals(new byte[] {4, 5}, encoded);
    }



    @Data
    public static class SimpleCheck implements BitStruct {
        @BitVal(first = 0, len =  4)
        private final int delta1;

        @BitVal(first = 8, len = 8)
        private final TestEnum delta2;
    }

    public enum TestEnum implements BitEnum {
        A(0b1),
        B(0b10),
        C(0b100);

        private final int value;
        TestEnum(int value) {
            this.value = value;
        }

        @Override
        public long val() {
            return value;
        }
    }

}
