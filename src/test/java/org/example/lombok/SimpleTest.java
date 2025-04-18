package org.example.lombok;

import org.example.BitStruct;
import org.example.BitVal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleTest {

    @Test
    public void simpleCheck() {
        final byte[] bytes = {0x0, 0x0, (byte) 0b1010_0101, (byte) 0b1111_0101};
        final SimpleCheck decoded = BitStruct.decode(SimpleCheck.class, bytes);

        assertEquals(0b0101, decoded.delta1);
        assertEquals(0b1010_0101, decoded.delta2);

        final byte[] encoded = decoded.encode();
        // Can't compare with input because non-zero bits 4 to 7.
        assertArrayEquals(new byte[] {-91, 5}, encoded);
    }

    public static class SimpleCheck implements BitStruct {
        @BitVal(first = 0, len =  4)
        private final int delta1;

        @BitVal(first = 8, len = 8)
        private final int delta2;

        public SimpleCheck(int delta1, int delta2) {
            this.delta1 = delta1;
            this.delta2 = delta2;
        }
    }

}
