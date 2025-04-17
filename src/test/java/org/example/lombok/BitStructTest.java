package org.example.lombok;

import org.example.BitStruct;
import org.example.BitVal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitStructTest {

    @Test
    public void simpleCheck() {
        final byte[] bytes = {0x0, 0x0, (byte) 0b1010_0101, (byte) 0b1111_0101};
        final SimpleCheck decode = BitStruct.decode(SimpleCheck.class, bytes);

        assertEquals(0b0101, decode.delta1);
        assertEquals(0b1010_0101, decode.delta2);
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
