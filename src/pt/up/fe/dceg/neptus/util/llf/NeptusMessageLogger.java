/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: NeptusMessageLogger.java 9616 2012-12-30 23:23:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.util.llf;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;
import pt.up.fe.dceg.neptus.util.conf.PreferencesListener;


/**
 * This class allows the logging of messages as LSF.
 * @author Paulo Dias
 *
 */
public class NeptusMessageLogger {

  private static boolean logEnabled = true;    

  private static PreferencesListener gplistener = new PreferencesListener() {
      public void preferencesUpdated() {
          try {
              boolean oldVal = logEnabled;
//              logEnabled = GeneralPreferences.getPropertyBoolean(GeneralPreferences.LLF_LOGGING_ENABLED);
              logEnabled = GeneralPreferences.messageLogSentMessages || GeneralPreferences.messageLogReceivedMessages;
              if (oldVal && !logEnabled) {
//                  defaultLogger.setLoggingEnabled(false);
//                  defaultLogger.cleanup();
                  NeptusLog.pub().info(NeptusMessageLogger.class.getSimpleName() + " : Logging stopped.");
              }
              if (!oldVal && logEnabled) {
                  changeLog();
                  NeptusLog.pub().info(NeptusMessageLogger.class.getSimpleName() + " : Logging started.");                  
              }
          }
          catch (Exception e) {
              NeptusLog.pub().error(this, e);
          }                     
      }
  };

  static {
      GeneralPreferences.addPreferencesListener(gplistener);
      gplistener.preferencesUpdated();
  }

//  public NeptusMessageLogger() {
//      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//          public void run() {
//              cleanup();
//          }
//      }, NeptusMessageLogger.class.getSimpleName() + " Shutdown Hook"));
//  }

    public static boolean changeLog() {
        return LsfMessageLogger.changeLogSingleton();
    }
      
    public static String getLogDir() {
        return LsfMessageLogger.getLogDirSingleton();
    }

