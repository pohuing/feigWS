package de.opentiming.feigWS.reader;
import java.util.Date;

import de.feig.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReaderTime {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private FedmIscReader fedm;
	private FedmConnect con;

	/**
	 * Sets the reader time to the current system time
	 * @return the updated reader time
	 */
	public synchronized String setTime() {
		
		String time = getComputerTime();
		String timeArr[] = time.split(":");
		String returnString = "";
		
		// Durch probleme beim setzen der Zeit wenn die Sekunde kleiner 10 ist
		// wird gewartet bis die Sekunden auf jeden Fall zweistellig sind
		if(timeArr[2].startsWith("0")) {
			try {
				log.info("{} sorry, warte 10 sec, bis die Sekunde zweistellig ist.");
				Thread.sleep(10000);
				time = getComputerTime();
				String timeArr2[] = time.split(":");
				for (int i = 0; i < timeArr2.length; i++) {
					timeArr[i] = timeArr2[i];
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		String date = getComputerDate();
		String dateArr[] = date.split("-");

//		String rTime = getReaderTime();
		
//		System.out.println("Computer Date:   " + date);
//		System.out.println("Computer Time:   " + time);
//		System.out.println("Reader Time:     " + rTime);
		

		byte century = Byte.parseByte(dateArr[0].substring(0,2));
		byte year    = Byte.parseByte(dateArr[0].substring(2,4));
		//System.out.println((int)Byte.parseByte(timeArr[2]) * 1000);
		//System.out.println((byte)Byte.parseByte(timeArr[2]) * 1000);
		
		// Set Time
		try {
			
			FedmIscReaderInfo readerInfo = fedm.getReaderInfo();
			if (readerInfo.readerType == FedmIscReaderConst.TYPE_ISCLRU1002) {
				fedm.setData(FedmIscReaderID.FEDM_ISCLR_TMP_TIME_H, (byte) Byte.parseByte(timeArr[0]));
				fedm.setData(FedmIscReaderID.FEDM_ISCLR_TMP_TIME_M, (byte) Byte.parseByte(timeArr[1]));
				fedm.setData(FedmIscReaderID.FEDM_ISCLR_TMP_TIME_MS, (int) Byte.parseByte(timeArr[2]) * 1000 + 500);
				fedm.sendProtocol((byte) 0x85);
			} else {
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_CENTURY, (byte) century); // 20. Jahrhundert
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_YEAR, (byte) year); // Jahr 04 im Jahrhundert
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MONTH, (byte) Byte.parseByte(dateArr[1])); // September
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_DAY, (byte) Byte.parseByte(dateArr[2])); // 15. September
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_TIMEZONE, (byte) 0); // z.Zt. ungenutzt
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_HOUR, (byte) Byte.parseByte(timeArr[0])); // Stunden
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MINUTE, (byte) Byte.parseByte(timeArr[1])); // Minuten
				fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MILLISECOND, (int) Byte.parseByte(timeArr[2]) * 1000 + 500); // Millisekunden (inkl. Sekunden)
				fedm.sendProtocol((byte) 0x87);    // Datum und Uhrzeit setzen
			}
									
		} catch (FePortDriverException | FeReaderDriverException | FedmException e) {
			e.printStackTrace();
		}

		String nrTime = getReaderDateTime();
		log.info("{} set reader Time {}", con.getHost(), nrTime.substring(0, 19));
		returnString = nrTime.substring(0, 19);
		
		return returnString;

	}

	public String getReaderTime() {
		try
		{
			fedm.sendProtocol((byte)0x88);
		} catch (FePortDriverException | FeReaderDriverException | FedmException e) {
			e.printStackTrace();
		}

		int stunde = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_HOUR);
		String minute = String.format("%02d", fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MINUTE));
		String millisekunde = String.format("%05d", fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MILLISECOND));

		return stunde + ":" + minute + ":" + millisekunde;
	}

	/**
	 * @return Returns the current reader time and date in yyyy-MM-dd hh:mm:SSSSS(that's 5 digit milliseconds)
	 */
	public String getReaderDateTime() {

		int century = 0;
		int year    = 0;
		int month   = 0;
		int day     = 0;
		int std     = 0;
		int min     = 0;
		int milli     = 0;
		String date = "";
		
		try
		{
			FedmIscReaderInfo readerInfo = fedm.getReaderInfo();
			if (readerInfo.readerType == FedmIscReaderConst.TYPE_ISCLRU1002) {
				fedm.sendProtocol((byte) 0x86);
				std = fedm.getIntegerData(FedmIscReaderID.FEDM_ISCLR_TMP_TIME_H);
				min = fedm.getIntegerData(FedmIscReaderID.FEDM_ISCLR_TMP_TIME_M);
				milli = fedm.getIntegerData(FedmIscReaderID.FEDM_ISCLR_TMP_TIME_MS);
				date = getComputerDate();
			} else {
				fedm.sendProtocol((byte) 0x88);
				century = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_CENTURY);
				year = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_YEAR);
				month = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MONTH);
				day = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_DAY);
				std = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_HOUR);
				min = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MINUTE);
				milli = fedm.getIntegerData(FedmIscReaderID.FEDM_ISC_TMP_DATE_MILLISECOND);
				date = century + "" + year + "-" + month + "-" + day;
			}
			
		} catch (FePortDriverException | FeReaderDriverException | FedmException e) {
			e.printStackTrace();
		}
		
		String stunde = String.format("%02d", std);
		String minute = String.format("%02d", min);
		String millisekunde = String.format("%05d", milli);

		// TODO: 02.08.2021 Since every call of getReaderTime truncates the milliseconds to seconds, maybe we should
		//  just only return seconds
		return date + " " + stunde + ":" + minute + ":" + millisekunde;
	}

	/**
	 * @return The current system date in yyyy-MM-dd
	 */
	public static String getComputerDate() {
		Date now = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(now);
	}

	/**
	 * @return The current system time in HH:mm:ss
	 */
	public String getComputerTime() {
		Date now = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
		return sdf.format(now);
	}
	
    public void setReaderCon(FedmConnect con) {
    	this.con = con;
		this.fedm = con.getFedmIscReader();
    }

    public void setFedmIscReader(FedmIscReader fedm) {
		this.fedm = fedm;
    }
}
