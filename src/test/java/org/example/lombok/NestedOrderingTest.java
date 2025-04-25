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
    public void nestedOrderingLittle() {
        final byte[] bytes = bs(0xcf, 0, 0, 0, 0x12, 0, 0, 0, 0x1e, 0, 0, 2);
        final BankLittle decoded = BitStruct.decode(BankLittle.class, bytes);

        // [0 - 3] {0xcf, 0, 0, 0} <=> [0 - 3]
        assertEquals(0xf, decoded.pwrUp0.source);
        assertEquals(1, decoded.pwrUp0.enable);
        assertEquals(1, decoded.pwrUp0.direction);

        // [4 - 7] {0x12, 0, 0, 0} <=> [0 - 3]
        assertEquals(0x12, decoded.currentPowerUpReg.currentState);
        assertEquals(0, decoded.currentPowerUpReg.empty);

        // [8 - 11] {2, 0, 0, 0x1e} <=> [3 - 0]
        assertEquals(2, decoded.statusReg.status);
        assertEquals(15, decoded.statusReg.date);

        final byte[] encoded = decoded.encode();
        assertArrayEquals(bytes, encoded);
    }

    @Test
    public void nestedOrderingBig() {
        final byte[] bytes = bs(0x1e, 0, 0, 2, 0x12, 0, 0, 0, 0xcf, 0, 0, 0);
        final BankBig decoded = BitStruct.decode(BankBig.class, bytes);

        // [3 - 0] {0xcf, 0, 0, 0} <=> [0 - 3]
        assertEquals(0xf, decoded.pwrUp0.source);
        assertEquals(1, decoded.pwrUp0.enable);
        assertEquals(1, decoded.pwrUp0.direction);

        // [7 - 4] {0x12, 0, 0, 0} <=> [0 - 3]
        assertEquals(0x12, decoded.currentPowerUpReg.currentState);
        assertEquals(0, decoded.currentPowerUpReg.empty);

        // [11 - 8] {0x1e, 0, 0, 2} <=> [3 - 0]
        assertEquals(2, decoded.statusReg.status);
        assertEquals(15, decoded.statusReg.date);

        final byte[] encoded = decoded.encode();
        assertArrayEquals(bytes, encoded);
    }



    @BitDetails(byteOrdering = BitDetails.ByteOrdering.LITTLE)
    @AllArgsConstructor
    public static class BankLittle implements BitStruct {
        @BitVal(first = 0, len = 32)
        private PwrUp0 pwrUp0;

        @BitVal(first = 32, len = 32)
        private CurrentPowerUpReg currentPowerUpReg;

        @BitVal(first = 64, len = 32)
        private StatusReg statusReg;
    }

    @BitDetails(byteOrdering = BitDetails.ByteOrdering.BIG)
    @AllArgsConstructor
    public static class BankBig implements BitStruct {
        @BitVal(first = 0, len = 32)
        private PwrUp0 pwrUp0;

        @BitVal(first = 32, len = 32)
        private CurrentPowerUpReg currentPowerUpReg;

        @BitVal(first = 64, len = 32)
        private StatusReg statusReg;
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
    public record CurrentPowerUpReg(
            @BitVal(first = 0, len = 7) byte currentState,
            @BitVal(first = 7, len = 25) int empty
    ) implements BitStruct { }

    @BitDetails(len = 4, byteOrdering = BitDetails.ByteOrdering.BIG)
    @AllArgsConstructor
    public static class StatusReg implements BitStruct {
        @BitVal(first = 0, len = 3)
        private byte status;

        @BitVal(first = 25, len = 4)
        private byte date;
    }

}
