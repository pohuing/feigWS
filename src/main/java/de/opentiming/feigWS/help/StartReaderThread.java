package de.opentiming.feigWS.help;

import de.opentiming.feigWS.reader.BrmReadThread;
import de.opentiming.feigWS.reader.FedmConnect;
import de.opentiming.feigWS.reader.SerialNumberEncodingType;

public class StartReaderThread {

	private BrmReadThread brmReadThread;
	
	public StartReaderThread(FedmConnect con, String file, String sleep, SerialNumberEncodingType serialNumberEncodingType) {
		brmReadThread = new BrmReadThread(con, file, serialNumberEncodingType);
	    brmReadThread.setSleepTime(Integer.parseInt(sleep));
	    brmReadThread.setSets(10);
	    Thread runner = new Thread(brmReadThread);
	    brmReadThread.setRunning(true);
	    runner.start();
	}

	public BrmReadThread getBrmReadThread() {
		return brmReadThread;
	}

}
