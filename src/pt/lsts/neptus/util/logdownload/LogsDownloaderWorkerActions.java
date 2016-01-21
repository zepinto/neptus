/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 15/01/2016
 */
package pt.lsts.neptus.util.logdownload;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.net.ftp.FTPFile;
import org.jdesktop.swingx.JXCollapsiblePane;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
class LogsDownloaderWorkerActions {

    private LogsDownloaderWorker worker;
    private LogsDownloaderWorkerGUI gui;
    
    // Actions
    AbstractAction downloadListAction = null;
    AbstractAction downloadSelectedLogDirsAction = null;
    AbstractAction downloadSelectedLogFilesAction = null;
    AbstractAction deleteSelectedLogFoldersAction = null;
    AbstractAction deleteSelectedLogFilesAction = null;
    AbstractAction toggleConfPanelAction = null;
    AbstractAction toggleExtraInfoPanelAction = null;
    AbstractAction helpAction = null;
    AbstractAction resetAction = null;
    AbstractAction stopAllAction = null;
    AbstractAction turnCameraOn = null;

    boolean stopLogListProcessing = false;
    boolean resetting = false;

    /**
     * This will initialize the actions and set them up on the GUI
     * (so the GUI components are assume to exist).
     * 
     * @param worker
     * @param gui
     */
    public LogsDownloaderWorkerActions(LogsDownloaderWorker worker, LogsDownloaderWorkerGUI gui) {
        this.worker = worker;
        this.gui = gui;
        
        initializeActions();
        setupListenersOnGuiComponents();
    }

    private void initializeActions() {
        downloadListAction = createDownloadListAction();
        downloadSelectedLogDirsAction = createDownloadSelectedLogDirsAction();
        downloadSelectedLogFilesAction = createDownloadSelectedLogFilesAction();
        
        deleteSelectedLogFoldersAction = createDeleteSelectedLogFoldersAction();
        deleteSelectedLogFilesAction = createDeleteSelectedLogFilesAction();
        
        toggleConfPanelAction = createToggleConfPanelAction();
        toggleExtraInfoPanelAction = createToggleExtraInfoPanelAction();
        
        helpAction = createHelpAction();
        
        resetAction = createResetAction();
        stopAllAction = createStopAllAction();
        turnCameraOn = createTurnCameraOnAction();
    }

    private void setupListenersOnGuiComponents() {
        gui.downloadListButton.addActionListener(downloadListAction);
        gui.downloadSelectedLogDirsButton.addActionListener(downloadSelectedLogDirsAction);
        gui.downloadSelectedLogFilesButton.addActionListener(downloadSelectedLogFilesAction);
        
        gui.deleteSelectedLogFoldersButton.addActionListener(deleteSelectedLogFoldersAction);
        gui.deleteSelectedLogFilesButton.addActionListener(deleteSelectedLogFilesAction);

        // Collapsible Panel Show/Hide buttons
        gui.toggleConfPanelButton.addActionListener(toggleConfPanelAction);
        gui.toggleExtraInfoPanelButton.addActionListener(toggleExtraInfoPanelAction);

        gui.helpButton.addActionListener(helpAction);
        
        gui.resetButton.addActionListener(resetAction);
        gui.stopAllButton.addActionListener(stopAllAction);
        gui.cameraButton.addActionListener(turnCameraOn);
    }

    @SuppressWarnings("serial")
    private AbstractAction createDownloadListAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        if (stopLogListProcessing)
                            stopLogListProcessing = false;

                        long time = System.currentTimeMillis();
                        showInGuiStarting();

                        gui.downloadListButton.setEnabled(false);
                        // logFolderList.setEnabled(false);
                        gui.logFolderList.setValueIsAdjusting(true);
                        // logFilesList.setEnabled(false);

                        // ->Getting txt list of logs from server
                        showInGuiConnectingToServers();

                        // Map base log folder vs servers presence (space separated list of servers keys)
                        LinkedHashMap<String, String> serversLogPresenceList = new LinkedHashMap<>();
                        // Map FTPFile (log base folder) vs remote path
                        LinkedHashMap<FTPFile, String> retList = new LinkedHashMap<>();

                        // Get list from servers
                        getFromServersBaseLogList(retList, serversLogPresenceList);
                        
                        if (retList.isEmpty()) {
                            gui.msgPanel.writeMessageTextln(I18n.text("Done"));
                            return null;
                        }

