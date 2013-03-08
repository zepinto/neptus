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
 * 10 de Jul de 2011
 * $Id:: RemotePlanControl.java 10012 2013-02-21 14:23:45Z pdias                $:
 */
package pt.up.fe.dceg.neptus.plugins.webupdate;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.comm.proxy.ProxyInfoProvider;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.IPlanSelection;
import pt.up.fe.dceg.neptus.console.plugins.LockableSubPanel;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.system.btn.SystemsSelectionAction;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author pdias
 * 
 */
@PluginDescription(name = "Remote Plan Control", author = "Paulo Dias", version = "0.1")
public class RemotePlanControl extends SimpleSubPanel implements ConfigurationListener, MainVehicleChangeListener,
        LockableSubPanel, IPeriodicUpdates, NeptusMessageListener {

    private static final long serialVersionUID = 1L;
    private final ImageIcon ICON_UP = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/up.png");
    // private final ImageIcon ICON_DOWN_R = ImageUtils
    // .getIcon("pt/up/fe/dceg/neptus/plugins/planning/fileimport.png");
    private final ImageIcon ICON_START = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/stop.png");

    @NeptusProperty(name = "Publish web address")
    public String pubURL = "http://whale.fe.up.pt/neptleaves/";

    @NeptusProperty(name = "Font Size Multiplier", description = "The font size. Use '1' for default.")
    public int fontMultiplier = 1;

    @NeptusProperty(name = "Verify plans for island nodes", description = "Always runs a verification "
            + "on the plan for maneuvers that have no input edges. If you choose to switch off here "
            + "you can allways click Alt whan sending the plan that this verification will run.")
    public boolean allwaysVerifyAllManeuversUsed = true;

    private DefaultHttpClient client;
    private PoolingClientConnectionManager httpConnectionManager;

    // GUI
    private JXPanel holder;
    private JLabel titleLabel;
    private JXLabel planIdLabel;
    private Font planIdLabelFont;
    private ToolbarButton sendUploadPlanButton, sendStartButton, sendStopButton;

    private AbstractAction sendUploadPlanAction, sendStartAction, sendStopAction;

    private boolean locked = false;
    private Integer requestId = 0xFFFF;
    private LinkedHashMap<Integer, Long> registerRequestIdsTime = new LinkedHashMap<Integer, Long>();
    private String[] messagesToObserve = new String[] { "PlanControl", "PlanControlState" };

    // private String state = "";

    /**
     * 
     */
    public RemotePlanControl(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return messagesToObserve;
    }

    /**
     * 
     */
    private void initialize() {
        initializeComm();
        initializeActions();

        removeAll();
        setSize(new Dimension(255, 60));
        setLayout(new BorderLayout());
        titleLabel = new JLabel(PluginUtils.getPluginName(this.getClass()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        add(titleLabel, BorderLayout.NORTH);
        holder = new JXPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.LINE_AXIS));
        add(holder, BorderLayout.CENTER);
        planIdLabel = new JXLabel("");
        planIdLabelFont = planIdLabel.getFont();
        planIdLabel.setFont(planIdLabelFont.deriveFont((float) (planIdLabelFont.getSize() * fontMultiplier)));
        add(planIdLabel, BorderLayout.SOUTH);

        holder.setOpaque(false);
        setBackground(new Color(255, 255, 110));

        sendUploadPlanButton = new ToolbarButton(sendUploadPlanAction);
        sendStartButton = new ToolbarButton(sendStartAction);
        sendStartButton.setActionCommand("Start Plan");
        sendStopButton = new ToolbarButton(sendStopAction);
        sendStopButton.setActionCommand("Stop Plan");

        holder.add(sendUploadPlanButton);
        holder.add(sendStartButton);
        holder.add(sendStopButton);
    }

    private void initializeComm() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
        httpConnectionManager.setMaxTotal(4);
        httpConnectionManager.setDefaultMaxPerRoute(50);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 5000);
        client = new DefaultHttpClient(httpConnectionManager, params);

        ProxyInfoProvider.setRoutePlanner((AbstractHttpClient) client);
    }

    /**
     * 
     */
    private void initializeActions() {
        sendUploadPlanAction = new AbstractAction("Send Selected Plan", ICON_UP) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent ev) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendUploadPlanButton.setEnabled(false);
                        boolean verifyAllManeuversUsed = false;
                        if (allwaysVerifyAllManeuversUsed
                                || (ev.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK)
                            verifyAllManeuversUsed = true;
                        sendPlan(verifyAllManeuversUsed,
                                getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
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

        sendStartAction = new AbstractAction("Start Plan", ICON_START) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent ev) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendStartButton.setEnabled(false);
                        sendStartStop(0, getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
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

        sendStopAction = new AbstractAction("Stop Plan", ICON_STOP) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(final ActionEvent ev) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendStopButton.setEnabled(false);
                        sendStartStop(1, getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
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
    }

    /**
     * @return the next requestId
     */
    private int getNextRequestId() {
        synchronized (requestId) {
            ++requestId;
            if (requestId > 0xFFFF)
                requestId = 0;
            if (requestId < 0)
                requestId = 0;
            return requestId;
        }
    }

    @Override
    public void propertiesChanged() {
        if (fontMultiplier < 1)
            return;
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        planIdLabel.setFont(planIdLabelFont.deriveFont((float) (planIdLabelFont.getSize() * fontMultiplier)));
        this.revalidate();
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        // mainVehicleMessageListener.setSystemToListenStrings(id);
        // setReportedState(EmergencyStateEnum.UNKNOWN);
        // setReportedPlanId("");
        // setReportedCommLevel((short) -1);
        // state = "";
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
        if (bEnable != sendUploadPlanButton.isEnabled()) {
            for (Component comp : holder.getComponents()) {
                comp.setEnabled(bEnable);
            }
        }

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
        return 5000;
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
        if (requestsCleanupFlag > 20) {
            requestsCleanupFlag = 0;
            try {
                for (Integer key : registerRequestIdsTime.keySet()) {
                    if (System.currentTimeMillis() - registerRequestIdsTime.get(key) > 10000)
                        registerRequestIdsTime.remove(key);
                }
            }
            catch (Exception e) {
            }
        }
        return true;
    }

    private String[] getSystemsToSendTo(boolean clearSelection) {
        return new String[] { getMainVehicleId() };
    }

    private boolean sendPlan(boolean verifyAllManeuversUsed, String... systems) {
        if (!checkConditionToRun(this, true, true))
            return false;

        PlanType[] plans = getSelectedPlansFromExternalComponents();
        PlanType plan = plans[0];

        try {
            if (verifyAllManeuversUsed) {
                // getConsole().getPlan().verifyAllManeuversUsed();
                plan.validatePlan();
            }
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
            return false;
        }

        IMCMessage planSpecificationMessage = getPlanAsSpecification(plan);
        if (planSpecificationMessage == null) {
            GuiUtils.errorMessage(this, "Send Plan", "Error sending plan message!\n" + "No plan spec. valid!");
        }

        IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
        planControlMessage.setValue("type", 0); // REQUEST

        planControlMessage.setValue("op", "LOAD");

        int reqId = getNextRequestId();
        planControlMessage.setValue("request_id", reqId);

        planControlMessage.setValue("plan_id", plan.getId());

        planControlMessage.setValue("arg", planSpecificationMessage);
        planControlMessage.setValue("info", "");

        planSpecificationMessage = planSpecificationMessage.cloneMessage();
        boolean ret = sendTheMessage(planSpecificationMessage, "Error sending plan", systems);
        if (!ret) {
            GuiUtils.errorMessage(this, "Send Plan Spec", "Error sending PlanSpecification message!");
            return false;
        }

        ret = sendTheMessage(planControlMessage, "Error sending plan", systems);
        if (!ret) {
            GuiUtils.errorMessage(this, "Send Plan", "Error sending PlanControl message!");
            return false;
        }
        else {
            registerPlanControlRequest(reqId);

            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            getConsole().getMission().asZipFile(missionlog, true);
        }

        return true;
    }

    private boolean sendStartStop(int cmd, String... systems) {
        if (!checkConditionToRun(this, true, true))
            return false;

        IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
        planControlMessage.setValue("type", 0); // REQUEST
        int reqId = getNextRequestId();
        planControlMessage.setValue("request_id", reqId);

        String opEnumerated = planControlMessage.getString("op");
        String cmdStrMsg = "Error ";
        try {
            switch (cmd) {
                case 0:
                    cmdStrMsg += "sending start plan";
                    try {
                        opEnumerated = "START";
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(this, ex);
                        return false;
                    }
                    PlanType[] plans = getSelectedPlansFromExternalComponents();
                    PlanType plan = plans[0];
                    planControlMessage.setValue("plan_id", plan.getId());
                    LinkedHashMap<String, Boolean> flagsBitmask = planControlMessage.getBitmask("flags");
                    try {
                        flagsBitmask.put("CALIBRATE", true);
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(this, ex);
                        return false;
                    }
                    planControlMessage.setValue("flags", flagsBitmask);
                    break;
                case 1:
                    cmdStrMsg += "sending stopping plan";
                    try {
                        opEnumerated = "STOP";
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(this, ex);
                        return false;
                    }
                    break;
                default:
                    return false;
                    // break;
            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }
        planControlMessage.setValue("op", opEnumerated);

        boolean ret = sendTheMessage(planControlMessage, cmdStrMsg, systems);
        if (!ret) {
            GuiUtils.errorMessage(this, "Send Plan", "Error sending PlanControl message!");
            return false;
        }
        else {
            registerPlanControlRequest(reqId);

            // String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            // getConsole().getMission().asZipFile(missionlog, true);
        }

        return true;
    }

    /**
     * @param reqId
     */
    private void registerPlanControlRequest(int reqId) {
        registerRequestIdsTime.put(reqId, System.currentTimeMillis());
    }

    /**
     * @param planType
     * @return
     */
    private IMCMessage getPlanAsSpecification(PlanType plan) {
        if (plan == null || !(plan instanceof PlanType))
            return null;

        PlanType iPlan = (PlanType) plan;
        IMCMessage msgPlanSpecification = iPlan.asIMCPlan();

        return msgPlanSpecification;
    }

    /**
     * @param component
     * @param checkMission
     * @param checkPlan
     * @return
     */
    protected boolean checkConditionToRun(Component component, boolean checkMission, boolean checkPlan) {
        if (!ImcMsgManager.getManager().isRunning()) {
            GuiUtils.errorMessage(this, component.getName(), "IMC comms. are not running!");
            return false;
        }

        ConsoleLayout cons = getConsole();
        if (cons == null) {
            GuiUtils.errorMessage(this, component.getName(), "Missing console attached!");
            return false;
        }

        if (checkMission) {
            MissionType miss = cons.getMission();
            if (miss == null) {
                GuiUtils.errorMessage(this, component.getName(), "Missing attached mission!");
                return false;
            }
        }

        if (checkPlan) {
            PlanType[] plans = getSelectedPlansFromExternalComponents();
            if (plans == null || plans.length == 0) {
                GuiUtils.errorMessage(this, component.getName(), "Missing attached plan!");
                return false;
            }
        }
        return true;
    }

    /**
     * 
     */
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
            return psel.get(0).getSelectedPlans().toArray(new PlanType[0]);
        }
    }

    private boolean sendTheMessage(IMCMessage msg, String errorTextForDialog, String... ids) {
        return sendTheMessage(msg, errorTextForDialog, false, ids);
    }

    private boolean sendTheMessage(IMCMessage msg, String errorTextForDialog, boolean ignoreIfActive, String... ids) {

        Vector<IMCMessage> msgs = new Vector<IMCMessage>();
        boolean retAll = true;

        for (String sid : ids) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sid);
            if (sys == null)
                continue;
            msg.getHeader().setValue("src", ImcMsgManager.getManager().getLocalId());
            msg.getHeader().setValue("dst", sys.getId());
            msgs.add(msg);
        }

        retAll = postMessages(msgs);

        if (!retAll) {
            GuiUtils.errorMessage(this, "Send Plan Control Message", errorTextForDialog);
        }
        return retAll;
    }

    protected IMCMessage getPlanAsSpecification() {
        if (!checkConditionToRun(this, true, true))
            return null;
        ConsoleLayout cons = getConsole();
        // MissionType miss = cons.getMission();
        PlanType plan = cons.getPlan();

        if (!(plan instanceof PlanType))
            return null;

        PlanType iPlan = (PlanType) plan;
        return iPlan.asIMCPlan();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#messageArrived(pt.up.fe.dceg.neptus.util.comm.vehicle.IMCMessage
     * )
     */
    @Override
    public void messageArrived(IMCMessage message) {
        if ("PlanControlState".equalsIgnoreCase(message.getAbbrev())) {
            // state = message.getAsString("state");
        }
        else if ("PlanControl".equalsIgnoreCase(message.getAbbrev())) {
            try {
                // FIXME jqcorreia talvez não seja o melhor mecanismo
                Integer typeId = message.getInteger("type"); // REQUEST = 0
                String typeName = message.getString("type");
                if (typeId != 0) {
                    int reqId = (Integer) message.getAsNumber("request_id").intValue();
                    if (registerRequestIdsTime.containsKey(reqId)) {
                        boolean cleanReg = false;
                        if ("SUCCESS".equalsIgnoreCase(typeName)) {
                            cleanReg = true;
                        }
                        else if ("FAILURE".equalsIgnoreCase(typeName)) {
                            cleanReg = true;
                        }
                        if (cleanReg)
                            registerRequestIdsTime.remove(reqId);
                        
                    }
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }

    private boolean postMessages(Collection<IMCMessage> messages) {
        HttpPost post = null;
        try {
            String uri = pubURL + "imc/";
            post = new HttpPost(uri);

            // NameValuePair nvp_type = new BasicNameValuePair("Content-Type",
            // "application/lsf");
            // List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            // nvps.add(nvp_type);
            // post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            for (IMCMessage msg : messages) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IMCOutputStream ios = new IMCOutputStream(baos);
                msg.serialize(ios);
                pos.write(baos.toByteArray());
            }
            pos.flush();
            pos.close();
            // ByteArrayInputStream inStream = new
            // ByteArrayInputStream(sb.getBuffer());
            InputStreamEntity reqEntity = new InputStreamEntity(pis, -1);
            reqEntity.setContentType("application/lsf");
            reqEntity.setChunked(true);

            post.setEntity(reqEntity);

            HttpContext localContext = new BasicHttpContext();
            HttpResponse iGetResultCode = client.execute(post, localContext);
            ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);

            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (post != null) {
                    post.abort();
                }
                return false;
            }
            try {
                // InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            // e.printStackTrace();
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            if (post != null) {
                post.abort();
                post = null;
            }
        }
        return true;
    }

    @Override
    public void cleanSubPanel() {

        if (client != null) {
        }
        if (httpConnectionManager != null) {
            httpConnectionManager.shutdown();
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
