package de.opentiming.feigWS.reader;

import de.feig.FeHexConvert;
import de.feig.FedmBrmTableItem;
import de.feig.FedmIscReaderConst;
import de.feig.FedmIscReaderInfo;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReaderTag {
    public final String serialNumberHex;
    public final String uniqeNumber;
    public final String data;
    public final LocalDateTime dateTime;
    public final String type;
    public final String antNr;
    public final String rssi;
    public final String host;
    public final String cTime;


    public ReaderTag(FedmBrmTableItem fedmBrmTableItem, FedmIscReaderInfo readerInfo, String host, String rssi, String antNr, String cTime){
        if (fedmBrmTableItem.isDataValid(FedmIscReaderConst.DATA_SNR)) {

            if (fedmBrmTableItem.isDataValid(FedmIscReaderConst.DATA_SNR)) {
                String sNH = fedmBrmTableItem.getStringData(FedmIscReaderConst.DATA_SNR);

                // zu kurze Seriennummern werden abgefangen
                while (sNH.length() < 8) {
                    sNH = "0" + sNH;
                }

                if (sNH.length() > 8) {
                    sNH = sNH.substring(0, 8);
                }

                serialNumberHex = sNH;
                uniqeNumber = serialNumberHex.substring(0, serialNumberHex.length() - 4);
            }else{
                serialNumberHex = null;
                uniqeNumber = null;
            }

            if (fedmBrmTableItem.isDataValid(FedmIscReaderConst.DATA_RxDB)) { // data
                // block
                byte[] b = fedmBrmTableItem.getByteArrayData(FedmIscReaderConst.DATA_RxDB,
                        fedmBrmTableItem.getBlockAddress(), fedmBrmTableItem.getBlockCount());
                data = FeHexConvert.byteArrayToHexString(b);
                System.out.println("DATA_RxDB: " + FeHexConvert.byteArrayToHexString(b));
            }else
                data = null;

            if (fedmBrmTableItem.isDataValid(FedmIscReaderConst.DATA_TRTYPE)) { // tranponder
                // type
                type = fedmBrmTableItem.getStringData(FedmIscReaderConst.DATA_TRTYPE);
                // System.out.println("DATA_TRTYPE: "+
                // brmItems[i].getStringData(FedmIscReaderConst.DATA_TRTYPE));
            }else
                type = null;

            this.rssi = rssi;
            this.antNr = antNr;

            if (fedmBrmTableItem.isDataValid(FedmIscReaderConst.DATA_TIMER)) { // Timer
                int iYear;
                int iMonth;
                int iDay;
                int iHour = fedmBrmTableItem.getReaderTime().getHour();
                int iMinute = fedmBrmTableItem.getReaderTime().getMinute();
                int iSecond = fedmBrmTableItem.getReaderTime().getMilliSecond() / 1000;
                int iMillisecond = fedmBrmTableItem.getReaderTime().getMilliSecond() % 1000;

                switch (readerInfo.readerType) {
                    case de.feig.FedmIscReaderConst.TYPE_ISCLRU1002:
                        String strDate = ReaderTime.getComputerDate();
                        iYear = Integer.parseInt(strDate.split("-")[0]);
                        iMonth = Integer.parseInt(strDate.split("-")[1]);
                        iDay = Integer.parseInt(strDate.split("-")[2]);
                        break;
                    default:
                        iYear = fedmBrmTableItem.getReaderTime().getYear();
                        iMonth = fedmBrmTableItem.getReaderTime().getMonth();
                        iDay = fedmBrmTableItem.getReaderTime().getDay();
                        break;
                }
                dateTime = LocalDateTime.of(iYear, iMonth, iDay, iHour, iMinute, iSecond, iMillisecond*1_000_000);
            }else{
                dateTime = null;
            }

            this.host = host;
            this.cTime = cTime;
            return;
        }
        throw new Error("FedmBrmTableItem is invalid");
    }

    /**
     * Returns the parsed serial number
     * @param encodingType the base in which to parse
     * @return -1 if the serial number could not be parsed(probably due to wrong encodingType)
     */
    public int getSerialNumber(SerialNumberEncodingType encodingType) {
        int result = -1;
        String sNSubstring = serialNumberHex.substring(serialNumberHex.length() - 4);
        if (encodingType == SerialNumberEncodingType.HEXADECIMAL) {
            result = Integer.parseInt(sNSubstring, 16);
        } else if (sNContainsCharacters()) {
            LoggerFactory.getLogger(this.getClass()).warn("Encountered hexadecimal tag but expected decimal, encountered value: {}", sNSubstring);
        } else {
            result = Integer.parseInt(sNSubstring, 10);
        }
        return result;
    }

    /**
     * Checks if the serialNumber(last four characters of serialNumberHex) contains any characters, e.g. if it is hexadecimal or decimal
     * @return true if serialNumber contains characters
     */
    public boolean sNContainsCharacters(){
        String sNSubstring = serialNumberHex.substring(serialNumberHex.length() - 4);
        return !sNSubstring.chars().allMatch(value -> value >= '0' && value <='9');
    }

    public String formatForCSV(SerialNumberEncodingType encodingType){
        return getSerialNumber(encodingType) + ";" + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ";"
                + dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss;SSS")) + ";" + host + ";" + antNr
                + ";" + rssi + ";" + uniqeNumber + ";" + cTime;
    }

    @Override
    public String toString() {
        return "ReaderTag{" +
                "serialNumberHex='" + serialNumberHex + '\'' +
                ", uniqeNumber='" + uniqeNumber + '\'' +
                ", data='" + data + '\'' +
                ", dateTime=" + dateTime +
                ", type='" + type + '\'' +
                ", antNr='" + antNr + '\'' +
                ", rssi='" + rssi + '\'' +
                ", host='" + host + '\'' +
                ", cTime='" + cTime + '\'' +
                '}';
    }
}
