package de.opentiming.feigWS.test;

import de.opentiming.feigWS.reader.ReaderTag;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

class ReaderTagTest {
    public ReaderTag hexTag = new ReaderTag("00001c00", "0000", "", LocalDateTime.MIN, "", "0010", "", "127.0.0.1", "14:54:06");
    public ReaderTag decTag = new ReaderTag("00005500", "0000", "", LocalDateTime.MIN, "", "0010", "", "127.0.0.1", "14:54:06");

    @Test
    void getSerialNumberShouldSucceed() {
        Assertions.assertEquals(7168, hexTag.getSerialNumber(SerialNumberEncodingType.HEXADECIMAL).get());
        Assertions.assertEquals(5500, decTag.getSerialNumber(SerialNumberEncodingType.DECIMAL).get());
    }

    @Test
    void getSerialNumberShouldFail(){
        Assertions.assertEquals(Optional.empty(), hexTag.getSerialNumber(SerialNumberEncodingType.DECIMAL));
    }

    @Test
    void sNContainsCharacters() {
        Assertions.assertTrue(hexTag.sNContainsCharacters());
        Assertions.assertFalse(decTag.sNContainsCharacters());
    }

    @Test
    void formatForCSV() {
        Assertions.assertEquals(
                "7168;+1000000000-01-01;00:00:00;000;127.0.0.1;0010;;0000;14:54:06",
                hexTag.formatForCSV(SerialNumberEncodingType.HEXADECIMAL));
        Assertions.assertNotEquals(
                "7168;+1000000000-01-01;00:00:00;000;127.0.0.1;0010;;0000;14:54:06",
                hexTag.formatForCSV(SerialNumberEncodingType.DECIMAL));
    }
}