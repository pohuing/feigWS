package de.opentiming.feigWS.reader;

/**
 * TagFilter defines a simple filter for ReaderTags
 */
public interface TagFilter {
    public String getName();
    /**
     * Validates that tag passes the checks of this filter
     * For example a StartSerialNumberFilter might return false if the tag has a serial number lower than the defined
     * start range
     * @param tag the ReaderTag to validate
     * @return true if the checks pass, false if any condition is broken
     */
    boolean validate(ReaderTag tag);
}
