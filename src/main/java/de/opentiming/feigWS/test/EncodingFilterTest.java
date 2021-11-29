package de.opentiming.feigWS.test;

import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.reader.ReaderTag;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import de.opentiming.feigWS.reader.filterVariants.EncodingFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class EncodingFilterTest {
    // Filters all non hexadecimals out
    public EncodingFilter hexFilter;
    // Filters all not purely decimals out
    public EncodingFilter decFilter;

    public ReaderTag hexTag = new ReaderTag("00001c00", "0000", "", LocalDateTime.MIN, "", "", "", "", "");
    public ReaderTag decTag = new ReaderTag("00005500", "0000", "", LocalDateTime.MIN, "", "", "", "", "");


    @BeforeEach
    void setUp() {
        RuntimeConfig hexConfig = new RuntimeConfig();
        hexConfig.setTagEncodingType(SerialNumberEncodingType.HEXADECIMAL);
        hexFilter = new EncodingFilter(hexConfig, "hexFilter");

        RuntimeConfig decConfig = new RuntimeConfig();
        decConfig.setTagEncodingType(SerialNumberEncodingType.DECIMAL);
        decFilter = new EncodingFilter(decConfig, "decFilter");
    }

    /**
     * Unfortunately all decimal numbers are also hexadecimal numbers, so only the decimal filter can actually fail from
     * a wrongly encoded value
     */
    @Test
    void validateShouldFail() {
        Assertions.assertFalse(decFilter.validate(hexTag));
        Assertions.assertFalse(decFilter.validate(null));
        Assertions.assertFalse(hexFilter.validate(null));
    }

    @Test
    void validateShouldWork(){
        Assertions.assertTrue(decFilter.validate(decTag));
        Assertions.assertTrue(hexFilter.validate(hexTag));
    }
}