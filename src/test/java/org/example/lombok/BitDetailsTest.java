package org.example.lombok;

import lombok.AllArgsConstructor;
import org.example.BitDetails;
import org.example.BitStruct;
import org.example.BitVal;
import org.junit.jupiter.api.Test;

import static org.example.Utils.bs;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitDetailsTest {

    @Test
    public void bitDetails_BigEndian() {
        final BigMessage bigMessage = new BigMessage(0x12_34_56_78);
        final byte[] encoded = bigMessage.encode();
        assertArrayEquals(bs(0, 0, 0, 0x12, 0x34, 0x56, 0x78, 0), encoded);

        final BigMessage decoded = BitStruct.decode(BigMessage.class, encoded);
        assertEquals(bigMessage.header, decoded.header);
    }

    @Test
    public void bitDetails_LittleEndian() {
        final LittleMessage littleMessage = new LittleMessage(0x12_34_56_78);
        final byte[] encoded = littleMessage.encode();
        assertArrayEquals(bs(0, 0x78, 0x56, 0x34, 0x12, 0, 0, 0), encoded);

        final LittleMessage decoded = BitStruct.decode(LittleMessage.class, encoded);
        assertEquals(littleMessage.header, decoded.header);
    }



    @BitDetails(len = 8, byteOrdering = BitDetails.ByteOrdering.BIG)
    @AllArgsConstructor
    private static class BigMessage implements BitStruct {
        @BitVal(first = 8, len = 32)
        private int header;
    }

    @BitDetails(len = 8, byteOrdering = BitDetails.ByteOrdering.LITTLE)
    @AllArgsConstructor
    private static class LittleMessage implements BitStruct {
        @BitVal(first = 8, len = 32)
        private int header;
    }

}
