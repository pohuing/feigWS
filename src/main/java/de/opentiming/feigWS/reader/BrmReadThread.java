package de.opentiming.feigWS.reader;

import de.feig.*;
import de.opentiming.feigWS.help.FileOutput;
import de.opentiming.feigWS.help.RuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 *
 * @author Martin Bussmann
 *
 * Thread der den Buffer ausließt und die Inhalte in eine Textdatei schreibt.
 *
 * - Nach einem Reconnect wird die Zeit automatisch neu gesetzt.
 * - Wenn der Reader nicht verbunden werden konnte wird dies alle 5 sec. erneut versucht
 *
 */
public class BrmReadThread implements Runnable {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final FileOutput fo;
	private boolean firstConnect;
	private final FedmConnect con;
	private int sleepTime;
	private final FedmIscReader fedm;
	private int sets = 255;
	private boolean running;
    private final RuntimeConfig runtimeConfig;


	public BrmReadThread(FedmConnect con, String outputDir, RuntimeConfig runtimeConfig) {
		firstConnect = true;
		this.con = con;
		fedm = con.getFedmIscReader();
		fo = new FileOutput(outputDir);
		fo.setHost(con.getHost());
        this.runtimeConfig = runtimeConfig;
	}

	public synchronized void run() {
		try {
			while (isRunning()) {
				con.fedmOpenConnection();
				if (con.isConnected()) {

					if (firstConnect) {
						firstConnect = false;
					}

					fedm.setTableSize(FedmIscReaderConst.BRM_TABLE, 256);
					readBuffer();

				} else {
					firstConnect = true;
					 // 5 sec. warten, wenn kein Reader verbunden werden konnte
					Thread.sleep(5000);
				}

				Thread.sleep(sleepTime);
			}
		} catch (InterruptedException e) {
		} catch (FedmException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Auslesen des Buffers
	 */
	private void readBuffer() {

		if (fedm == null) {
			return;
		}

		FedmIscReaderInfo readerInfo = fedm.getReaderInfo();

		// read data from reader
		// read max. possible no. of data sets: request 255 data sets
		try {
			switch (readerInfo.readerType) {
			default:
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_ADV_BRM_SETS, sets);
				// 0x22 is ReadBuffer
				fedm.sendProtocol((byte) 0x22);

				break;
			}

			FedmBrmTableItem[] brmItems = null;
			log.info("{} Anzahl Tags: {}", con.getHost(), fedm.getTableLength(FedmIscReaderConst.BRM_TABLE));

			if (fedm.getTableLength(FedmIscReaderConst.BRM_TABLE) > 0)
				brmItems = (FedmBrmTableItem[]) fedm.getTable(FedmIscReaderConst.BRM_TABLE);

			if (brmItems != null) {
				String cTime = getComputerTime();
				String csvFileContent = "";

                for (FedmBrmTableItem brmItem : brmItems) {
                    ReaderTag tag;
                    try {
                        tag = new ReaderTag(brmItem, readerInfo, con.getHost(), getAntData(brmItem, "RSSI"), getAntData(brmItem, "NR"), cTime);
                    } catch (Exception e) {
                        continue;
                    }
                    if (!validate(tag, runtimeConfig.getTagEncodingType())) {
                        log.warn("Skipping tag because it failed validation {}", tag);
                        continue;
                    }
                    log.info("{} {} - {} - {} - {}", con.getHost(), tag.serialNumberHex, tag.antNr, tag.rssi, tag.getSerialNumber(runtimeConfig.getTagEncodingType()));
                    csvFileContent += "\n" + tag.formatForCSV(getEncodingType());
                }

				try {
					fo.writeToFile(csvFileContent);
					if (fedm.getLastError() >= 0) {
						clearBuffer(this.fedm);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error("{} reader connection broken",  con.getHost());
		}
	}

    /**
     * Checks if the recorded tag is within filter parameters
     * Currently that means is not of Hexadecimal encoding if Decimal is expected, and that the tag is in the expected range of runners
     * @return true if tag passes all checks, false if not
     */
    private boolean validate(ReaderTag tag, SerialNumberEncodingType encodingType) {
        if(tag == null)
            return false;
        return runtimeConfig.getFilters().stream().allMatch(tagFilter -> tagFilter.validate(tag));
    }

    /**
	 *
	 * Liefert den RSSI Wert und die Antennennummer
	 *
	 */
	private String getAntData(FedmBrmTableItem fedmBrmTableItem, String key) {

		String res = "0";
		byte b = 0;
		try {
			if (fedmBrmTableItem.getIntegerData(FedmIscReaderConst.DATA_ANT_NR) == 0) {
                HashMap<Integer, FedmIscRssiItem> item = fedmBrmTableItem.getRSSI();

				for (int i = 1; i < 5; i++) {
					if (item.get(i) != null) {
						FedmIscRssiItem fedmIscRssiItem = (item.get(i));
						if (key.equals("RSSI")) {
							b = fedmIscRssiItem.RSSI;
						}
						if (key.equals("NR")) {
							b = fedmIscRssiItem.antennaNumber;
						}
						res = b + "";
					}
				}

			} else {
				if (key.equals("NR")) {
					if (fedmBrmTableItem.isDataValid(FedmIscReaderConst.DATA_ANT_NR)) { // ant nr
						res = fedmBrmTableItem.getStringData(FedmIscReaderConst.DATA_ANT_NR);
						res = getDualValue(res);
					}
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res + "";
	}

	/**
	 * Liefert die aktuelle Zeit des Hosts
	 *
	 */
	public String getComputerTime() {
		Date now = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
		return sdf.format(now);
	}

	/**
	 * Liefert die Antennen, an denen ein Tag erkannt wurde im Dual Format
	 *
	 */
	private String getDualValue(String antNr) {
		int dez = Integer.parseInt(antNr, 16);
		return String.format("%4s", Integer.toBinaryString(dez)).replace(" ", "0");
	}

	private void clearBuffer(FedmIscReader fedm) {
		if (fedm == null) {
			return;
		}

		// clear all read data in reader
		try {
			fedm.sendProtocol((byte) 0x32); //0x32 is ClearDataBuffer
		} catch (FedmException | FePortDriverException | FeReaderDriverException e) {
            e.printStackTrace();
        }

    }

	public int getSets() {
		return sets;
	}

	public void setSets(int sets) {
		this.sets = sets;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
	    if(running) { log.info("{} start brmReadThread", con.getHost()); }
	    if(!running) { log.info("{} kill brmReadThread", con.getHost()); }
		this.running = running;
	}

	public void setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
	}

    public List<TagFilter> getFilters(){
        return runtimeConfig.getFilters();
    }

    public RuntimeConfig getRuntimeConfig(){
        return runtimeConfig;
    }

    public SerialNumberEncodingType getEncodingType(){
        return runtimeConfig.getTagEncodingType();
    }
}