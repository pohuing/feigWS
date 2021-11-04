package de.opentiming.feigWS.reader.filterVariants;

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
    public SerialNumberEncodingType encodingType;
    private String name;

    public SnrRangeFilter(){};


    /**
     * Creates a new Serial number range filter
     * @param start the lower bound of accepted serial numbers
     * @param end the upper exclusive bound of accepted serial numbers
     * @param encodingType the encoding type used to interpret the tag's serial number
     * @param name the name for this filter instance
     */
    public SnrRangeFilter(int start, int end, SerialNumberEncodingType encodingType, String name) {
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        this.start = start;
        this.end = end;
        this.encodingType = encodingType;
        this.name = name;

    }

    /**
     * Creates a new Serial number range filter
     * The name defaults to the class name in this constructor
     * @param start the lower bound of accepted serial numbers
     * @param end the upper exclusive bound of accepted serial numbers
     * @param encodingType the encoding type used to interpret the tag's serial number
     */
    public SnrRangeFilter(int start, int end, SerialNumberEncodingType encodingType){
        this(start, end, encodingType, "SnrRangeFilter");
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
        Optional<Integer> snrOptional = tag.getSerialNumber(encodingType);
        if (snrOptional.isPresent()) {
            int snr = snrOptional.get();
            return snr >= getStart() && snr < end;
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
}
