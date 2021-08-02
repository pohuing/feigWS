package de.opentiming.feigWS;

import java.util.Map;

import javax.annotation.Resource;

import de.opentiming.feigWS.reader.SerialNumberEncodingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import de.opentiming.feigWS.reader.FedmConnect;
import de.opentiming.feigWS.help.FileOutput;
import de.opentiming.feigWS.help.StartReaderThread;
import de.opentiming.feigWS.reader.BrmReadThread;

@Component
public class FeigWsApplicationRunner implements ApplicationRunner {

	@Resource(name = "connections")
	private Map<String, FedmConnect> connections;

	@Resource(name = "brmthreads")
	private Map<String, BrmReadThread> brmthreads;
	
	@Autowired
	private Environment env;
	
	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		String[] readers = env.getProperty("reader.ip").split(" ");
		SerialNumberEncodingType encodingType;
		// Default to hexadecimal tag encoding if no tag.sNFormatting application property is set
		if (env.getProperty("tag.sNFormatting") == null){
			System.out.println("tag.sNFormatting is not defined in application.properties. Defaulting to Hexadecimal");
			encodingType = SerialNumberEncodingType.HEXADECIMAL;
		}else
			encodingType = env.getProperty("tag.sNFormatting").equals("Hex") ? SerialNumberEncodingType.HEXADECIMAL : SerialNumberEncodingType.DECIMAL;


		for( String reader : readers) {
			
			/*
			 * Roll output file
			 */
			FileOutput fo = new FileOutput(env.getProperty("file.output"));
			fo.setHost(reader);
			fo.resetReaderFile();
			
			/*
			 * Configure connection to reader and store in global bean resource
			 */
			FedmConnect con = new FedmConnect();
			con.logReaderProtocol(Boolean.valueOf(env.getProperty("reader.protocol")));
			con.setHost(reader);
			con.setPort(Integer.parseInt(env.getProperty("reader.port")));
			con.fedmOpenConnection();
			connections.put(reader, con);
			
			/*
			 * Start Reader thread and store in global bean resource
			 */			
			StartReaderThread srt = new StartReaderThread(con, env.getProperty("file.output"), env.getProperty("reader.sleep"), encodingType);
		    brmthreads.put(reader, srt.getBrmReadThread());
		}
	}

}