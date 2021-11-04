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
}
