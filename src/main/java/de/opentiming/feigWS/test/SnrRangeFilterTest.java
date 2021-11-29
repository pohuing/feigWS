package de.opentiming.feigWS.test;

import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.reader.ReaderTag;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import de.opentiming.feigWS.reader.filterVariants.SnrRangeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class SnrRangeFilterTest {
    // Filters all non hexadecimals out
    public SnrRangeFilter from10to50Filter;
    public ReaderTag tagNr0 = new ReaderTag("0000_0000", "0000", "", LocalDateTime.MIN, "", "", "", "", "");
    public ReaderTag tagNr10 = new ReaderTag("0000_0010", "0000", "", LocalDateTime.MIN, "", "", "", "", "");
    public ReaderTag tagNr49 = new ReaderTag("0000_0049", "0000", "", LocalDateTime.MIN, "", "", "", "", "");
    public ReaderTag tagNr50 = new ReaderTag("0000_0050", "0000", "", LocalDateTime.MIN, "", "", "", "", "");
    public ReaderTag tagNr51 = new ReaderTag("0000_0051", "0000", "", LocalDateTime.MIN, "", "", "", "", "");

    public RuntimeConfig decConfig = new RuntimeConfig();


    @BeforeEach
    void setUp() {
        decConfig.setTagEncodingType(SerialNumberEncodingType.DECIMAL);

        from10to50Filter = new SnrRangeFilter(10, 50, "10 to 50", decConfig);
    }

    @Test
    void validateShouldFail() {
        Assertions.assertFalse(from10to50Filter.validate(tagNr0));
        Assertions.assertFalse(from10to50Filter.validate(tagNr50));
        Assertions.assertFalse(from10to50Filter.validate(tagNr51));
    }

    @Test
    void validateShouldSucceed(){
        Assertions.assertTrue(from10to50Filter.validate(tagNr10));
        Assertions.assertTrue(from10to50Filter.validate(tagNr49));
    }

    @Test
    void SnrRangeFilter(){
        Assertions.assertEquals(new SnrRangeFilter(10,50,"10 to 50", decConfig), new SnrRangeFilter(50,10,"10 to 50", decConfig));
    }
}