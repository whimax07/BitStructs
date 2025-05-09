# BitStructs
A helper library for defining, serializing and deserializing bit-level structures with annotations.

This is largely a pseudocode implementation given it is not backed by compile time processing, enabling type checking and performance gains.

The library has nice integration with Lombok. You can use records instead of lombok but the annotations can make it hard to read. 

Fields can be enums. 

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

// Enum value.
assertEquals( BAD, decoded.statusReg.status);
assertEquals(  15, decoded.statusReg.date);

final byte[] encoded = decoded.encode();
assertArrayEquals(bytes, encoded);
```

### Backing Classes

```java
import org.example.BitEnum;

@BitDetails(byteOrdering = LITTLE)
@AllArgsConstructor
public class BankLittle implements BitStruct {
    @BitVal(first = 0, len = 32)
    private final PwrUp0 pwrUp0;

    @BitVal(first = 32, len = 32)
    private final CurrentPowerUpReg currentPowerUpReg;

    @BitVal(first = 64, len = 32)
    private final StatusReg statusReg;
}



@BitDetails(len = 4, byteOrdering = LITTLE)
@AllArgsConstructor
public class PwrUp0 implements BitStruct {
    @BitVal(first = 0, len = 6)
    private final byte source;

    @BitVal(first = 6, len = 1)
    private final byte enable;

    @BitVal(first = 7, len = 1)
    private final byte direction;
}

@BitDetails(len = 4, byteOrdering = LITTLE)
@AllArgsConstructor
public class CurrentPowerUpReg implements BitStruct {
    @BitVal(first = 0, len = 7)
    private final byte currentState;

    @BitVal(first = 7, len = 25)
    private final int empty;
}

@BitDetails(len = 4, byteOrdering = BIG)
@AllArgsConstructor
public class StatusReg implements BitStruct {
    @BitVal(first = 0, len = 3)
    private final Statuses status;

    @BitVal(first = 25, len = 4)
    private final byte date;
}

@AllArgsConstructor
public enum Statuses implements BitEnum {
    GOOD(0b1),
    BAD(0b10),
    WAITING(0b100),
    ERROR(0b1000);

    private final int value;

    @Override
    public long val() {
        return value;
    }
}
```
