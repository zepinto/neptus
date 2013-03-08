/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Jun 19, 2012
 * $Id:: ImcTo837.java 10079 2013-03-08 17:37:35Z jqcorreia                     $:
 */
package pt.up.fe.dceg.neptus.mra.exporters;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.llf.LsfLogSource;

/**
 * Class to extract data from a LogSource and generate Imagenex .837 file from data acquired from Delta T Multibeam 
 * @author jqcorreia
 * 
 */
public class ImcTo837 implements MraExporter {
    DataOutputStream os;
    IMraLog pingLog;
    IMraLog esLog;
    int multiBeamEntityId;
    IMraLogGroup log;
    static int st;
    
    public ImcTo837(IMraLogGroup log) {
        try {
            File outFile = new File(log.getFile("Data.lsf").getParentFile() + "/multibeam.837");
            os = new DataOutputStream(new FileOutputStream(outFile));
            this.log = log;
            pingLog = log.getLog("SonarData");
            esLog = log.getLog("EstimatedState");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public String getName() {
        return "IMC to 837";
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        IMraLog log = source.getLog("SonarData");
        if (log == null)
            return false;
        
        IMCMessage first = log.firstLogEntry();
        IMCMessage msg = first;
        
        while(msg != null) {
            // Wait 2 seconds for the first valid multibeam SonarData message //FIXME
            if(msg.getTimestampMillis() > first.getTimestampMillis() + 2000)
                break;
            if(msg.getLong("type") == SonarData.TYPE.MULTIBEAM.value()) {
                return true;
            }
            msg = log.nextLogEntry();
        }
        return false;
    }

    public void process() {
        IMCMessage pingMsg = pingLog.firstLogEntry();
        
        byte[] buffer;
        byte[] prevBuffer;
        byte[] zeroFill = new byte[236];
        prevBuffer = new byte[16000];
        short pitch;
        short roll;
        short heading;
        String lat = "";
        String lon = "";
        String sTime = "";
        String sMillis = "";
        double res[] = new double[2];
        
        Calendar cal = Calendar.getInstance();
        new LocationType();

        // Build zeroFill padding
        for(int i = 0; i < zeroFill.length; i++) {
            zeroFill[i] = 0;
        }
        
        try {
            while (pingMsg != null) {
                
                // Check for Sidescan message and multibeam entity 
                if(pingMsg.getInteger("type") == SonarData.TYPE.MULTIBEAM.value()) {
                        
                    IMCMessage esMsg = esLog.getEntryAtOrAfter(pingLog.currentTimeMillis());
                    if(esMsg == null) {
                        roll = 900;
                        pitch = 900;
                        heading = 900;
                    }
                    else {
                        roll = -5; //(short) (Math.toDegrees(esMsg.getDouble("phi")));
                        pitch = (short) (Math.toDegrees(esMsg.getDouble("theta")));
                        heading = (short) (Math.toDegrees(esMsg.getDouble("psi")));
                        res = CoordinateUtil.latLonAddNE2(Math.toDegrees(esMsg.getDouble("lat")), Math.toDegrees(esMsg.getDouble("lon")), esMsg.getDouble("x"), esMsg.getDouble("y"));
                        
                        int d = (int)res[0];
                        double m = ((res[0] - d) * 60);
                        lat = String.format(" %02d.%.5f",Math.abs(d),Math.abs(m)) + (d > 0 ? " N" : " S");
                        d = (int)res[1];
                        m = ((res[1] - d) * 60);
                        lon = String.format("%03d.%.5f",Math.abs(d),Math.abs(m)) + (d > 0 ? " E" : " W");
//                        
                        System.out.println(lat);
                        System.out.println(lon);
                        
//                        if(heading < 0)
//                            heading = (short) (360 + heading);
                        
                    }
                    long timestamp = esLog.currentTimeMillis();
                    cal.setTimeInMillis(timestamp);
                    
                    String month="";
                    
                    switch(cal.get(Calendar.MONTH)) 
                    {
                        case 0: month = "JAN"; break;
                        case 1: month = "FEB"; break;
                        case 2: month = "MAR"; break;
                        case 3: month = "APR"; break;
                        case 4: month = "MAY"; break;
                        case 5: month = "JUN"; break;
                        case 6: month = "JUL"; break;
                        case 7: month = "AUG"; break;
                        case 8: month = "SEP"; break;
                        case 9: month = "OCT"; break;
                        case 10: month = "NOV"; break;
                        case 11: month = "DEC"; break;
                    }
                    sTime = String.format("%02d-%s-%d\0%02d:%02d:%02d\0.00\0", 
                            cal.get(Calendar.DAY_OF_MONTH), month, cal.get(Calendar.YEAR), 
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
                    
                    sMillis = String.format(".%03d\0", cal.get(Calendar.MILLISECOND));
                    
                    
                    buffer = new byte[pingMsg.getRawData("data").length];
                    // Header
                    os.write("837".getBytes());
                    os.writeByte((buffer.length == 8000 ? 10 : 11)); // 10 = 8000(IUX), 11 = 16000(IVX)
                    os.writeShort(buffer.length == 8000 ? 8192 : 16384); // Number of total bytes to read
                    os.writeShort(buffer.length + 13); // Number of bytes only for data points
                    os.write(sTime.getBytes()); // TIMESTAMP
                    os.writeInt(0);
                    os.writeByte(0x83); // 11000011 = 1 Reserved, 1 Xcdr Up, 000 Reserved and 011 Profile
                    os.writeByte(1); // Start gain
                    os.write(new byte[] { 0, 0 }); // Tilt Angle
                    os.write(new byte[] { 0, 0, 7 } ); // Reserved, Reserved, Pings Averaged
                    os.writeByte(18); // Pulse lenght in us/10
                    os.writeByte(0); // User defined byte
                    os.writeShort(0); // sound speed short ( 0 = 1500ms )
                    os.write(lat.getBytes()); // Lat and Lon NMEA style
                    os.write(lon.getBytes()); 
                    os.writeByte(0); // Speed
                    os.writeShort(0); // Course 
                    os.writeByte(0); // and a Reserved byte as 0
                    os.writeShort(260); // 260Hz operating frequency
                    os.writeShort((pitch*10+900)+32768); // Pitch = 0
                    os.writeShort((roll*10+900)+32768); // Roll
                    
                    os.writeShort(0x8000); // Heading = 0
                    os.writeShort(97); // Repetition rate in ms
                    os.writeByte(50);
                    os.writeShort(0); // 2 reserved bytes 0
                    os.write(sMillis.getBytes());
                    os.writeShort(0);
                    
                    // Sonar return header
                    os.write(buffer.length == 8000 ? "IUX".getBytes() : "IVX".getBytes());
                    os.write(new byte[] { 
                            16,     // Head ID
                            0,      // Serial status
                            7,      // Packet Number
                            36,     // Version
                            (byte)pingMsg.getInteger("max_range"),     // Range
                            0,      // reserved
                            0,      // reserved
                            //3,
                            //-24
                         });
                    os.writeShort(buffer.length); // data bytes

                    System.arraycopy(pingMsg.getRawData("data"), 0, buffer, 0, buffer.length);
                    
                    // Echo values
                    os.write(buffer);
                    os.writeByte(0xFC); // Trailing value always 0xFC
                    
                    // Exta bytes and zero-fill
                    os.writeFloat(0); // Offset X
                    os.writeFloat(0); // Offset Y
                    os.writeFloat(0); // Offset Z
                    os.writeByte(1); // Sensor type (?)
                    os.writeShort(pitch); // Pitch
                    os.writeShort(roll); // Roll   
                    os.writeShort(heading); // Heading
                    os.writeShort(0); // Timer Ticks
                    os.writeShort(0); // Azimuth Head Position
                    os.writeByte(1); // Azimuth Up/Down
                    os.writeFloat(0); // Heave
                    os.write(new byte[] { 0,0,0,0,0,0,0 }); // 7 reserved bytes 
                    if (buffer.length == 8000)
                        os.write(zeroFill, 0, 44);  // in case we have only 8000 bytes                  
                    else
                        os.write(zeroFill);
                    System.arraycopy(buffer, 0, prevBuffer, 0, buffer.length);
                } 
                pingMsg = pingLog.nextLogEntry();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("end");
    }
    
//    public static void main(String args[]) throws Exception {
//        while(true) {
//            new ImcTo837(new LsfLogSource(
//                "/home/jqcorreia/lsts/logs/lauv-noptilus-1/20121220/160655_rows_btrack/Data.lsf",null));
//            try {
//                Thread.sleep(1000);
//                st+=10;
//                System.out.println(st);
//                break;
//            }
//            catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

}
