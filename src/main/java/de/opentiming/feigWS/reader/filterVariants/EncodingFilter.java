package de.opentiming.feigWS.reader.filterVariants;

import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.reader.ReaderTag;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import de.opentiming.feigWS.reader.TagFilter;

import java.util.Objects;

public class EncodingFilter implements TagFilter {
    private String name;
    private RuntimeConfig runtimeConfig;
    public EncodingFilter(){}

    public EncodingFilter(RuntimeConfig runtimeConfig, String name){
        setRuntimeConfig(runtimeConfig);
        this.setName(name);
    }

    public EncodingFilter(RuntimeConfig runtimeConfig){
        setName("EncodingFilter");
        setRuntimeConfig(runtimeConfig);
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
        if(tag == null)
            return false;
        return getRuntimeConfig().getTagEncodingType() != SerialNumberEncodingType.DECIMAL || !tag.sNContainsCharacters();
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public void setRuntimeConfig(RuntimeConfig runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Tests if other is of the same Class, and it's members(except for {@link RuntimeConfig} are the same
     * @param o other Object to check for equality
     * @return true if o is of the same class, and it's members have the same values
     * @implNote This does not test the {@link RuntimeConfig}'s equality as this would introduce recursion
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncodingFilter that = (EncodingFilter) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
