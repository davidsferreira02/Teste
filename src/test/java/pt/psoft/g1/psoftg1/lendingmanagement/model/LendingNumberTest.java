package pt.psoft.g1.psoftg1.lendingmanagement.model;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.LendingNumber;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class LendingNumberTest {

    @Test
    void ensureLendingNumberNotNull() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber((String) null));
    }

    @Test
    void ensureLendingNumberNotBlank() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(""));
    }

    @Test
    void ensureLendingNumberNotWrongFormat() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("1/2024"));
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("24/1"));
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("2024-1"));
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber("2024\\1"));
    }

    @Test
    void ensureLendingNumberIsSetWithString() {
        final var ln = new LendingNumber("2024/1");
        assertEquals("2024/1", ln.toString());
    }

    @Test
    void ensureLendingNumberIsSetWithSequential() {
        final LendingNumber ln = new LendingNumber(1);
        assertNotNull(ln);
        assertEquals(LocalDate.now().getYear() + "/1", ln.toString());
    }

    @Test
    void ensureLendingNumberIsSetWithYearAndSequential() {
        final LendingNumber ln = new LendingNumber(2024, 1);
        assertNotNull(ln);
        assertEquals("2024/1", ln.toString());
    }

    @Test
    void ensureSequentialCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(2024, -1));
    }

    @Test
    void ensureYearCannotBeInTheFuture() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(LocalDate.now().getYear() + 1, 1));
    }

    @Test
    void ensureYearCannotBeBefore1970() {
        assertThrows(IllegalArgumentException.class, () -> new LendingNumber(1969, 1));
    }

    @Test
    void ensureYearIsValidBetween1970AndCurrentYear() {
        int currentYear = LocalDate.now().getYear();
        final LendingNumber ln = new LendingNumber(1970, 1);
        assertNotNull(ln);
        assertEquals("1970/1", ln.toString());

        final LendingNumber lnCurrentYear = new LendingNumber(currentYear, 2);
        assertNotNull(lnCurrentYear);
        assertEquals(currentYear + "/2", lnCurrentYear.toString());
    }

    @Test
    void testDefaultConstructor() {
        LendingNumber lendingNumber = new LendingNumber();
        assertNotNull(lendingNumber);
    }
}
