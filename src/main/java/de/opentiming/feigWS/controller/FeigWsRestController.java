package de.opentiming.feigWS.controller;

import de.opentiming.feigWS.help.FileOutput;
import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.help.StartReaderThread;
import de.opentiming.feigWS.reader.*;
import de.opentiming.feigWS.reader.filterVariants.EncodingFilter;
import de.opentiming.feigWS.reader.filterVariants.SnrRangeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static de.opentiming.feigWS.reader.SerialNumberEncodingType.DECIMAL;
import static de.opentiming.feigWS.reader.SerialNumberEncodingType.HEXADECIMAL;

@RestController
@RequestMapping(value="/api")
public class FeigWsRestController {

	@Autowired
	private Environment env;
	
	@Resource(name = "connections")
	private Map<String, FedmConnect> connections;
	
	@Resource(name = "brmthreads")
	private Map<String, BrmReadThread> brmthreads;

	/**
	 * @param reader ip form of reader from application.properties
	 * @return json object with fields "mode"
	 * 									"transponderValidTime"
	 * 									"readerTime"
	 * 									"antenna"
	 * 									"power"
	 * 									"relais"
	 */
    @RequestMapping(value="/{reader}/info", method=RequestMethod.GET)
    public Map<String, Object> getReaderInfo(@PathVariable String reader) {
    	FedmConnect con = connections.get(reader);
    	ReaderInfo ri = new ReaderInfo(con);
    	Map<String, Object> config = ri.getConfig();
    	
    	ReaderResultFiles rf = new ReaderResultFiles(env.getProperty("file.output"));
    	config.put("files", rf.getResultFiles(reader));
    	
    	if(config.get("mode") != null && config.get("mode").equals("ISO")) {
    		if(brmthreads.get(reader) != null) {
    			brmthreads.get(reader).setRunning(false);
    			brmthreads.put(reader, null);
    		}
    	}
        if (brmthreads.get(reader) != null){
            config.put("encoding", brmthreads.get(reader).getEncodingType());
            SnrRangeFilter tagFilter = (SnrRangeFilter) brmthreads.get(reader).getRuntimeConfig().getFilters().stream().filter(f -> f instanceof SnrRangeFilter).findFirst().orElse(new SnrRangeFilter());
            config.put("lower", tagFilter.getStart());
            config.put("upper", tagFilter.getEnd());
        }
    	return config;
    }

	/**
	 * @param reader ip representation of an existing reader
	 * @param value Binary bitset of antennas to be enabled, 1110 would enable antenna 2, 3 and 4
	 * @return always true
	 */
    @RequestMapping(value="/{reader}/ant/{value}", method=RequestMethod.GET)
    public boolean setAntenna(@PathVariable String reader, @PathVariable String value) {
    	FedmConnect con = connections.get(reader);
    	ReaderAntenna a = new ReaderAntenna(con);
    	return a.setAntennas(value);
    }

	/**
	 * @param reader ip representation of an existing reader
	 * @param value BRM or ISO, defaults to BRM on invalid input
	 * @return always true
	 */
    @RequestMapping(value="/{reader}/mode/{value}", method=RequestMethod.GET)
    public boolean setMode(@PathVariable String reader, @PathVariable String value) {
    	FedmConnect con = connections.get(reader);

		ReaderMode m = new ReaderMode(con);
    	m.setMode(value);
    	
        ReaderInfo ri = new ReaderInfo(con);
        Map<String, Object> config = ri.getConfig();
    	
    	if(config.get("mode") != null && config.get("mode").equals("BRM")) {
    		if(brmthreads.get(reader) == null) {
        		StartReaderThread srt = new StartReaderThread(con, env.getProperty("file.output"), env.getProperty("reader.sleep"), RuntimeConfig.init(env));
    		    brmthreads.put(reader, srt.getBrmReadThread());
    		}
    	}

    	if(config.get("mode") != null && config.get("mode").equals("ISO")) {
    		if(brmthreads.get(reader) != null) {
	    		brmthreads.get(reader).setRunning(false);
			    brmthreads.put(reader, null);
    		}
    	}
    	
    	return true;
    }
    