                        gui.msgPanel.writeMessageTextln(I18n.textf("Log Folders: %numberoffolders", retList.size()));

                        long timeS1 = System.currentTimeMillis();
                        
                        // Added in order not to show the active log (the last one)
                        orderAndFilterOutTheActiveLog(retList);
                        showInGuiNumberOfLogsFromServers(retList);
                        if (retList.size() == 0) // Abort the rest of processing
                            return null;

                        // ->Removing from already existing LogFolders to LOCAL state
                        showInGuiFiltering();
                        setStateLocalIfNotInPresentServer(retList);

                        if (stopLogListProcessing)
                            return null;
                        
                        // ->Adding new LogFolders
                        LinkedList<LogFolderInfo> existenteLogFoldersFromServer = new LinkedList<LogFolderInfo>();
                        LinkedList<LogFolderInfo> newLogFoldersFromServer = new LinkedList<LogFolderInfo>();
                        addTheNewFoldersAnFillTheReturnedExistentAndNewLists(retList, existenteLogFoldersFromServer,
                                newLogFoldersFromServer);

                        if (stopLogListProcessing)
                            return null;

                        // ->Getting Log files list from server
                        showInGuiProcessingLogList();
                        LinkedList<LogFolderInfo> tmpLogFolderList = getFromServersCompleteLogList(serversLogPresenceList);

                        showInGuiUpdatingLogsInfo();

                        long timeF1 = System.currentTimeMillis();
                        // Testing for log files from each log folder
                        testingForLogFilesFromEachLogFolderAndFillInfo(tmpLogFolderList);
                        NeptusLog.pub().warn(".......Testing for log files from each log folder " +
                                (System.currentTimeMillis() - timeF1) + "ms");

                        if (stopLogListProcessing)
                            return null;
                        
                        long timeF2 = System.currentTimeMillis();
                        testNewReportedLogFoldersForLocalCorrespondent(newLogFoldersFromServer);
                        updateLogFoldersState(existenteLogFoldersFromServer);
                        NeptusLog.pub().warn(".......Updating LogFolders State " +
                                (System.currentTimeMillis() - timeF2) + "ms");

                        long timeF3 = System.currentTimeMillis();
                        updateFilesListGUIForFolderSelected();
                        NeptusLog.pub().warn(".......updateFilesListGUIForFolderSelected " +
                                (System.currentTimeMillis() - timeF3) + "ms");

                        NeptusLog.pub().warn("....process list from all servers " + (System.currentTimeMillis() - timeS1) + "ms");                        

                        showInGuiUpdatingGui();
                        
//                        gui.logFolderList.invalidate();
//                        gui.logFolderList.revalidate();
//                        gui.logFolderList.repaint();
//                        gui.logFolderList.setEnabled(true);
//                        // logFilesList.invalidate();
//                        // logFilesList.revalidate();
//                        // logFilesList.repaint();
//                        gui.logFilesList.setEnabled(true);

