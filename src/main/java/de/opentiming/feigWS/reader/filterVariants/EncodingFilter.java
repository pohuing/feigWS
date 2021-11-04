package de.opentiming.feigWS.reader.filterVariants;

import de.opentiming.feigWS.reader.ReaderTag;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import de.opentiming.feigWS.reader.TagFilter;

public class EncodingFilter implements TagFilter {
    private String name;
    private SerialNumberEncodingType encodingType;
    public EncodingFilter(){}

    public EncodingFilter(SerialNumberEncodingType encodingType, String name){
        this.setEncodingType(encodingType);
        this.setName(name);
    }

    public EncodingFilter(SerialNumberEncodingType encodingType){
        setName("EncodingFilter");
        this.setEncodingType(encodingType);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Checks if the tag's serial number could be of the correct type
     * This cannot be an absolutely perfect check as some hexadecimal numbers are also decimal numbers and vice versa
     * @param tag the ReaderTag to validate
     * @return true if
     */
    @Override
    public boolean validate(ReaderTag tag) {
        return getEncodingType() != SerialNumberEncodingType.DECIMAL || !tag.sNContainsCharacters();
    }

    public void setName(String name) {
        this.name = name;
    }

    public SerialNumberEncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(SerialNumberEncodingType encodingType) {
        this.encodingType = encodingType;
    }
}