    @RequestMapping(value="/{reader}/power/{value}", method=RequestMethod.GET)
    public boolean setPower(@PathVariable String reader, @PathVariable String value) {
    	FedmConnect con = connections.get(reader);
    	ReaderPower p = new ReaderPower(con);
    	return p.setPower(value);
    }
    
    @RequestMapping(value="/{reader}/validtime/{value}", method=RequestMethod.GET)
    public boolean setValidTime(@PathVariable String reader, @PathVariable String value) {
    	FedmConnect con = connections.get(reader);
    	ReaderValidTime v = new ReaderValidTime(con);
    	return v.setValidTime(value);
    }
    
    @RequestMapping(value="/{reader}/file/{value}", method=RequestMethod.GET)
    public List<String> getFileContent(@PathVariable String reader, @PathVariable String value) {
    	ReaderResultFiles rf = new ReaderResultFiles(env.getProperty("file.output"));
    	return rf.getFileContent(value);
    }
    
    @RequestMapping(value="/{reader}/resetReaderFile", method=RequestMethod.GET)
    public boolean resetReaderFile(@PathVariable String reader) {
    	FileOutput fo = new FileOutput(env.getProperty("file.output"));
    	fo.setHost(reader);
    	return fo.resetReaderFile();
    }
    
    @RequestMapping(value="/{reader}/relais/{value}", method=RequestMethod.GET)
    public boolean setRelais(@PathVariable String reader, @PathVariable String value) {
    	FedmConnect con = connections.get(reader);
		ReaderRelais r = new ReaderRelais(con);
		return r.setNewMode(value);
    }
    
    @RequestMapping(value="/{reader}/write/{value}", method=RequestMethod.GET)
    public Map<String, String> writeTag(@PathVariable String reader, @PathVariable int value) {
    	FedmConnect con = connections.get(reader);
		ReaderWriteTag w = new ReaderWriteTag(con);
		return w.writeTag(value);
    }

	/**
	 * @return Set of all reader connections
	 */
    @RequestMapping(value="/readers", method=RequestMethod.GET)
    public Set<String> getReaders() {
    	return connections.keySet();
    }

	/**
	 * @param value The filename without extension to download. The filename should be fetched via getReaderInfo
	 * @return A copy of the local log file
	 * @throws IOException
	 */
    @RequestMapping(path = "/download/{value}", method = RequestMethod.GET)
    public ResponseEntity<String> download(@PathVariable String value) throws IOException {
    	ReaderResultFiles files = new ReaderResultFiles(env.getProperty("file.output"));
    	List<String> lines = files.getFileContent(value);
		if (lines == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
    	String formattedResponse = String.join("\n", lines);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData(value, value + ".out");
		headers.setContentLength(formattedResponse.length());

        return new ResponseEntity<>(formattedResponse, headers, HttpStatus.OK);
    }

    @RequestMapping("/{reader}/encoding/{value}")
    public void setTagEncoding(@PathVariable String reader, @PathVariable String value){
        SerialNumberEncodingType type = Objects.equals(value.toUpperCase(), "DECIMAL") ? DECIMAL : HEXADECIMAL;
        RuntimeConfig runtimeConfig = brmthreads.get(reader).getRuntimeConfig();
        runtimeConfig.setTagEncodingType(type);
        runtimeConfig.getFilters().removeIf(tagFilter -> tagFilter instanceof EncodingFilter);
        runtimeConfig.getFilters().add(new EncodingFilter(type));
        runtimeConfig.serializeToXML();
    }

    @RequestMapping("/{reader}/snrrange/{lower}-{upper}")
    public void setTagRange(@PathVariable String reader, @PathVariable int lower, @PathVariable int upper){
        BrmReadThread brmReadThread = brmthreads.get(reader);
        brmReadThread.getFilters().removeIf(tagFilter -> tagFilter instanceof SnrRangeFilter);
        brmReadThread.getFilters().add(new SnrRangeFilter(lower, upper, brmReadThread.getEncodingType()));
        brmReadThread.getRuntimeConfig().serializeToXML();
    }
}