                        NeptusLog.pub().warn("....all downloadListAction " + (System.currentTimeMillis() - time) + "ms");
                        showInGuiDone();
                        return true;
                    }

                    @Override
                    public void finish() {
                        stopLogListProcessing = false;

                        gui.logFolderList.setValueIsAdjusting(false);
                        gui.logFolderList.invalidate();
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        // logFilesList.invalidate();
                        // logFilesList.revalidate();
                        // logFilesList.repaint();
                        gui.listHandlingProgressBar.setValue(0);
                        gui.listHandlingProgressBar.setIndeterminate(false);
                        gui.listHandlingProgressBar.setString("");
                        gui.logFilesList.setEnabled(true);
                        gui.downloadListButton.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };
    }

    /**
     * Goes through the servers ({@link LogsDownloaderWorker#getServersList()})
     * and gets the base logs list.
     * 
     * It fills the provided retList and serversLogPresenceList.
     * 
     * @param baselogFolderList
     * @param serversLogPresenceList
     */
    private void getFromServersBaseLogList(LinkedHashMap<FTPFile, String> baselogFolderList,
            LinkedHashMap<String, String> serversLogPresenceList) {
        long timeD1 = System.currentTimeMillis();
        for (String serverKey : worker.getServersList()) {
            long timeD2 = System.currentTimeMillis();
            LinkedHashMap<FTPFile, String> ret = getBaseLogListFrom(serverKey);
            fillServerPresenceList(serverKey, ret, baselogFolderList, serversLogPresenceList);
            NeptusLog.pub().warn(".......get list from '" + serverKey + "' server "
                    + (System.currentTimeMillis() - timeD2) + "ms");
        }
        NeptusLog.pub().warn(".......get list from all servers " + (System.currentTimeMillis() - timeD1) + "ms");                        
    }

    /**
     * Contacts the server given by the serverKey ID and gets the base logs list.
     * 
     * Uses {@link LogsDownloaderWorker#getHostFor(String)} and
     * {@link LogsDownloaderWorker#getPortFor(String)} to fill the destination.
     * 
     * @param serverKey
     * @return
     */
    private LinkedHashMap<FTPFile, String> getBaseLogListFrom(String serverKey) {
        LinkedHashMap<FTPFile, String> retList = null;
        String host = worker.getHostFor(serverKey);
        int port = worker.getPortFor(serverKey);
        
        if (host.length() == 0 || !worker.isServerAvailable(serverKey))
            return retList;
        
        try {
            FtpDownloader clientFtp = LogsDownloaderWorkerUtil.getOrRenewFtpDownloader(serverKey,
                    worker.getFtpDownloaders(), host, port);
            retList = clientFtp.listLogs();
        }
        catch (Exception e) {
            NeptusLog.pub().error("Connecting " + serverKey + " with " 
                    + host + ":" + port + " with error: " + e.getMessage());
        }
        return retList;
    }

    /**
     * For the given server with serverKey ID, takes his {@link #getBaseLogListFrom(String)}
     * reply as toProcessLogList and fill the serversLogPresenceList for each base log
     * adding the serverKey to the list of presence for that base log.
     * 
     * If finalLogList is not null, also adds the missing entries to it.
     * 
     * @param serverKey
     * @param toProcessLogList
     * @param finalLogList
     * @param serversLogPresenceList
     */
    private void fillServerPresenceList(String serverKey, LinkedHashMap<FTPFile, String> toProcessLogList,
            LinkedHashMap<FTPFile, String> finalLogList, LinkedHashMap<String, String> serversLogPresenceList) {

        if (toProcessLogList != null && !toProcessLogList.isEmpty()) {
            if (finalLogList == null || finalLogList.isEmpty()) {
                for (String partialUri : toProcessLogList.values()) {
                    serversLogPresenceList.put(partialUri, serverKey);
                }
                if (finalLogList != null)
                    finalLogList.putAll(toProcessLogList);
            }
            else {
                for (FTPFile ftpFile : toProcessLogList.keySet()) {
                    String val = toProcessLogList.get(ftpFile);
                    if (finalLogList.containsValue(val)) {
                        serversLogPresenceList.put(val, serversLogPresenceList.get(val) + " " + serverKey);
                        continue;
                    }
                    else {
                        finalLogList.put(ftpFile, val);
                        serversLogPresenceList.put(val, serverKey);
                    }
                }
            }
        }
    }

    private void orderAndFilterOutTheActiveLog(LinkedHashMap<FTPFile, String> retList) {
        if (retList.size() > 0) {
            String[] ordList = retList.values().toArray(new String[retList.size()]);
            Arrays.sort(ordList);
            String activeLogName = ordList[ordList.length - 1];
            for (FTPFile fFile : retList.keySet().toArray(new FTPFile[retList.size()])) {
                if (retList.get(fFile).equals(activeLogName)) {
                    retList.remove(fFile);
                    break;
                }
            }
        }
    }

    private void setStateLocalIfNotInPresentServer(LinkedHashMap<FTPFile, String> retList) {
        long timeC1 = System.currentTimeMillis();

        Object[] objArray = new Object[gui.logFolderList.myModel.size()];
        gui.logFolderList.myModel.copyInto(objArray);
        for (Object comp : objArray) {
            if (stopLogListProcessing)
                break;

            try {
                // NeptusLog.pub().info("<###>... upda
                LogFolderInfo log = (LogFolderInfo) comp;
                if (!retList.containsValue(log.getName())) {
                    // retList.remove(log.getName());
                    for (LogFileInfo lfx : log.getLogFiles()) {
                        if (stopLogListProcessing)
                            break;
                        lfx.setState(LogFolderInfo.State.LOCAL);
                    }
                    log.setState(LogFolderInfo.State.LOCAL);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().debug(e.getMessage());
            }
        }
        NeptusLog.pub().warn(".......Removing from already existing LogFolders to LOCAL state "
                + (System.currentTimeMillis() - timeC1) + "ms");
    }

    private void addTheNewFoldersAnFillTheReturnedExistentAndNewLists(
            LinkedHashMap<FTPFile, String> retList,
            LinkedList<LogFolderInfo> existenteLogFoldersFromServer,
            LinkedList<LogFolderInfo> newLogFoldersFromServer)
                    throws InterruptedException, InvocationTargetException {
        for (String newLogName : retList.values()) {
            if (stopLogListProcessing)
                return;

            final LogFolderInfo newLogDir = new LogFolderInfo(newLogName);
            if (gui.logFolderList.containsFolder(newLogDir)) {
                existenteLogFoldersFromServer.add(gui.logFolderList.getFolder((newLogDir.getName())));
            }
            else {
                newLogFoldersFromServer.add(newLogDir);
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        gui.logFolderList.addFolder(newLogDir);
                    }
                });
            }
        }
        // msgPanel.writeMessageTextln("Logs Folders: " + logFolderList.myModel.size());
    }

    /**
     * Process the serversLogPresenceList and gets the actual log files/folder for each base log folder. This is done by
     * iterating the {@link LogsDownloaderWorker#getServersList()} depending on its presence.
     * 
     * @param serversLogPresenceList Map for each base log folder and servers presence as values (space separated list
     *            of servers keys)
     * @return
     */
    private LinkedList<LogFolderInfo> getFromServersCompleteLogList(
            LinkedHashMap<String, String> serversLogPresenceList) {
        if (serversLogPresenceList.size() == 0)
            return new LinkedList<LogFolderInfo>();

        long timeF0 = System.currentTimeMillis();

        LinkedList<LogFolderInfo> tmpLogFolders = new LinkedList<LogFolderInfo>();
        
        try {
            ArrayList<String> servers = worker.getServersList();
            for (String serverKey : servers) { // Let's iterate by servers first
                if (stopLogListProcessing)
                    break;
                
                if (!worker.isServerAvailable(serverKey))
                    continue;
                
                FtpDownloader ftpServer = null;
                try {
                    ftpServer = LogsDownloaderWorkerUtil.getOrRenewFtpDownloader(serverKey,
                            worker.getFtpDownloaders(), worker.getHostFor(serverKey), worker.getPortFor(serverKey));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                if (ftpServer == null)
                    continue;
                
                String host = worker.getHostFor(serverKey); // To fill the log files host info
                
                for (String logDir : serversLogPresenceList.keySet()) { // For the server go through the folders
                    if (stopLogListProcessing)
                        break;

                    if (!serversLogPresenceList.get(logDir).contains(serverKey))
                        continue;
                    
                    // This is needed to avoid problems with non English languages
                    String isoStr = new String(logDir.getBytes(), "ISO-8859-1");
                    
                    LogFolderInfo lFolder = null;

                    for (LogFolderInfo lfi : tmpLogFolders) {
                        if (lfi.getName().equals(logDir))
                            lFolder = lfi;
                    }
                    if (lFolder == null) {
                        lFolder = new LogFolderInfo(logDir);
                    }
                    
                    if (!ftpServer.isConnected())
                        ftpServer.renewClient();
                    
                    try {
                        FTPFile[] files = ftpServer.getClient().listFiles("/" + isoStr + "/");
                        for (FTPFile file : files) {
                            if (stopLogListProcessing)
                                break;

                            String name = logDir + "/" + file.getName();
                            String uriPartial = logDir + "/" + file.getName();
                            LogFileInfo logFileTmp = new LogFileInfo(name);
                            logFileTmp.setUriPartial(uriPartial);
                            logFileTmp.setSize(file.getSize());
                            logFileTmp.setFile(file);
                            logFileTmp.setHost(host);
                            
                            // Let us see if its a directory
                            if (file.isDirectory()) {
                                logFileTmp.setSize(-1); // Set size to -1 if directory
                                long allSize = 0;

                                // Here there are no directories, considering only 2 folder layers only, e.g. "Photos/00000"
                                LinkedHashMap<String, FTPFile> dirListing = ftpServer.listDirectory(logFileTmp.getName());
                                ArrayList<LogFileInfo> directoryContents = new ArrayList<>();
                                for (String fName : dirListing.keySet()) {
                                    if (stopLogListProcessing)
                                        break;

                                    FTPFile fFile = dirListing.get(fName);
                                    String fURIPartial = fName;
                                    LogFileInfo fLogFileTmp = new LogFileInfo(fName);
                                    fLogFileTmp.setUriPartial(fURIPartial);
                                    fLogFileTmp.setSize(fFile.getSize());
                                    fLogFileTmp.setFile(fFile);
                                    fLogFileTmp.setHost(host);

                                    allSize += fLogFileTmp.getSize();
                                    directoryContents.add(fLogFileTmp);
                                }
                                logFileTmp.setDirectoryContents(directoryContents);
                                logFileTmp.setSize(allSize);
                            }
                            lFolder.addFile(logFileTmp);
                            tmpLogFolders.add(lFolder);
                        }
                    }
                    catch (Exception e) {
                        System.err.println(isoStr);
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        NeptusLog.pub().warn(".......Contacting remote system for complete log file list " +
                (System.currentTimeMillis() - timeF0) + "ms");

        return tmpLogFolders;
    }
    
    private void testingForLogFilesFromEachLogFolderAndFillInfo(
            LinkedList<LogFolderInfo> tmpLogFolderList) {
        Object[] objArray = new Object[gui.logFolderList.myModel.size()];
        gui.logFolderList.myModel.copyInto(objArray);
        for (Object comp : objArray) {
            if (stopLogListProcessing)
                return;

            try {
                LogFolderInfo logFolder = (LogFolderInfo) comp;

                int indexLFolder = tmpLogFolderList.indexOf(logFolder);
                LinkedHashSet<LogFileInfo> logFilesTmp = (indexLFolder != -1) ? tmpLogFolderList.get(
                        indexLFolder).getLogFiles() : new LinkedHashSet<LogFileInfo>();
                        for (LogFileInfo logFx : logFilesTmp) {
                            if (stopLogListProcessing)
                                return;

                            if (!logFolder.getLogFiles().contains(logFx)) {
                                // The file or directory is new
                                logFolder.addFile(logFx);
                            }
                            else {
                                // The file or directory is already known so let us update
                                LogFileInfo lfx = logFolder.getLogFile(logFx.getName()/* fxStr */);
                                if (lfx.getSize() == -1) {
                                    lfx.setSize(logFx.getSize());
                                }
                                else if (lfx.getSize() != logFx.getSize()) {
                                    System.out.println("//////////// " + lfx.getSize() + "  " + logFx.getSize());
                                    if (lfx.getState() == LogFolderInfo.State.SYNC)
                                        lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                    else if (lfx.getState() == LogFolderInfo.State.LOCAL)
                                        lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                    lfx.setSize(logFx.getSize());
                                    lfx.setFile(logFx.getFile());
                                }
                                else if (lfx.getSize() == logFx.getSize()) {
                                    if (lfx.getState() == LogFolderInfo.State.LOCAL)
                                        lfx.setState(LogFolderInfo.State.SYNC);
                                }
                                lfx.setHost(logFx.getHost());

                                if (logFx.isDirectory()) {
                                    ArrayList<LogFileInfo> notMatchElements = new ArrayList<>();
                                    notMatchElements.addAll(lfx.getDirectoryContents());
                                    for (LogFileInfo lfi : logFx.getDirectoryContents()) {
                                        boolean alreadyExists = false;
                                        for (LogFileInfo lfiLocal : lfx.getDirectoryContents()) {
                                            if (lfi.equals(lfiLocal)) {
                                                alreadyExists = true;
                                                notMatchElements.remove(lfiLocal);
                                                lfi.setSize(lfiLocal.getSize());
                                                lfi.setFile(lfiLocal.getFile());
                                                lfi.setHost(lfiLocal.getHost());
                                            }
                                        }
                                        if (!alreadyExists) {
                                            lfx.getDirectoryContents().add(lfi);
                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                        }
                                    }
                                    for (LogFileInfo lfi : notMatchElements) {
                                        lfx.getDirectoryContents().remove(lfi);
                                    }
                                }

                                if (lfx.isDirectory()) {
                                    if (!LogsDownloaderWorkerUtil.getFileTarget(lfx.getName(), worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                        for (LogFileInfo lfi : lfx.getDirectoryContents()) {
                                            if (!LogsDownloaderWorkerUtil.getFileTarget(lfi.getName(), worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                                if (lfx.getState() != LogFolderInfo.State.NEW && lfx.getState() != LogFolderInfo.State.DOWNLOADING)
                                                    lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                                break;
                                            }
                                        }
                                    }
                                    else {
                                        long sizeD = LogsDownloaderWorkerUtil.getDiskSizeFromLocal(lfx, worker);
                                        if (lfx.getSize() != sizeD && lfx.getState() == LogFolderInfo.State.SYNC)
                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                    }
                                }
                                else {
                                    if (!LogsDownloaderWorkerUtil.getFileTarget(lfx.getName(), worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                        if (lfx.getState() != LogFolderInfo.State.NEW && lfx.getState() != LogFolderInfo.State.DOWNLOADING) {
                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                            // System.out.println("//////////// " + lfx.getName() + "  " + LogsDownloaderUtil.getFileTarget(lfx.getName()).exists());
                                        }
                                    }
                                    else {
                                        long sizeD = LogsDownloaderWorkerUtil.getDiskSizeFromLocal(lfx, worker);
                                        if (lfx.getSize() != sizeD && lfx.getState() == LogFolderInfo.State.SYNC)
                                            lfx.setState(LogFolderInfo.State.INCOMPLETE);
                                    }
                                }
                            }
                        }

                        // Put LOCAL state on files not in server
                        LinkedHashSet<LogFileInfo> toDelFL = new LinkedHashSet<LogFileInfo>();
                        for (LogFileInfo lfx : logFolder.getLogFiles()) {
                            if (!logFilesTmp.contains(lfx)
                                    /* !res.keySet().contains(lfx.getName()) */) {
                                lfx.setState(LogFolderInfo.State.LOCAL);
                                if (!LogsDownloaderWorkerUtil.getFileTarget(lfx.getName(), 
                                        worker.getDirBaseToStoreFiles(), worker.getLogLabel()).exists()) {
                                    toDelFL.add(lfx);
                                    // logFolder.getLogFiles().remove(lfx); //This cannot be done here
                                }
                            }
                        }
                        for (LogFileInfo lfx : toDelFL)
                            logFolder.getLogFiles().remove(lfx);
            }
            catch (Exception e) {
                NeptusLog.pub().debug(e.getMessage());
            }
        }
    }
    
    private void testNewReportedLogFoldersForLocalCorrespondent(
            LinkedList<LogFolderInfo> newLogFoldersFromServer) {
        worker.testNewReportedLogFoldersForLocalCorrespondent(newLogFoldersFromServer);
    }

    private void updateLogFoldersState(LinkedList<LogFolderInfo> existenteLogFoldersFromServer) {
        for (LogFolderInfo logFolder : existenteLogFoldersFromServer) {
            LogsDownloaderWorkerGUIUtil.updateLogFolderState(logFolder, gui.logFolderList);
        }
        LogsDownloaderWorkerGUIUtil.updateLogStateIconForAllLogFolders(gui.logFolderList,
                gui.logFoldersListLabel);
    }

    private void updateFilesListGUIForFolderSelected() {
        // updateFilesListGUIForFolderSelected();
        new Thread("updateFilesListGUIForFolderSelected") {
            @Override
            public void run() {
                worker.updateFilesListGUIForFolderSelected();
            };
        }.start();
    }
    
    private void showInGuiStarting() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(0);
                gui.listHandlingProgressBar.setString(I18n.text("Starting..."));
            }
        });
    }
    
    private void showInGuiConnectingToServers() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(10);
                gui.listHandlingProgressBar.setIndeterminate(true);
                gui.listHandlingProgressBar.setString(I18n
                        .text("Connecting to remote system for log list update..."));
            }
        });
    }

    private void showInGuiNumberOfLogsFromServers(LinkedHashMap<FTPFile, String> retList)
            throws InterruptedException, InvocationTargetException {
        if (retList.size() == 0) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    gui.listHandlingProgressBar.setValue(100);
                    gui.listHandlingProgressBar.setIndeterminate(false);
                    gui.listHandlingProgressBar.setString(I18n.text("No logs..."));
                }
            });
        }
        else {
            final String msg1 = I18n.textf("Log Folders: %numberoffolders", retList.size());
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // listHandlingProgressBar.setValue(10);
                    // listHandlingProgressBar.setIndeterminate(true);
                    gui.listHandlingProgressBar.setString(msg1);
                }
            });
        }
    }

    private void showInGuiFiltering() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(20);
                gui.listHandlingProgressBar.setIndeterminate(false);
                gui.listHandlingProgressBar.setString(I18n.text("Filtering list..."));
            }
        });
    }

    private void showInGuiProcessingLogList() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(30);
                gui.listHandlingProgressBar.setIndeterminate(true);
                gui.listHandlingProgressBar.setString(I18n
                        .text("Contacting remote system for complete log file list..."));

                gui.listHandlingProgressBar.setValue(40);
                gui.listHandlingProgressBar.setIndeterminate(false);
                gui.listHandlingProgressBar.setString(I18n.text("Processing log list..."));
            }
        });
    }

    private void showInGuiUpdatingLogsInfo() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(70);
                gui.listHandlingProgressBar.setIndeterminate(false);
                gui.listHandlingProgressBar.setString(I18n.text("Updating logs info..."));
            }
        });
    }

    private void showInGuiUpdatingGui() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(90);
                gui.listHandlingProgressBar.setIndeterminate(false);
                gui.listHandlingProgressBar.setString(I18n.text("Updating GUI..."));
            }
        });
    }

    private void showInGuiDone() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                gui.listHandlingProgressBar.setValue(100);
                gui.listHandlingProgressBar.setIndeterminate(false);
                gui.listHandlingProgressBar.setString(I18n.text("Done"));
            }
        });
    }
    
    @SuppressWarnings("serial")
    private AbstractAction createDownloadSelectedLogDirsAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                gui.downloadSelectedLogDirsButton.setEnabled(false);
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        for (Object comp : gui.logFolderList.getSelectedValues()) {
                            try {
                                // NeptusLog.pub().info("<###>... updateFilesForFolderSelected");
                                LogFolderInfo logFd = (LogFolderInfo) comp;
                                for (LogFileInfo lfx : logFd.getLogFiles()) {
                                    // if (downloadSelectedLogDirsButton.isEnabled())
                                    //      break; // If button enabled a reset was called, so let's interrupt all 
                                    if (resetting)
                                        break;

                                    worker.singleLogFileDownloadWorker(lfx, logFd);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }

                            if (resetting)
                                break;
                        }
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.downloadSelectedLogDirsButton.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                AsyncWorker.getWorkerThread().postTask(task);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createDownloadSelectedLogFilesAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        gui.downloadSelectedLogFilesButton.setEnabled(false);

                        for (Object comp : gui.logFilesList.getSelectedValues()) {
                            if (resetting)
                                break;

                            try {
                                LogFileInfo lfx = (LogFileInfo) comp;
                                worker.singleLogFileDownloadWorker(lfx,
                                        LogsDownloaderWorkerUtil.findLogFolderInfoForFile(lfx, gui.logFolderList));
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.downloadSelectedLogFilesButton.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createDeleteSelectedLogFoldersAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        gui.deleteSelectedLogFoldersButton.setEnabled(false);
                        // logFolderList.setEnabled(false);
                        // logFilesList.setEnabled(false);

                        Object[] objArray = gui.logFolderList.getSelectedValues();
                        if (objArray.length == 0)
                            return null;

                        JOptionPane jop = new JOptionPane(I18n.text("Are you sure you want to delete "
                                + "selected log folders from remote system?"), JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.YES_NO_OPTION);
                        JDialog dialog = jop.createDialog(gui.frameCompHolder, I18n.text("Remote Delete Confirmation"));
                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                        dialog.setVisible(true);
                        Object userChoice = jop.getValue();
                        try {
                            if (((Integer) userChoice) != JOptionPane.YES_OPTION) {
                                return null;
                            }
                        }
                        catch (Exception e2) {
                            NeptusLog.pub().error(e2.getMessage());
                            return null;
                        }
                        gui.deleteSelectedLogFoldersButton.setEnabled(true);
                        for (Object comp : objArray) {
                            try {
                                LogFolderInfo logFd = (LogFolderInfo) comp;
                                boolean resDel = worker.deleteLogFolderFromServer(logFd);
                                if (resDel) {
                                    logFd.setState(LogFolderInfo.State.LOCAL);
                                    LinkedHashSet<LogFileInfo> logFiles = logFd.getLogFiles();

                                    LinkedHashSet<LogFileInfo> toDelFL = LogsDownloaderWorkerGUIUtil
                                            .updateLogFilesStateDeleted(logFiles, gui.downloadWorkersHolder,
                                                    worker.getDirBaseToStoreFiles(), worker.getLogLabel());
                                    for (LogFileInfo lfx : toDelFL) {
                                        if (resetting)
                                            break;

                                        logFd.getLogFiles().remove(lfx);
                                    }
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }

                            if (resetting)
                                break;
                        }
                        worker.updateFilesListGUIForFolderSelected();
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.deleteSelectedLogFoldersButton.setEnabled(true);
                        gui.logFilesList.revalidate();
                        gui.logFilesList.repaint();
                        gui.logFilesList.setEnabled(true);
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createDeleteSelectedLogFilesAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gui.validateAndSetUI()) {
                    gui.popupErrorConfigurationDialog();
                    return;
                }
                AsyncTask task = new AsyncTask() {
                    @Override
                    public Object run() throws Exception {
                        gui.deleteSelectedLogFilesButton.setEnabled(false);

                        Object[] objArray = gui.logFilesList.getSelectedValues();
                        if (objArray.length == 0)
                            return null;

                        JOptionPane jop = new JOptionPane(
                                I18n.text("Are you sure you want to delete selected log files from remote system?"),
                                JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
                        JDialog dialog = jop.createDialog(gui.frameCompHolder, I18n.text("Remote Delete Confirmation"));
                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                        dialog.setVisible(true);
                        Object userChoice = jop.getValue();
                        try {
                            if (((Integer) userChoice) != JOptionPane.YES_OPTION) {
                                return null;
                            }
                        }
                        catch (Exception e2) {
                            NeptusLog.pub().error(e2.getMessage());
                            return null;
                        }
                        gui.deleteSelectedLogFoldersButton.setEnabled(true);

                        LinkedHashSet<LogFileInfo> logFiles = new LinkedHashSet<LogFileInfo>();
                        for (Object comp : objArray) {
                            if (resetting)
                                break;

                            try {
                                LogFileInfo lfx = (LogFileInfo) comp;
                                if (worker.deleteLogFileFromServer(lfx))
                                    logFiles.add(lfx);
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        if (!resetting) {
                            LogsDownloaderWorkerGUIUtil.updateLogFilesStateDeleted(logFiles, gui.downloadWorkersHolder,
                                    worker.getDirBaseToStoreFiles(), worker.getLogLabel());

                            worker.updateFilesListGUIForFolderSelected();
                        }
                        return true;
                    }

                    @Override
                    public void finish() {
                        gui.deleteSelectedLogFilesButton.setEnabled(true);
                        gui.logFilesList.revalidate();
                        gui.logFilesList.repaint();
                        gui.logFilesList.setEnabled(true);
                        gui.logFolderList.revalidate();
                        gui.logFolderList.repaint();
                        gui.logFolderList.setEnabled(true);
                        try {
                            this.getResultOrThrow();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };

                AsyncWorker.getWorkerThread().postTask(task);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createToggleConfPanelAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.configCollapsiblePanel.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION).actionPerformed(e);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createToggleExtraInfoPanelAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.extraInfoCollapsiblePanel.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION).actionPerformed(e);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createHelpAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GuiUtils.centerOnScreen(gui.downHelpDialog.getDialog());
                gui.downHelpDialog.getDialog().setIconImage(LogsDownloaderWorkerGUI.ICON_HELP.getImage());
                gui.downHelpDialog.getDialog().setVisible(true);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createResetAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.resetButton.setEnabled(false);
                worker.doReset(false);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createStopAllAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.stopAllButton.setEnabled(false);
                worker.doReset(true);
            }
        };
    }

    @SuppressWarnings("serial")
    private AbstractAction createTurnCameraOnAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ArrayList<EntityParameter> propList = new ArrayList<>();
                    EntityParameter entParsm = new EntityParameter().setName("Active")
                            .setValue(gui.cameraButton.getBackground() != LogsDownloaderWorker.CAM_CPU_ON_COLOR ? "true"
                                    : "false");
                    propList.add(entParsm);
                    SetEntityParameters setParams = new SetEntityParameters();
                    setParams.setName(LogsDownloaderWorker.CAMERA_CPU_LABEL);
                    setParams.setParams(propList);

                    ImcMsgManager.getManager().sendMessageToSystem(setParams, worker.getLogLabel());
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
    }
}
