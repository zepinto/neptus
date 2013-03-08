/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 21/06/2011
 * $Id:: PlanControlPanel.java 10081 2013-03-08 17:47:25Z mfaria                $:
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.notifications.Notification;
import pt.up.fe.dceg.neptus.console.plugins.IPlanSelection;
import pt.up.fe.dceg.neptus.console.plugins.ITransponderSelection;
import pt.up.fe.dceg.neptus.console.plugins.LockableSubPanel;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.SystemsList;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.system.btn.SystemsSelectionAction;
import pt.up.fe.dceg.neptus.gui.system.btn.SystemsSelectionAction.SelectionType;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.imc.PlanControl;
import pt.up.fe.dceg.neptus.imc.PlanControl.TYPE;
import pt.up.fe.dceg.neptus.imc.PlanDB;
import pt.up.fe.dceg.neptus.imc.PlanDB.OP;
import pt.up.fe.dceg.neptus.imc.Teleoperation;
import pt.up.fe.dceg.neptus.imc.TeleoperationDone;
import pt.up.fe.dceg.neptus.imc.VehicleState;
import pt.up.fe.dceg.neptus.imc.VehicleState.OP_MODE;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.map.TransponderUtils;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCSendMessageUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageDeliveryListener;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Plan Control", author = "Paulo Dias", version = "1.2.3", documentation = "plan-control/plan-control.html#PlanControl", category = CATEGORY.INTERFACE)
public class PlanControlPanel extends SimpleSubPanel implements ConfigurationListener, MainVehicleChangeListener,
        LockableSubPanel, IPeriodicUpdates, NeptusMessageListener {

    protected static final boolean DONT_USE_ACOUSTICS = true;
    protected static final boolean USE_ACOUSTICS = false;
    
    private final ImageIcon ICON_BEACONS = ImageUtils
            .getIcon("pt/up/fe/dceg/neptus/plugins/planning/uploadBeacons.png");
    private final ImageIcon ICON_UP = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/up.png");
    private final ImageIcon ICON_DOWN_R = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/fileimport.png");
    private final ImageIcon ICON_START = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/stop.png");
    private final ImageIcon ICON_TELEOP_ON = ImageUtils.getScaledIcon(
            "pt/up/fe/dceg/neptus/plugins/planning/teleoperation.png", 32, 32);
    private final ImageIcon ICON_TELEOP_OFF = ImageUtils.getScaledIcon(
            "pt/up/fe/dceg/neptus/plugins/planning/teleoperation-off.png", 32, 32);

    private final String startTeleOperationStr = I18n.text("Start Tele-Operation");
    private final String stopTeleOperationStr = I18n.text("Stop Tele-Operation");
    private final String startPlanStr = I18n.text("Start Plan");
    private final String stopPlanStr = I18n.text("Stop Plan");
    private final String sendAcousticBeaconsStr = I18n.text("Send Acoustic Beacons. Use Ctrl click to clear the transponders from vehicle.");
    private final String sendSelectedPlanStr = I18n.text("Send Selected Plan");
    private final String downloadActivePlanStr = I18n.text("Download Active Plan");
    
    @NeptusProperty(name = "Font Size Multiplier", description = "The font size. Use '1' for default.")
    public int fontMultiplier = 1;

    @NeptusProperty(name = "Verify plans for island nodes", userLevel = LEVEL.ADVANCED,
            description = "Always runs a verification "
            + "on the plan for maneuvers that have no input edges. If you choose to switch off here "
            + "you can allways click Alt whan sending the plan that this verification will run.")
    public boolean allwaysVerifyAllManeuversUsed = true;

//    @NeptusProperty(name = "Use Acoustic To Send Msg If System Not In WiFi Range", userLevel = LEVEL.ADVANCED, 
//            distribution = DistributionEnum.DEVELOPER)
//    public boolean useAcousticToSendMsgIfSystemNotInWiFiRange = true;

    @NeptusProperty(name = "Service name for acoustic message sending", userLevel = LEVEL.ADVANCED, 
            distribution = DistributionEnum.DEVELOPER)
    public String acousticOpServiceName = "acoustic/operation";

    @NeptusProperty(name = "Use only active systems for acoustic message sending", userLevel = LEVEL.ADVANCED, 
            distribution = DistributionEnum.DEVELOPER)
    public boolean acousticOpUseOnlyActive = false;

    @NeptusProperty(name = "Use Full Mode or Teleoperation Mode", userLevel = LEVEL.ADVANCED, 
            description = "By default this value is true and makes "
            + "it display all buttons, if false only teleoperation button is shown.")
    public boolean useFullMode = true;

    @NeptusProperty(name = "Enable console actions", hidden = true)
    public boolean enableConsoleActions = true;

    // @NeptusProperty(name = "Use PlanDB to send plan", description = "For current vehicles set to true.")
    // public boolean usePlanDBToSendPlan = true;

    @NeptusProperty(name = "Enable selection button", userLevel = LEVEL.ADVANCED,
            description = "Configures if system selection button is active or not")
    public boolean enableSelectionButton = false;
    
    @NeptusProperty(name = "Enable beacons button", userLevel = LEVEL.ADVANCED,
            description = "Configures if send beacons button is active or not")
    public boolean enableBeaconsButton = true;

    @NeptusProperty(name = "Use Calibration on Start Plan", userLevel = LEVEL.ADVANCED)
    public boolean useCalibrationOnStartPlan = true;

    @NeptusProperty(name = "Use TCP To Send Messages", userLevel = LEVEL.ADVANCED)
    public boolean useTcpToSendMessages = true;

    // GUI
    private JPanel holder;
    private JLabel titleLabel;
    private JLabel planIdLabel;
    private ToolbarButton selectionButton, sendAcousticsButton, sendUploadPlanButton, sendDownloadPlanButton,
            sendStartButton, sendStopButton, teleOpButton;

    private SystemsSelectionAction selectionAction;
    private AbstractAction sendAcousticsAction, sendUploadPlanAction, sendDownloadPlanAction, sendStartAction,
            sendStopAction, teleOpAction;

    private int teleoperationManeuver = -1;
    {
        IMCMessage tomsg = IMCDefinition.getInstance().create("Teleoperation");
        if (tomsg != null)
            teleoperationManeuver = tomsg.getMgid();
    }

    private boolean locked = false;
    private final LinkedHashMap<Integer, Long> registerRequestIdsTime = new LinkedHashMap<Integer, Long>();
    private final String[] messagesToObserve = new String[] { "PlanControl", "PlanControlState", "VehicleState",
            "PlanDB", "LblConfig" };

    public PlanControlPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    @Override
    public String[] getObservedMessages() {
        return messagesToObserve;
    }

    private void initialize() {
        initializeActions();

        removeAll();
        setSize(new Dimension(255, 60));
        setLayout(new BorderLayout());
        // FIXI18N
        titleLabel = new JLabel(I18n.text(getName()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        add(titleLabel, BorderLayout.NORTH);
        holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.LINE_AXIS));
        add(holder, BorderLayout.CENTER);
        
        planIdLabel = new JLabel("");
        add(planIdLabel, BorderLayout.SOUTH);

        selectionButton = new ToolbarButton(selectionAction);
        sendAcousticsButton = new ToolbarButton(sendAcousticsAction);
        sendUploadPlanButton = new ToolbarButton(sendUploadPlanAction);
        sendDownloadPlanButton = new ToolbarButton(sendDownloadPlanAction);
        sendStartButton = new ToolbarButton(sendStartAction);
        sendStartButton.setActionCommand(startPlanStr);
        sendStopButton = new ToolbarButton(sendStopAction);
        sendStopButton.setActionCommand(stopPlanStr);
        teleOpButton = new ToolbarButton(teleOpAction);
        teleOpButton.setActionCommand(startTeleOperationStr);

        holder.add(selectionButton);
        // holder.add(sendNavStartPointButton);
        holder.add(sendAcousticsButton);
        holder.add(sendUploadPlanButton);
        // holder.add(sendDownloadPlanButton);
        holder.add(sendStartButton);
        holder.add(sendStopButton);
        holder.add(teleOpButton);

        setModeComponentsVisibility();
    }

    /**
     * Parameter {@link #fontMultiplier} validator.
     * 
     * @param value
     * @return
     */
    public String validateFontMultiplier(int value) {
        if (value <= 0)
            return I18n.text("Values lower than zero are not valid!");
        if (value > 10)
            return I18n.text("Values bigger than 10 are not valid!");
        return null;
    }

    /**
     * 
     */
    private void setModeComponentsVisibility() {
        for (Component comp : holder.getComponents()) {
            comp.setVisible(useFullMode);
        }
        titleLabel.setVisible(useFullMode);
        teleOpButton.setVisible(true);
        selectionButton.setVisible(enableSelectionButton && useFullMode);
        sendAcousticsButton.setVisible(enableBeaconsButton && useFullMode);
    }

    /**
     * 
     */
    private void initializeActions() {
        selectionAction = new SystemsSelectionAction(I18n.text("Using") + ":", 20);

        sendAcousticsAction = new AbstractAction(sendAcousticBeaconsStr, ICON_BEACONS) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendAcousticsButton.setEnabled(false);
                            boolean sendBlancTransponders = (ev.getModifiers() & ActionEvent.CTRL_MASK) != 0;
                            sendAcoustics(sendBlancTransponders,
                                    getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        sendAcousticsButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendUploadPlanAction = new AbstractAction(sendSelectedPlanStr, ICON_UP) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendUploadPlanButton.setEnabled(false);
                            boolean verifyAllManeuversUsed = false;
                            if (allwaysVerifyAllManeuversUsed
                                    || (ev.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK)
                                verifyAllManeuversUsed = true;
                            sendPlan(verifyAllManeuversUsed,
                                    getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        sendUploadPlanButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendDownloadPlanAction = new AbstractAction(downloadActivePlanStr, ICON_DOWN_R) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendDownloadPlanButton.setEnabled(false);
                            sendDownLoadPlan(getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        sendDownloadPlanButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendStartAction = new AbstractAction(startPlanStr, ICON_START) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendStartButton.setEnabled(false);
                            sendStartPlan(getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        sendStartButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendStopAction = new AbstractAction(stopPlanStr, ICON_STOP) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendStopButton.setEnabled(false);
                            sendStopPlan(getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        sendStopButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        teleOpAction = new AbstractAction(startTeleOperationStr, ICON_TELEOP_ON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Object action = getValue(Action.NAME);
                NeptusLog.action().info(action);
                String[] systems = getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(e));

                if (testAndShowWarningForNoSystemSelection(systems))
                    return;

                if (startTeleOperationStr.equalsIgnoreCase(e.getActionCommand())) {
                    Teleoperation teleop = new Teleoperation();

                    int reqId = IMCSendMessageUtils.getNextRequestId();
                    PlanControl pc = new PlanControl();
                    pc.setType(PlanControl.TYPE.REQUEST);
                    pc.setOp(PlanControl.OP.START);
                    pc.setRequestId(reqId);
                    pc.setPlanId("teleoperation-mode");
                    pc.setFlags(0);
                    pc.setArg(teleop);

                    boolean ret = IMCSendMessageUtils.sendMessage(pc,
                            (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                            createDefaultMessageDeliveryListener(),
                            PlanControlPanel.this,
                            I18n.text("Error Initializing Tele-Operation"), DONT_USE_ACOUSTICS,
                            "", false, true, systems);
                    if (!ret) {
//                        GuiUtils.errorMessage(PlanControlPanel.this, I18n.text("Tele-Op"),
//                                I18n.text("Error sending Tele-Operation message!"));
                        post(Notification.error(I18n.text("Tele-Operation"),
                                I18n.text("Error sending Tele-Operation message!")));
                    }
                    else {
                        registerPlanControlRequest(reqId);
                    }
                }
                else {
                    boolean ret = IMCSendMessageUtils.sendMessage(new TeleoperationDone(), 
                            (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                            createDefaultMessageDeliveryListener(),
                            PlanControlPanel.this,
                            I18n.text("Error sending exiting Tele-Operation message!"), DONT_USE_ACOUSTICS,
                            "", false, true, systems);
                    if (!ret) {
//                        GuiUtils.errorMessage(PlanControlPanel.this, I18n.text("Tele-Op"),
//                                I18n.text("Error sending Tele-Operation message!"));
                        post(Notification.error(I18n.text("Tele-Op"),
                                I18n.text("Error sending exiting Tele-Operation message!")));
                    }
                }
            }
        };
    }

    private MessageDeliveryListener createDefaultMessageDeliveryListener() {
        return (!useTcpToSendMessages || false ? null : new MessageDeliveryListener() {

            private String  getDest(IMCMessage message) {
                ImcSystem sys = message != null ? ImcSystemsHolder.lookupSystem(message.getDst()) : null;
                String dest = sys != null ? sys.getName() : I18n.text("unknown destination");
                return dest;
            }
            
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery destination unreacheable",
                                message.getAbbrev(), getDest(message))));
            }
            
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery timeout",
                                message.getAbbrev(), getDest(message))));
            }
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery error. (%error)",
                                message.getAbbrev(), getDest(message), error)));
            }
            
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
//                post(Notification.info(
//                        I18n.text("Delivering Message"),
//                        I18n.textf("Message %messageType to %destination delivery uncertain",
//                                message.getAbbrev(), getDest(message))));
            }
            
            @Override
            public void deliverySuccess(IMCMessage message) {
//                post(Notification.success(
//                        I18n.text("Delivering Message"),
//                        I18n.textf("Message %messageType to %destination delivery success",
//                                message.getAbbrev(), getDest(message))));
            }
        });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {
        // addMenuItems();
        setModeComponentsVisibility();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (fontMultiplier < 1)
            return;
        
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        planIdLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        setModeComponentsVisibility();
        this.revalidate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#mainVehicleChange(java.lang.String)
     */
    @Override
    public void mainVehicleChangeNotification(String id) {
        update();
        refreshUI();
        // for (String msgStr : getObservedMessages()) {
        // IMCMessage msg = getConsole().getImcState().get(msgStr);
        // if (msg != null)
        // messageArrived(msg);
        // }
    }

    /**
     * 
     */
    private void refreshUI() {
        boolean bEnable = true;
        if (isLocked())
            bEnable = false;
        if (bEnable != sendAcousticsButton.isEnabled()) {
            for (Component comp : holder.getComponents()) {
                comp.setEnabled(bEnable);
            }
        }

        // if ("INITIALIZING".equalsIgnoreCase(state) || "EXECUTING".equalsIgnoreCase(state)) {
        // sendStartButton.setActionCommand("Stop Plan");
        // sendStartButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_STOP);
        // sendStartButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Stop Plan");
        // }
        // else {
        // sendStartButton.setActionCommand("Start Plan");
        // sendStartButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_START);
        // sendStartButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Start Plan");
        // }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.LockableSubPanel#lock()
     */
    @Override
    public void lock() {
        locked = true;
        refreshUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.LockableSubPanel#unLock()
     */
    @Override
    public void unLock() {
        locked = false;
        refreshUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.LockableSubPanel#isLocked()
     */
    @Override
    public boolean isLocked() {
        return locked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    private short requestsCleanupFlag = 0;

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        ImcSystem tmp = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        if (tmp != null) {
            refreshUI();
        }

        requestsCleanupFlag++;
        if (requestsCleanupFlag > 20 * 5) {
            requestsCleanupFlag = 0;
            try {
                for (Integer key : registerRequestIdsTime.keySet().toArray(new Integer[0])) {
                    if (System.currentTimeMillis() - registerRequestIdsTime.get(key) > 10000)
                        registerRequestIdsTime.remove(key);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private String[] getSystemsToSendTo(boolean clearSelection) {
        if (sendToMainOrSelection())
            return new String[] { getMainVehicleId() };
        else {
            Vector<String> selectedSystems = getSelectedSystems(clearSelection);
            return selectedSystems.toArray(new String[selectedSystems.size()]);
        }
    }

    private boolean sendToMainOrSelection() {
        if (selectionAction.getSelectionType() == SelectionType.MAIN)
            return true;
        return false;
    }

    /**
     * @param clearSelection
     * 
     */
    private Vector<String> getSelectedSystems(boolean clearSelection) {
        Vector<SystemsList> sysLst = getConsole().getSubPanelsOfClass(SystemsList.class);
        Vector<String> selectedSystems = new Vector<String>();
        for (SystemsList systemsList : sysLst)
            selectedSystems.addAll(systemsList.getSelectedSystems(clearSelection));
        return selectedSystems;
    }

    private boolean sendAcoustics(boolean sendBlancTranspondersList, String... systems) {
        if (!checkConditionToRun(this, true, false))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        MissionType miss = getConsole().getMission();

        LinkedList<TransponderElement> transpondersList = new LinkedList<TransponderElement>();

        if (!sendBlancTranspondersList) {
            LinkedHashMap<String, MapMission> mapList = miss.getMapsList();
            for (MapMission mpm : mapList.values()) {
                LinkedHashMap<String, TransponderElement> transList = mpm.getMap().getTranspondersList();
                for (TransponderElement tmp : transList.values()) {
                    transpondersList.add(tmp);
                }
            }
            
            TransponderElement[] selTransponders = getSelectedTransponderElementsFromExternalComponents();
            if (selTransponders.length > 0 && selTransponders.length < transpondersList.size()) {
                String beaconsToSend = "";
                boolean b = true;
                for (TransponderElement tElnt : selTransponders) {
                    beaconsToSend += b ? "" : ", ";
                    beaconsToSend += tElnt.getName();
                }
                int resp = GuiUtils.confirmDialog(SwingUtilities.windowForComponent(this),
                        I18n.text("LBL Beacons"),
                        I18n.textf("Are you sure that you want to override the existing LBL configuration with solely %beaconsToSend?",
                        beaconsToSend));
                if (resp == JOptionPane.YES_OPTION) {
                    transpondersList.clear();
                    transpondersList.addAll(Arrays.asList(selTransponders));
                }
            }
        }

        // For new LBL Beacon Configuration
        Vector<LblBeacon> lblBeaconsList = new Vector<LblBeacon>();

        if (!sendBlancTranspondersList) {
            for (int i = 0; i < transpondersList.size(); i++) {
                TransponderElement transp = transpondersList.get(i);
                LblBeacon msgLBLBeaconSetup = TransponderUtils.getTransponderAsLblBeaconMessage(transp);
                if (msgLBLBeaconSetup == null) {
                    post(Notification.error(sendAcousticsButton.getName(),
                            I18n.textf("Bad configuration parsing for transponder %transponderid!", transp.getId())));
                    return false;
                }
                lblBeaconsList.add(msgLBLBeaconSetup);
            }
        }

        if (lblBeaconsList.size() > 0) {
            // Let us order the beacons in alphabetic order (case insensitive)
            Collections.sort(lblBeaconsList, new Comparator<LblBeacon>() {
                @Override
                public int compare(LblBeacon o1, LblBeacon o2) {
                    return o1.getBeacon().compareTo(o2.getBeacon());
                }
            });
        }
        LblConfig msgLBLConfiguration = new LblConfig();
        msgLBLConfiguration.setOp(LblConfig.OP.SET_CFG);
        msgLBLConfiguration.setBeacons(lblBeaconsList);

        IMCSendMessageUtils.sendMessage(msgLBLConfiguration, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP
                : null), createDefaultMessageDeliveryListener(), this, I18n.text("Error sending acoustic beacons"),
                DONT_USE_ACOUSTICS, acousticOpServiceName, acousticOpUseOnlyActive, true, systems);
        
        final String[] dest = systems;
        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    Thread.sleep(1000);
                    LblConfig msgLBLConfiguration = new LblConfig();
                    msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);

                    IMCSendMessageUtils.sendMessage(msgLBLConfiguration, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP
                            : null), createDefaultMessageDeliveryListener(), PlanControlPanel.this, I18n.text("Error sending acoustic beacons"),
                            DONT_USE_ACOUSTICS, acousticOpServiceName, acousticOpUseOnlyActive, true, dest);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        sw.run();

        if (transpondersList.size() > 0) {
            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            miss.asZipFile(missionlog, true);
        }

        return true;
    }

    /**
     * @param verifyAllManeuversUsed
     * 
     */
    private boolean sendPlan(boolean verifyAllManeuversUsed, String... systems) {
        if (!checkConditionToRun(this, true, true))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        PlanType[] plans = getSelectedPlansFromExternalComponents();
        PlanType plan = plans[0];

        try {
            if (verifyAllManeuversUsed)
                plan.validatePlan();
        }
        catch (Exception e) {
//            GuiUtils.errorMessage(getConsole(), e);
            post(Notification.error(I18n.text("Send Plan"),
                    e.getMessage()));
            return false;
        }

        IMCMessage planSpecificationMessage = IMCUtils.generatePlanSpecification(plan);
        if (planSpecificationMessage == null) {
//            GuiUtils.errorMessage(this, I18n.text("Send Plan"),
//                    I18n.text("Error sending plan message!\nNo plan spec. valid!"));
            post(Notification.error(I18n.text("Send Plan"),
                    I18n.text("Error sending plan message!\nNo plan spec. valid!")));
        }

        int reqId = IMCSendMessageUtils.getNextRequestId();

        PlanDB pdb = new PlanDB();
        pdb.setType(PlanDB.TYPE.REQUEST);
        pdb.setOp(OP.SET);
        pdb.setRequestId(reqId);
        pdb.setPlanId(plan.getId());
        pdb.setArg(planSpecificationMessage);

        pdb.setInfo("Plan sent by Neptus version " + ConfigFetch.getNeptusVersion());
        registerPlanControlRequest(reqId);

        boolean ret = IMCSendMessageUtils.sendMessage(pdb, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                createDefaultMessageDeliveryListener(), this, I18n.text("Error sending plan"), DONT_USE_ACOUSTICS,
                acousticOpServiceName, acousticOpUseOnlyActive, true, systems);

        if (ret) {
            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            getConsole().getMission().asZipFile(missionlog, true);
        }

        return true;
    }

    /**
     * @return
     * 
     */
    private boolean sendDownLoadPlan(String... systems) {
        if (!checkConditionToRun(this, true, false))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
        planControlMessage.setValue("type", 0); // REQUEST

        planControlMessage.setValue("op", "GET");

        int reqId = IMCSendMessageUtils.getNextRequestId();
        planControlMessage.setValue("request_id", reqId);

        // planControlMessage.setValue("plan_id", plan.getId());

        // boolean ret = sendTheMessage(planControlMessage, "Error sending plan download request", systems);
        boolean ret = IMCSendMessageUtils.sendMessage(planControlMessage,
                (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null), 
                createDefaultMessageDeliveryListener(), this,
                I18n.text("Error sending plan download request"), DONT_USE_ACOUSTICS,
                acousticOpServiceName, acousticOpUseOnlyActive, true, systems);

        if (ret)
            registerPlanControlRequest(reqId);

        return ret;
    }

    private boolean sendStartPlan(String... systems) {
        return sendStartStop(PlanControl.OP.START, systems);
    }

    private boolean sendStopPlan(String... systems) {
        return sendStartStop(PlanControl.OP.STOP, systems);
    }

    private boolean sendStartStop(PlanControl.OP cmd, String... systems) {
        if (!checkConditionToRun(this, cmd == PlanControl.OP.START ? false : true, cmd == PlanControl.OP.STOP ? false
                : true))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;
        
        int reqId = IMCSendMessageUtils.getNextRequestId();
        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setRequestId(reqId);
        String cmdStrMsg = "";
        try {
            switch (cmd) {
                case START:
                    cmdStrMsg += I18n.text("Error sending start plan");
                    PlanType[] plans = getSelectedPlansFromExternalComponents();
                    PlanType plan = plans[0];

                    if (!verifyIfPlanIsInSyncOnTheSystem(plan, systems)) {
                        if (systems.length == 1)
                            post(Notification.error(I18n.text("Send Start Plan"),"Plan not in sync on system!"));
                        else
                            post(Notification.error(I18n.text("Send Start Plan"),"Plan not in sync on systems!"));

                        return false;
                    }

                    pc.setPlanId(plan.getId());
                    if (useCalibrationOnStartPlan)
                        pc.setFlags(PlanControl.FLG_CALIBRATE);
                    break;
                case STOP:
                    cmdStrMsg += I18n.text("Error sending stopping plan");
                    break;
                default:
                    return false;
            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }
        pc.setOp(cmd);
        
        boolean dontSendByAcoustics = DONT_USE_ACOUSTICS;
        if (cmd == PlanControl.OP.START) {
            String planId = pc.getPlanId();
            if (planId.length() == 1) {
                dontSendByAcoustics = USE_ACOUSTICS;
            }
        }
        
        boolean ret = IMCSendMessageUtils.sendMessage(pc, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                createDefaultMessageDeliveryListener(), this, cmdStrMsg, dontSendByAcoustics,
                acousticOpServiceName, acousticOpUseOnlyActive, true, systems);

        if (!ret) {
//            GuiUtils.errorMessage(this, I18n.text("Send Plan"), I18n.text("Error sending PlanControl message!"));
            post(Notification.error(I18n.text("Send Plan"), I18n.text("Error sending PlanControl message!")));
            return false;
        }
        else {
            registerPlanControlRequest(reqId);
        }

        return true;
    }

    /**
     * @param plan
     * @param systems
     * @return
     */
    private boolean verifyIfPlanIsInSyncOnTheSystem(PlanType plan, String... systems) {
        boolean planInSync = true;
        String systemsNotInSync = "";
        for (String sysStr : systems) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sysStr);
            if (sys == null)
                continue;
            PlanDBState prs = sys.getPlanDBControl().getRemoteState();
            if (prs == null || !prs.matchesRemotePlan(plan)) {
               planInSync = false;
               systemsNotInSync += (systemsNotInSync.length() > 0 ? ", " : "") + sysStr;
            }
        }
        if (!planInSync) {//Synchronized 
            int resp = GuiUtils.confirmDialog(SwingUtilities.windowForComponent(this),
                    I18n.text("Plan not synchronized"),
                    I18n.textf("The plan '%plan' is not synchronized on %system.\nYou should resend the plan.\nDo you still want to start the plan?",
                    plan.getId(), systemsNotInSync));
            planInSync = (resp == JOptionPane.YES_OPTION);
        }
        
        return planInSync;
    }


    /**
     * @param reqId
     */
    private void registerPlanControlRequest(int reqId) {
        registerRequestIdsTime.put(reqId, System.currentTimeMillis());
    }

    /**
     * @param component
     * @param checkMission
     * @param checkPlan
     * @return
     */
    protected boolean checkConditionToRun(Component component, boolean checkMission, boolean checkPlan) {
        if (!ImcMsgManager.getManager().isRunning()) {
//            GuiUtils.errorMessage(this, component.getName(), I18n.text("IMC comms. are not running!"));
            post(Notification.error(component.getName(), I18n.text("IMC comms. are not running!")));
            return false;
        }

        ConsoleLayout cons = getConsole();
        if (cons == null) {
//            GuiUtils.errorMessage(this, component.getName(), I18n.text("Missing console attached!"));
            post(Notification.error(component.getName(), I18n.text("Missing console attached!")));
            return false;
        }

        if (checkMission) {
            MissionType miss = cons.getMission();
            if (miss == null) {
//                GuiUtils.errorMessage(this, component.getName(), I18n.text("Missing attached mission!"));
                post(Notification.error(component.getName(), I18n.text("Missing attached mission!")));
                return false;
            }
        }

        if (checkPlan) {
            PlanType[] plans = getSelectedPlansFromExternalComponents();
            if (plans == null || plans.length == 0) {
//                GuiUtils.errorMessage(this, component.getName(), I18n.text("Missing attached plan!"));
                post(Notification.error(component.getName(), I18n.text("Missing attached plan!")));
                return false;
            }
        }
        return true;
    }

    private PlanType[] getSelectedPlansFromExternalComponents() {
        if (getConsole() == null)
            return new PlanType[0];
        Vector<IPlanSelection> psel = getConsole().getSubPanelsOfInterface(IPlanSelection.class);
        if (psel.size() == 0) {
            if (getConsole().getPlan() != null)
                return new PlanType[] { getConsole().getPlan() };
            else
                return new PlanType[0];
        }
        else {
            Vector<PlanType> vecPlans = psel.get(0).getSelectedPlans();
            return vecPlans.toArray(new PlanType[vecPlans.size()]);
        }
    }

    private TransponderElement[] getSelectedTransponderElementsFromExternalComponents() {
        if (getConsole() == null)
            return new TransponderElement[0];
        Vector<ITransponderSelection> psel = getConsole().getSubPanelsOfInterface(ITransponderSelection.class);
        Collection<TransponderElement> vecTrans = psel.get(0).getSelectedTransponders();
        return vecTrans.toArray(new TransponderElement[vecTrans.size()]);
    }

    /**
     * @param systems
     */
    private boolean testAndShowWarningForNoSystemSelection(String... systems) {
        if (systems.length < 1) {
            // getConsole().warning(this.getName() + ": " + I18n.text("No systems selected to send to!"));
            return true;
        }
        return false;
    }

    int lastTeleopState = 0;

    private String convertTimeSecondsToFormatedStringMillis(double timeSeconds) {
        String tt = "";
        if (timeSeconds < 60)
            tt = MathMiscUtils.parseToEngineeringNotation(timeSeconds, 3) + "s";
        else
            tt = DateTimeUtil.milliSecondsToFormatedString((long) (timeSeconds * 1000.0));
        return tt;
    }
    
    @Override
    public void messageArrived(IMCMessage message) {

        // message.dump(System.out);

        switch (message.getMgid()) {
            case PlanControl.ID_STATIC:
                PlanControl msg = (PlanControl) message;
                try {
                    PlanControl.TYPE type = msg.getType();
                    if (type != PlanControl.TYPE.IN_PROGRESS) {
                        int reqId = msg.getRequestId();
                        if (registerRequestIdsTime.containsKey(reqId)) {
                            boolean cleanReg = false;

                            if (type == TYPE.SUCCESS) {
                                cleanReg = true;
                            }
                            else if (type == TYPE.FAILURE) {
                                cleanReg = true;
                                long requestTimeMillis = registerRequestIdsTime.get(reqId);
                                String utcStr = " " + I18n.text("UTC");
                                double deltaTime = (msg.getTimestampMillis() - requestTimeMillis) / 1E3;
                                post(Notification.error(I18n.text("Plan Control Error"),
                                                I18n.textf("The following error arrived at @%timeArrived for a request @%timeRequested (\u2206t %deltaTime): %msg",
                                                        DateTimeUtil.timeFormaterNoMillis2UTC.format(msg.getDate())
                                                                + utcStr,
                                                        DateTimeUtil.timeFormaterNoMillis2UTC.format(new Date(
                                                                requestTimeMillis)) + utcStr, deltaTime < 0 ? "-"
                                                                : convertTimeSecondsToFormatedStringMillis(deltaTime),
                                                        msg.getInfo())).src(
                                                ImcSystemsHolder.translateImcIdToSystemName(msg.getSrc())));
                            }
                            if (cleanReg)
                                registerRequestIdsTime.remove(reqId);
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                break;
            case VehicleState.ID_STATIC:
                VehicleState vstate = (VehicleState) message;

                OP_MODE mode = vstate.getOpMode();
                int manType = vstate.getManeuverType();

                int teleopState = new String(mode.hashCode() + "," + manType).hashCode();

                if (teleopState != lastTeleopState) {
                    if (manType == teleoperationManeuver && mode == OP_MODE.MANEUVER) {
                        teleOpButton.setActionCommand(stopTeleOperationStr);
                        teleOpButton.setIcon(ICON_TELEOP_OFF);
                        teleOpButton.setToolTipText(I18n.text(stopTeleOperationStr));
                    }
                    else {
                        teleOpButton.setActionCommand(startTeleOperationStr);
                        teleOpButton.setIcon(ICON_TELEOP_ON);
                        teleOpButton.setToolTipText(startTeleOperationStr);
                    }
                }
                lastTeleopState = teleopState;
                break;
            case PlanDB.ID_STATIC:
                PlanDB planDb = (PlanDB) message;
                try {
                    PlanDB.TYPE type = planDb.getType();

                    if (type != PlanDB.TYPE.IN_PROGRESS) {
                        int reqId = planDb.getRequestId();
                        if (registerRequestIdsTime.containsKey(reqId)) {
                            // Date date = new Date(registerRequestIdsTime.get(reqId));
                            boolean cleanReg = false;
                            if (type == PlanDB.TYPE.SUCCESS) {
                                cleanReg = true;
                            }
                            else if (type == PlanDB.TYPE.FAILURE) {
                                cleanReg = true;
                                long requestTimeMillis = registerRequestIdsTime.get(reqId);
                                String utcStr = " " + I18n.text("UTC");
                                double deltaTime = (planDb.getTimestampMillis() - requestTimeMillis) / 1E3;
                                post(Notification.error(I18n.text("Plan DB Error"),
                                                I18n.textf("The following error arrived at @%timeArrived for a request @%timeRequested (\u2206t %deltaTime): %msg",
                                                        DateTimeUtil.timeFormaterNoMillis2UTC.format(planDb.getDate())
                                                                + utcStr,
                                                        DateTimeUtil.timeFormaterNoMillis2UTC.format(new Date(
                                                                requestTimeMillis)) + utcStr, deltaTime < 0 ? "-"
                                                                : convertTimeSecondsToFormatedStringMillis(deltaTime),
                                                        planDb.getInfo())).src(
                                                ImcSystemsHolder.translateImcIdToSystemName(planDb.getSrc())));
                            }
                            if (cleanReg)
                                registerRequestIdsTime.remove(reqId);
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e, e);
                }
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
