package de.opentiming.feigWS.help;

import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import de.opentiming.feigWS.reader.TagFilter;
import org.springframework.core.env.Environment;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RuntimeConfig {
    private SerialNumberEncodingType tagEncodingType = SerialNumberEncodingType.DECIMAL;
    private List<TagFilter> filters = new ArrayList<>();
    private String configLocation = "config";
    public RuntimeConfig(){}

    public List<TagFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<TagFilter> filters) {
        this.filters = filters;
    }

    public SerialNumberEncodingType getTagEncodingType() {
        return tagEncodingType;
    }

    public void setTagEncodingType(SerialNumberEncodingType tagEncodingType) {
        this.tagEncodingType = tagEncodingType;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public void setConfigLocation(String configLocation) {
        this.configLocation = configLocation;
    }

    public boolean serializeToXML(){
        return serializeToXML(configLocation);
    }

    /**
     * Serializes this using {@link XMLEncoder} at the specified location, overwriting existing files.
     * @return true if none of the writing threw an Exception, false if something went wrong
     */
    public boolean serializeToXML(String path){
        try (FileOutputStream fileOutputStream = new FileOutputStream(path);
             XMLEncoder xmlEncoder = new XMLEncoder(fileOutputStream)) {
            xmlEncoder.setExceptionListener(e -> System.out.println(e.toString()));
            xmlEncoder.writeObject(this);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Tries to create a new RuntimeConfig object from a stored RuntimeConfig xml file using an {@link XMLDecoder}
     * @param path Location of serialized RuntimeConfig
     * @return Optional.of() if deserialization worked, Optional.empty() if not
     */
    public static Optional<RuntimeConfig> deserializeFromXML(String path){
        try(FileInputStream fileInputStream = new FileInputStream(path);
            XMLDecoder xmlDecoder = new XMLDecoder(fileInputStream)){
            RuntimeConfig config = (RuntimeConfig) xmlDecoder.readObject();
            return Optional.of(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Tries to load a RuntimeConfig from file.filters
     * Will create a new {@link RuntimeConfig} if none is found with defaults from env
     */
    public static RuntimeConfig init(Environment env){
        String filterLocation = env.getProperty("config.runtimeConfig");
        if(filterLocation == null){
            filterLocation = "runtimeConfig.xml";
        }
        Optional<RuntimeConfig> runtimeConfig = RuntimeConfig.deserializeFromXML(filterLocation);
        if (runtimeConfig.isPresent()){
            return runtimeConfig.get();
        }
        SerialNumberEncodingType encodingType;
        // Default to hexadecimal tag encoding if no tag.sNFormatting application property is set
        if (env.getProperty("tag.sNFormatting") == null){
            System.out.println("tag.sNFormatting is not defined in application.properties. Defaulting to Hexadecimal");
            encodingType = SerialNumberEncodingType.HEXADECIMAL;
        }else
            encodingType = env.getProperty("tag.sNFormatting").equals("Hex") ? SerialNumberEncodingType.HEXADECIMAL : SerialNumberEncodingType.DECIMAL;
        RuntimeConfig newConfig = new RuntimeConfig();
        newConfig.setConfigLocation(filterLocation);
        newConfig.setTagEncodingType(encodingType);
        newConfig.serializeToXML();
        return newConfig;
    }

    /**
     * Checks if all members and Class match this. Also checks if the Filters are the same using the filter's .equals()
     * @param o other RuntimeConfig to be tested
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuntimeConfig that = (RuntimeConfig) o;
        return getTagEncodingType() == that.getTagEncodingType() && Objects.equals(getFilters(), that.getFilters()) && Objects.equals(getConfigLocation(), that.getConfigLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTagEncodingType(), getFilters(), getConfigLocation());
    }
}
