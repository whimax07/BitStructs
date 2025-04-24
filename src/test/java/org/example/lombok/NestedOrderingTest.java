package org.example.lombok;

import lombok.AllArgsConstructor;
import org.example.BitDetails;
import org.example.BitStruct;
import org.example.BitVal;
import org.junit.jupiter.api.Test;

import static org.example.Utils.bs;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedOrderingTest {

    @Test
    public void nestedOrdering() {
        final byte[] bytes = bs(0xcf, 0, 0, 0, 0x12, 0, 0, 0);
        final Bank decoded = BitStruct.decode(Bank.class, bytes);

        assertEquals(0xf, decoded.pwrUp0.source);
        assertEquals(1, decoded.pwrUp0.enable);
        assertEquals(1, decoded.pwrUp0.direction);

        assertEquals(0x12, decoded.currentPowerUpReg.currentState);
        assertEquals(0, decoded.currentPowerUpReg.empty);

        final byte[] encoded = decoded.encode();
        assertArrayEquals(bytes, encoded);
    }



    @BitDetails(byteOrdering = BitDetails.ByteOrdering.LITTLE)
    @AllArgsConstructor
    public static class Bank implements BitStruct {
        @BitVal(first = 0, len = 32)
        private PwrUp0 pwrUp0;

        @BitVal(first = 32, len = 32)
        private CurrentPowerUpReg currentPowerUpReg;
    }

    @BitDetails(len = 4, byteOrdering = BitDetails.ByteOrdering.LITTLE)
    @AllArgsConstructor
    public static class PwrUp0 implements BitStruct {
        @BitVal(first = 0, len = 6)
        private byte source;

        @BitVal(first = 6, len = 1)
        private byte enable;

        @BitVal(first = 7, len = 1)
        private byte direction;
    }

    @BitDetails(len = 4, byteOrdering = BitDetails.ByteOrdering.LITTLE)
    @AllArgsConstructor
    public static class CurrentPowerUpReg implements BitStruct {
        @BitVal(first = 0, len = 7)
        private byte currentState;

        @BitVal(first = 7, len = 25)
        private int empty;
    }

}
