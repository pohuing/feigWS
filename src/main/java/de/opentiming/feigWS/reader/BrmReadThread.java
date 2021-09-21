package de.opentiming.feigWS.reader;

import de.feig.*;
import de.opentiming.feigWS.help.FileOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;


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
	public SerialNumberEncodingType encodingType;


	public BrmReadThread(FedmConnect con, String outputDir, SerialNumberEncodingType serialNumberEncodingType) {
		firstConnect = true;
		this.con = con;
		fedm = con.getFedmIscReader();
		fo = new FileOutput(outputDir);
		fo.setHost(con.getHost());
		this.encodingType = serialNumberEncodingType;
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
			// TODO Auto-generated catch block
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
                // TODO: 11.08.2021 Remove these arrays if no further formatting issues show up 
				String[] serialNumberHex = new String[brmItems.length];
				// String[] serialNumber = new String[brmItems.length];
				int[] serialNumber = new int[brmItems.length];
				String[] uniqeNumber = new String[brmItems.length];
				String[] data = new String[brmItems.length];
				String[] date = new String[brmItems.length];
				String[] time = new String[brmItems.length];
				String[] type = new String[brmItems.length];
				String[] antNr = new String[brmItems.length];
				String[] rssi = new String[brmItems.length];

				String cTime = getComputerTime();
				String csvFileContent = "";

				for (int i = 0; i < brmItems.length; i++) {
                    String oldNewline = formatTag(readerInfo, brmItems, serialNumberHex, serialNumber, uniqeNumber, data, date, time, type, antNr, rssi, cTime, i);
                    ReaderTag tag;
                    try {
                        tag = new ReaderTag(brmItems[i], readerInfo, con.getHost(), getAntData(brmItems[i], "RSSI"), getAntData(brmItems[i], "NR"), cTime);
                    }catch (Exception e){
                        // TODO: 14.08.2021 Change to Option because ReaderTag construction may fail 
                        continue;
                    }
                    if(!validate(tag, encodingType)){
                        log.warn("Skipping tag because it failed validation {}", tag);
                        continue;
                    }
                    // TODO: 11.08.2021 Remove these checks and fallbacks if no further issues show up
				    if (oldNewline.equals(tag.formatForCSV(encodingType)))
					    csvFileContent = csvFileContent + "\n" + tag.formatForCSV(encodingType);
                    else{
                        log.error("tag formatting is not equal to old formatter old: {} \n new:{}", oldNewline, tag.formatForCSV(encodingType));
                        csvFileContent = csvFileContent + "\n" + oldNewline;
                    }
					log.info("{} " + serialNumberHex[i] + " - " + antNr[i] + " - " + rssi[i] + " - " + serialNumber[i], con.getHost());
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
     * // TODO: 11.08.2021 Replace with a run time changeable filtering system
     * @return true if tag passes all checks, false if not
     */
    private boolean validate(ReaderTag tag, SerialNumberEncodingType encodingType) {
        if(tag == null)
            return false;
	    int LOWEST_START_NUMBER = 0;
	    int HIGHEST_START_NUMBER = 150;
	    // Decimal Tags may not contain Characters
        if (encodingType == SerialNumberEncodingType.DECIMAL && tag.sNContainsCharacters())
            return false;
        if (tag.getSerialNumber(encodingType) > HIGHEST_START_NUMBER)
            return false;
        if (tag.getSerialNumber(encodingType) < LOWEST_START_NUMBER)
            return false;

        return true;
    }

    /**
     * I'm not entirely convinced my refactored formatting does exactly the same thing as the prior version so this will stay for a bit
     * TODO: 11.08.2021 remove if no related issues show up
     */
    private String formatTag(FedmIscReaderInfo readerInfo, FedmBrmTableItem[] brmItems, String[] serialNumberHex, int[] serialNumber, String[] uniqeNumber, String[] data, String[] date, String[] time, String[] type, String[] antNr, String[] rssi, String cTime, int i) {
        if (brmItems[i].isDataValid(FedmIscReaderConst.DATA_SNR)) {
            serialNumberHex[i] = brmItems[i].getStringData(FedmIscReaderConst.DATA_SNR);

            // zu kurze Seriennummern werden abgefangen
            while (serialNumberHex[i].length() < 8) {
                serialNumberHex[i] = "0" + serialNumberHex[i];
            }

            if (serialNumberHex[i].length() > 8) {
                serialNumberHex[i] = serialNumberHex[i].substring(0, 8);
            }

            // serialNumber[i] =
            // serialNumberHex[i].substring(serialNumberHex[i].length()-4,
            // serialNumberHex[i].length());
            String sNSubstring = serialNumberHex[i].substring(serialNumberHex[i].length() - 4);
            if (encodingType == SerialNumberEncodingType.HEXADECIMAL) {
                serialNumber[i] = Integer.parseInt(sNSubstring, 16);
            }
            else{
                if(sNSubstring.chars().allMatch(value -> value >= '0' && value <='9'))
                    serialNumber[i] = Integer.parseInt(sNSubstring, 10);
                else{
                    log.warn("Encountered hexadecimal tag but expected decimal, encountered value: {}", sNSubstring);
                }
            }

            uniqeNumber[i] = serialNumberHex[i].substring(0, serialNumberHex[i].length() - 4);

        }

        if (brmItems[i].isDataValid(FedmIscReaderConst.DATA_RxDB)) { // data
            // block
            byte[] b = brmItems[i].getByteArrayData(FedmIscReaderConst.DATA_RxDB,
                    brmItems[i].getBlockAddress(), brmItems[i].getBlockCount());
            data[i] = FeHexConvert.byteArrayToHexString(b);
            System.out.println("DATA_RxDB: " + FeHexConvert.byteArrayToHexString(b));
        }

        if (brmItems[i].isDataValid(FedmIscReaderConst.DATA_TRTYPE)) { // tranponder
            // type
            type[i] = brmItems[i].getStringData(FedmIscReaderConst.DATA_TRTYPE);
            // System.out.println("DATA_TRTYPE: "+
            // brmItems[i].getStringData(FedmIscReaderConst.DATA_TRTYPE));
        }

        rssi[i] = getAntData(brmItems[i], "RSSI");
        antNr[i] = getAntData(brmItems[i], "NR");

        if (brmItems[i].isDataValid(FedmIscReaderConst.DATA_TIMER)) { // Timer

            switch (readerInfo.readerType) {
                case FedmIscReaderConst.TYPE_ISCLRU1002:
                    date[i] = ReaderTime.getComputerDate();
                    break;
                default:
                    date[i] = brmItems[i].getReaderTime().getYear()
                            + "-" + brmItems[i].getReaderTime().getMonth()
                            + "-" + brmItems[i].getReaderTime().getDay();
                    break;
            }
            String hour = String.format("%02d", brmItems[i].getReaderTime().getHour());
            String minute = String.format("%02d", brmItems[i].getReaderTime().getMinute());
            String second = String.format("%02d", brmItems[i].getReaderTime().getMilliSecond() / 1000);
            String millisecond = String.format("%03d", brmItems[i].getReaderTime().getMilliSecond() % 1000);

            time[i] = hour + ":" + minute + ":" + second + "." + millisecond;
        }

        return serialNumber[i] + ";" + date[i] + ";"
                + time[i].substring(0, 8) + ";" + time[i].substring(9, 12) + ";" + con.getHost() + ";" + antNr[i]
                + ";" + rssi[i] + ";" + uniqeNumber[i] + ";" + cTime;
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
	 * Liefert die Antennn an denen ein Tag erkannt wurde im Dual Format
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

}