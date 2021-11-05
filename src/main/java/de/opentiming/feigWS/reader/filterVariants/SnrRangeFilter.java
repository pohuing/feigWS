package de.opentiming.feigWS.reader.filterVariants;

import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.reader.ReaderTag;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import de.opentiming.feigWS.reader.TagFilter;

import java.util.Optional;

/**
 * This filter variant checks if the tag's serial number is at least as big as start and smaller than end
 */
public class SnrRangeFilter implements TagFilter {
    private int start;
    private int end;
    private String name;
    private RuntimeConfig runtimeConfig;

    public SnrRangeFilter(){};


    /**
     * Creates a new Serial number range filter
     * @param start the lower bound of accepted serial numbers
     * @param end the upper exclusive bound of accepted serial numbers
     * @param name the name for this filter instance
     * @param runtimeConfig The {@link RuntimeConfig} to track for encoding filter changes
     */
    public SnrRangeFilter(int start, int end, String name, RuntimeConfig runtimeConfig) {
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        this.start = start;
        this.end = end;
        this.name = name;
        setRuntimeConfig(runtimeConfig);
    }

    /**
     * Creates a new Serial number range filter
     * The name defaults to the class name in this constructor
     * @param start the lower bound of accepted serial numbers
     * @param end the upper exclusive bound of accepted serial numbers
     */
    public SnrRangeFilter(int start, int end, RuntimeConfig runtimeConfig){
        this(start, end, "SnrRangeFilter", runtimeConfig);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Checks if the tag's serial number, interpreted using the encodingType is at least start and less than end
     * @param tag the ReaderTag to validate
     * @return true if within the range, false if not or tag.getSerialNumber() returned an empty Optional
     */
    @Override
    public boolean validate(ReaderTag tag) {
        Optional<Integer> snrOptional = tag.getSerialNumber(runtimeConfig.getTagEncodingType());
        if (snrOptional.isPresent()) {
            int snr = snrOptional.get();
            return snr >= getStart() && snr < getEnd();
        }else
            return false;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public RuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public void setRuntimeConfig(RuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }
}
