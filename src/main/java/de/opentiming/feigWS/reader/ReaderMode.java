package de.opentiming.feigWS.reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.feig.FePortDriverException;
import de.feig.FeReaderDriverException;
import de.feig.FedmException;
import de.feig.FedmIscReader;
import de.feig.FedmIscReaderID;


public class ReaderMode {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private FedmIscReader fedm;
	private FedmConnect con;

	public ReaderMode(FedmConnect con) {
		this.con = con;
		this.fedm = con.getFedmIscReader();
	}

	/**
	 * Sets reader mode to ISO or BRM, defaulting to BRM on invalid input
	 * @param mode BRM or ISO though anything that isn't BRM defaults to ISO
	 * @return
	 */
	public synchronized boolean setMode(String mode) {
		
		byte modeAdr = 1;
		int intMode;
		
		if(mode.equals("BRM")) { intMode = 128; } else { intMode = 0; }		
		
		try {
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_READ_CFG, (byte)0);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_READ_CFG_LOC, true); // aus dem EPROM lesen
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_READ_CFG_ADR, modeAdr);
			fedm.sendProtocol((byte)0x80);
			
			// schreiben
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_WRITE_CFG, (byte)0);
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_WRITE_CFG_LOC, true); // aus dem EPROM lesen
			fedm.setData(FedmIscReaderID.FEDM_ISC_TMP_WRITE_CFG_ADR, modeAdr);
			fedm.setConfigPara(de.feig.ReaderConfig.OperatingMode.Mode, intMode, true);			
			
			fedm.sendProtocol((byte)0x81);
			fedm.sendProtocol((byte)0x63);
			
			log.info("{} setting {} mode",  con.getHost(), mode);
		} catch (FePortDriverException | FeReaderDriverException | FedmException e) {
			log.error("{} reader connection broken",  con.getHost());
		}

		return true;
	}
	
    public void setFedmIscReader(FedmIscReader fedm) {
        this.fedm = fedm;
    }
	
}
