package de.opentiming.feigWS;

import de.opentiming.feigWS.help.FileOutput;
import de.opentiming.feigWS.help.RuntimeConfig;
import de.opentiming.feigWS.help.StartReaderThread;
import de.opentiming.feigWS.reader.BrmReadThread;
import de.opentiming.feigWS.reader.FedmConnect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

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
        RuntimeConfig runtimeConfig = RuntimeConfig.init(env);

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
			StartReaderThread srt = new StartReaderThread(con, env.getProperty("file.output"), env.getProperty("reader.sleep"), runtimeConfig);
		    brmthreads.put(reader, srt.getBrmReadThread());
		}
	}
}