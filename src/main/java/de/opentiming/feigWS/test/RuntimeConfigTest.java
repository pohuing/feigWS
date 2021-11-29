package de.opentiming.feigWS.test;

import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.reader.TagFilter;
import de.opentiming.feigWS.reader.filterVariants.SnrRangeFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class RuntimeConfigTest {
    private RuntimeConfig referenceConfig;
    private final String referenceLocation = "testingConfig";

    @BeforeEach
    void setUp() {
        referenceConfig = new RuntimeConfig();
        referenceConfig.setConfigLocation(referenceLocation);
    }

    @AfterEach
    void tearDown() {
        new File(referenceLocation).delete();
    }

    @Test
    void serializeToXML() {
        List<TagFilter> filters = new ArrayList<>();
        SnrRangeFilter filter = new SnrRangeFilter(0,100,referenceConfig);
        filters.add(filter);
        referenceConfig.setFilters(filters);

        Assertions.assertTrue(referenceConfig.serializeToXML());

        RuntimeConfig written = RuntimeConfig.deserializeFromXML(referenceLocation).get();
        Assertions.assertEquals(referenceConfig, written);

        Assertions.assertEquals(Optional.empty(), RuntimeConfig.deserializeFromXML("nonsense"));
    }

    @Test
    void init() {
    }
}