# BitStructs
A helper library for defining, serializing and deserializing bit-level structures with annotations.

This is largely a pseudocode implementation given it is not backed by compile time processing, enabling type checking and performance gains.

The libary has nice integration with Lombok.

## Example
This is a ruff example of how you might model a few registers.

### Structured Data
```java
final byte[] bytes = new byte[] {0xcf, 0, 0, 0, 0x12, 0, 0, 0, 0x1e, 0, 0, 2};
final BankLittle decoded = BitStruct.decode(BankLittle.class, bytes);

assertEquals( 0xf, decoded.pwrUp0.source);
assertEquals(   1, decoded.pwrUp0.enable);
assertEquals(   1, decoded.pwrUp0.direction);

assertEquals(0x12, decoded.currentPowerUpReg.currentState);
assertEquals(   0, decoded.currentPowerUpReg.empty);

assertEquals(   2, decoded.statusReg.status);
assertEquals(  15, decoded.statusReg.date);
```

### Backing Classes
```java
@BitDetails(byteOrdering = BitDetails.ByteOrdering.LITTLE)
@AllArgsConstructor
public class BankLittle implements BitStruct {
    @BitVal(first = 0, len = 32)
    private final PwrUp0 pwrUp0;

    @BitVal(first = 32, len = 32)
    private final CurrentPowerUpReg currentPowerUpReg;

    @BitVal(first = 64, len = 32)
    private final StatusReg statusReg;
}



@BitDetails(len = 4, byteOrdering = BitDetails.ByteOrdering.LITTLE)
@AllArgsConstructor
public class PwrUp0 implements BitStruct {
    @BitVal(first = 0, len = 6)
    private final byte source;

    @BitVal(first = 6, len = 1)
    private final byte enable;

    @BitVal(first = 7, len = 1)
    private final byte direction;
}

@BitDetails(len = 4, byteOrdering = BitDetails.ByteOrdering.LITTLE)
@AllArgsConstructor
public class CurrentPowerUpReg implements BitStruct {
    @BitVal(first = 0, len = 7)
    private final byte currentState;

    @BitVal(first = 7, len = 25)
    private final int empty;
}

@BitDetails(len = 4, byteOrdering = BitDetails.ByteOrdering.BIG)
@AllArgsConstructor
public class StatusReg implements BitStruct {
    @BitVal(first = 0, len = 3)
    private final byte status;

    @BitVal(first = 25, len = 4)
    private final byte date;
}
```