    /**
     * @param message
     */
    public static void logMessage(IMCMessage message) {
        LsfMessageLogger.log(message);
    }

}
//
//
//    private static String sep = "/";// System.getProperty("file.separator");
//    private static DateFormat day = new SimpleDateFormat("yyyyMMdd");
//    private static DateFormat timeOfDay = new SimpleDateFormat("HHmmss");
//
//    private Date startDate = new Date();
//    private String logName = "messages";
//
//    private LinkedHashMap<String, String> logDirs = new LinkedHashMap<String, String>();
//    private LinkedHashMap<String, LLFMessageLogger> loggers = new LinkedHashMap<String, LLFMessageLogger>();
//
//    private String LOG_DIR = "log";
//
//    private static NeptusMessageLogger defaultLogger;
//
//    public static NeptusMessageLogger getLogger() {
//        return defaultLogger;
//    }
//
//    public void setLogName(String logName) {
//        boolean prev = isLoggingEnabled();
//        setLoggingEnabled(false);
//        this.logName = logName;
//        resetLogs();
//        setLoggingEnabled(prev);
//    }
//
//    public void changeLog() {
//        boolean prev = isLoggingEnabled();
//        setLoggingEnabled(false);
//        startDate = new Date();
//        resetLogs();
//        setLoggingEnabled(prev);
//    }
//
//    public void setStartTime(long startTimeMillis) {
//        boolean prev = isLoggingEnabled();
//        setLoggingEnabled(false);
//        startDate = new Date(startTimeMillis);
//        resetLogs();
//        setLoggingEnabled(prev);
//    }
//
//    private void resetLogs() {
//        boolean prev = isLoggingEnabled();
//        setLoggingEnabled(false);
//        logDirs = new LinkedHashMap<String, String>();
//        cleanup();
//        setLoggingEnabled(prev);		
//    }
//
//    private static boolean LOG_ENABLED = true;	
//
//    private static PreferencesListener gplistener = new PreferencesListener() {
//        public void preferencesUpdated() {
//            try {
//                boolean oldVal = LOG_ENABLED;
//                LOG_ENABLED = GeneralPreferences.getPropertyBoolean(GeneralPreferences.LLF_LOGGING_ENABLED);
//                if (oldVal && !LOG_ENABLED) {
//                    defaultLogger.setLoggingEnabled(false);
////                    defaultLogger.cleanup();
//                    NeptusLog.pub().info("LLFMessageLogger : Logging stopped.");
//                }
//                if (!oldVal && LOG_ENABLED) {
//                    defaultLogger.setLoggingEnabled(true);
//                    defaultLogger.resetLogs();
//                    NeptusLog.pub().info("LLFMessageLogger : Logging started.");					
//                }
//            }
//            catch (Exception e) {
//                NeptusLog.pub().error(this, e);
//            }						
//        }
//    };
//
//    static {
//        defaultLogger = new NeptusMessageLogger();
//        GeneralPreferencesPropertiesProvider.addPreferencesListener(gplistener);
//        gplistener.preferencesUpdated();
//    }
//
//    public NeptusMessageLogger() {
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run() {
//                cleanup();
//            }
//        }, "LLFLogger Shutdown Hook"));
//
//        resetLogs();
//    }
//
//    public NeptusMessageLogger(String logName, long startTimeInMillis) {
//        setStartTime(startTimeInMillis);
//        setLogName(logName);
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run() {
//                cleanup();
//            }
//        },"LLFLogger Shutdown Hook"));
//        resetLogs();
//    }
//
//
//
//    public void logMessage(IMessage message) throws IOException {
//        logMessage("messages", "unknown", "unknown", System.currentTimeMillis(), message);
//    }
//
//    public void logMessage(String src, String dst, IMessage message) throws IOException {
//
//        Object src_ent = message.getHeaderValue("src_ent");
//        Object dst_ent = message.getHeaderValue("dst_ent");
//
//        if (src_ent != null && dst_ent != null)
//            logMessage("messages", src, dst, src_ent.toString(), dst_ent.toString(), System.currentTimeMillis(), message);
//        else
//            logMessage("messages", src, dst, System.currentTimeMillis(), message);
//    }
//
//
//    public void logMessageToDir(String dir, String src, String dst, long timeMilis, IMessage message) throws IOException {
//        logMessage(dir, src, dst, timeMilis, message);
//    }
//
//    public void logMessageToDir(String dir, String src, String dst, IMessage message) throws IOException {
//        logMessage(dir, src, dst, System.currentTimeMillis(), message);
//    }
//
//    public void logMessageToDir(String dir, IMessage message) throws IOException {
//        logMessage(dir, "unknown", "unknown", System.currentTimeMillis(), message);
//    }
//    public void logMessage(String dir, String src, String dst, long timestamp, IMessage message) throws IOException {
//        if (message == null)
//            return;
//        Object src_ent = message.getHeaderValue("src_ent");
//        Object dst_ent = message.getHeaderValue("dst_ent");
//
//        if (src_ent == null)
//            src_ent = "255";
//        if (dst_ent == null)
//            dst_ent = "255";
//
//        logMessage(dir, src, dst, src_ent.toString(), dst_ent.toString(), timestamp, message);		
//    }
//
//
//    private long lastFlushMillis = System.currentTimeMillis();
//
//    public void logMessage(String dir, String src, String dst, String src_ent, String dst_ent, long timestamp, IMessage message) throws IOException {
//
//        if(!(message instanceof IMCMessage)) {
//            NeptusLog.pub().error("logging is not supported for "+message.getClass().getSimpleName());
//            return;
//        }
//
//        logMessage(dir, src, dst, src_ent, dst_ent, timestamp, (IMCMessage) message);
//    }
//
//    public void logMessage(String dir, String src, String dst, String src_ent, String dst_ent, long timestamp, IMCMessage message) throws IOException {
//
//        if (!isLoggingEnabled() || message == null) {
//            return;
//        }
//
//        if (message.getTimestamp() == 0)
//            message.setTimestamp(timestamp/1000.0);
//
//        String res = "self";
//        try {
//            ImcId16 imcid = new ImcId16(src);
//
//            ImcSystem sys = ImcSystemsHolder.lookupSystem(imcid);
//            if(sys != null)
//                res = sys.getName();
//            else {
//                if(!ImcMsgManager.getManager().getLocalId().equals(imcid))
//                    res = src; //imcid.toPrettyString(); //This has to use src because of the inline msgs
//            }
//            if (ConfigFetch.isOSEqual(ConfigFetch.OS_WINDOWS))
//                if (res != null)
//                    res = res.replaceAll(":", ".");
//        }
//
//        catch (Exception e1) {
//            e1.printStackTrace();
//            res = "unknown";
//        }
//
//        dir = logName + sep + res;
//
//        final LLFMessageLogger logger = findLogForMessage(dir, message);
//        logger.logMessage(message);
//        LsfMessageLogger.log(message);
//
//        if (System.currentTimeMillis() - lastFlushMillis > 10000) {
//            Thread t = new Thread(NeptusMessageLogger.this.getClass().getSimpleName() + "::lof flusher::" + System.currentTimeMillis()) {
//                @Override
//                public void run() {
//                    logger.flushLogs();
//                    lastFlushMillis = System.currentTimeMillis();
//                }
//            };
//            t.setDaemon(false);
//            t.start();
//            try {
//                t.join();
//            }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void logMessage(String src, String dest, long timestamp, IMessage message) throws IOException {
//        logMessage("messages"+sep+src, src, dest, timestamp, message);
//    }	
//
//    private LLFMessageLogger findLogForMessage(String logDir, IMessage message) throws IOException {
//
//        if (!isLoggingEnabled())
//            return null;
//
//        if (logDirs.containsKey(logDir))
//            logDir = logDirs.get(logDir);
//        else {
//            logDir = logDir.replace('\\', '/');			
//            String tmp = LOG_DIR+sep+logDir+sep+day.format(startDate)+sep+timeOfDay.format(startDate);
//            new File(tmp).mkdirs();
//            logDirs.put(logDir, tmp);
//
//            logDir = tmp;			
//        }
//        if (!loggers.containsKey(logDir))
//            loggers.put(logDir, new LLFMessageLogger(logDir));
//
//        new File(logDir).mkdirs();
//        return loggers.get(logDir);
//    }
//
//    /**
//     * Clears all the opened (log) files
//     * This method is automatically called when the JVM terminates but you can also call it as needed
//     */
//    public void cleanup() {
//        for (LLFMessageLogger l : loggers.values()) {
//            try {
//                l.close();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        loggers.clear();
//    }
//
//    public boolean isLoggingEnabled() {
//        return LOG_ENABLED;
//    }
//
//    public void setLoggingEnabled(boolean log_enabled) {
//        LOG_ENABLED = log_enabled;
//    }
//
//    public String getLOG_DIR() {
//        return LOG_DIR;
//    }
//
//}
