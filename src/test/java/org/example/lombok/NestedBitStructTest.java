package org.example.lombok;

import lombok.AllArgsConstructor;
import org.example.BitStruct;
import org.example.BitVal;
import org.junit.jupiter.api.Test;

import static org.example.Utils.bs;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedBitStructTest {

    @Test
    public void simpleNestedTest() {
        final byte[] bytes = bs(0x11, 0xcb, 0xaa, 0x12, 0xf3);
        final ParentStruct decoded = BitStruct.decode(ParentStruct.class, bytes);
        assertEquals(3, decoded.delta1);
        assertEquals(0x12, decoded.delta2);
        assertEquals(0xa, decoded.nested.firstChunk);
        assertEquals(0xcba, decoded.nested.secondChunk);
        assertEquals(1, decoded.lastBit);

        final byte[] encoded = decoded.encode();
        final byte[] expected = bs(0x10, 0xcb, 0xaa, 0x12, 0x3);
        assertArrayEquals(expected, encoded);
    }

    @AllArgsConstructor
    public static class ParentStruct implements BitStruct {
        @BitVal(first = 0, len =  4)
        private final int delta1;

        @BitVal(first = 8, len = 8)
        private final int delta2;

        @BitVal(first = 16, len = 16)
        private final NestedStruct nested;

        @BitVal(first = 36, len = 1)
        private final byte lastBit;
    }

    @AllArgsConstructor
    public static class NestedStruct implements BitStruct {
        @BitVal(first = 0 , len = 4)
        private final byte firstChunk;

        @BitVal(first = 4, len = 12)
        private final short secondChunk;
    }

}
