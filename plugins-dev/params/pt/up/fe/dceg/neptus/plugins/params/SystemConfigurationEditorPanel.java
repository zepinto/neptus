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
 * Mar 2, 2013
 * $Id:: SystemConfigurationEditorPanel.java 10065 2013-03-04 12:40:58Z pdias   $:
 */
package pt.up.fe.dceg.neptus.plugins.params;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.EntityParameter;
import pt.up.fe.dceg.neptus.imc.EntityParameters;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.QueryEntityParameters;
import pt.up.fe.dceg.neptus.imc.SetEntityParameters;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.params.SystemProperty.Scope;
import pt.up.fe.dceg.neptus.plugins.params.SystemProperty.Visibility;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageDeliveryListener;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SystemConfigurationEditorPanel extends JPanel implements PropertyChangeListener {

    protected final LinkedHashMap<String, SystemProperty> params = new LinkedHashMap<>();

    protected PropertySheetPanel psp;
    private JButton sendButton;
    private JButton refreshButton;
    private JLabel titleLabel;
    private JCheckBox checkAdvance;
    private JComboBox<Scope> scopeComboBox;
    
    protected boolean refreshing = false;
    private PropertyEditorRegistry per;
    private PropertyRendererRegistry prr;

    private Scope scopeToUse = Scope.GLOBAL;
    private Visibility visibility = Visibility.USER;

    protected String systemId;
    protected ImcSystem sid = null;
    
    protected ImcMsgManager imcMsgManager;
    
    public SystemConfigurationEditorPanel(String systemId, Scope scopeToUse, Visibility visibility,
            boolean showSendButton, boolean showScopeCombo, ImcMsgManager imcMsgManager) {
        this.systemId = systemId;
        this.imcMsgManager = imcMsgManager;
        
        this.scopeToUse = scopeToUse;
        this.visibility = visibility;
        
        initialize(showSendButton, showScopeCombo);
    }
    
    private void initialize(boolean showSendButton, boolean showScopeCombo) {
        setLayout(new MigLayout());

        scopeComboBox = new JComboBox<Scope>(Scope.values()) {
            public void setSelectedItem(Object anObject) {
                super.setSelectedItem(anObject);
            }
            
        };
        scopeComboBox.setRenderer(new ListCellRenderer<Scope>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends Scope> list, Scope value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = new JLabel(I18n.text(value.getText()));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
    
                return label;
            }
        });
        scopeComboBox.setSelectedItem(scopeToUse);
        scopeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                scopeToUse = (Scope) e.getItem();
                new Thread() {
                    @Override
                    public void run() {
                        if (refreshButton != null)
                            refreshButton.doClick(50);
                    }
                }.start();
            }
        });
        
        titleLabel = new JLabel("<html><b>" + createTitle() + "</b></html>");
        add(titleLabel, "w 100%, wrap"); 

        // Configure Property sheet
        psp = new PropertySheetPanel();
        psp.setSortingCategories(true);
        psp.setSortingProperties(false);
        psp.setDescriptionVisible(true);
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        psp.setToolBarVisible(false);
        
        resetPropertiesEditorAndRendererFactories();
        
        add(psp, "w 100%, h 100%, wrap");
        
        sendButton = new JButton(new AbstractAction(I18n.text("Send")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendPropertiesToSystem();
            }
        });
        if (showSendButton)
            add(sendButton, "sg buttons, split");

        refreshButton = new JButton(new AbstractAction(I18n.text("Refresh")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshPropertiesOnPanel();
            }
        });
        add(refreshButton, "sg buttons, split");

        if (showScopeCombo)
            add(scopeComboBox, "split, w :200:");

        
        checkAdvance = new JCheckBox(I18n.text("Access Developer Parameters"));
        if (ConfigFetch.getDistributionType() == DistributionEnum.DEVELOPER)
            add(checkAdvance);
        else
            visibility = Visibility.USER;
        if (visibility == Visibility.DEVELOPER)
            checkAdvance.setSelected(true);
        else
            checkAdvance.setSelected(false);
        checkAdvance.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkAdvance.isSelected())
                    visibility = Visibility.DEVELOPER;
                else
                    visibility = Visibility.USER;
                
                refreshPropertiesOnPanel();
            }
        });
        checkAdvance.setFocusable(false);

        refreshPropertiesOnPanel();
        
        revalidate();
        repaint();
    }

    private void resetPropertiesEditorAndRendererFactories() {
        per = new PropertyEditorRegistry();
        // per.registerDefaults();
        psp.setEditorFactory(per);
        prr = new PropertyRendererRegistry();
        // prr.registerDefaults();
        psp.setRendererFactory(prr);
    }
    
    /**
     * @return the params
     */
    public LinkedHashMap<String, SystemProperty> getParams() {
        return params;
    }
    
    /**
     * @return the systemId
     */
    public String getSystemId() {
        return systemId;
    }
    
    /**
     * @param systemId the systemId to set
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
        sid = ImcSystemsHolder.getSystemWithName(this.systemId);
        refreshPropertiesOnPanel();
    }

    /**
     * @return the refreshing
     */
    public boolean isRefreshing() {
        return refreshing;
    }
    
    /**
     * @param refreshing the refreshing to set
     */
    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }
    
    private synchronized void refreshPropertiesOnPanel() {
        titleLabel.setText("<html><b>" + createTitle() + "</b></html>");
        removeAllPropertiesFromPanel();
        
        resetPropertiesEditorAndRendererFactories();
        
        ArrayList<SystemProperty> pr = ConfigurationManager.INSTANCE.getProperties(systemId, visibility, scopeToUse);
        ArrayList<String> secNames = new ArrayList<>();
        for (SystemProperty sp : pr) {
            String sectionName = sp.getCategoryId();
            String name = sp.getName();
            if (!secNames.contains(sectionName))
                secNames.add(sectionName);
            params.put(sectionName + "." + name, sp);
            sp.addPropertyChangeListener(this);
            psp.addProperty(sp);
            if (sp.getEditor() != null) {
                per.registerEditor(sp, sp.getEditor());
            }
            if (sp.getRenderer() != null) {
                prr.registerRenderer(sp, sp.getRenderer());
            }
        }
        // Let us make sure all dependencies between properties are ok
        for (SystemProperty spCh : params.values()) {
            for (SystemProperty sp : params.values()) {
                PropertyChangeEvent evt = new PropertyChangeEvent(spCh, spCh.getName(), null, spCh.getValue());
                sp.propertyChange(evt);
            }
        }
        for (String sectionName : secNames) {
            queryValues(sectionName, scopeToUse.getText(), visibility.getText());
        }
        
        revalidate();
        repaint();
    }

    private void removeAllPropertiesFromPanel() {
        params.clear();
        for (Property p : psp.getProperties()) {
            psp.removeProperty(p);
        }
    }

    private String createTitle() {
        return I18n.textf("%systemName Parameters", getSystemId());
    }

    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        System.out.println("--------------- " + evt);
        if(!refreshing && evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            sp.setValue(evt.getNewValue());
            
            for (SystemProperty sprop : params.values()) {
                sprop.propertyChange(evt);
            }
            sp.propertyChange(evt);
        }
    }
    
    private void queryValues(String entityName, String scope, String visibility) {
        QueryEntityParameters qep = new QueryEntityParameters();
        qep.setScope(scope);
        qep.setVisibility(visibility);
        qep.setName(entityName);
        send(qep);
    }
    
    private void sendProperty(SystemProperty prop) {
        if (prop.getValue() == null)
            return;
        
        SetEntityParameters setParams = new SetEntityParameters();
        Vector<EntityParameter> v = new Vector<>();
        EntityParameter ep = new EntityParameter();
        
        ep.setName(prop.getName());
        boolean isList = false;
        if (ArrayList.class.equals(prop.getType()))
            isList = true;
        String str = (String) prop.getValue().toString();
        if (isList)
            str = ConfigurationManager.convertArrayListToStringToPropValueString(str);
        ep.setValue(str);
        v.add(ep);
        
        setParams.setName(prop.getCategoryId());
        setParams.setParams(v);
        
        ep.dump(System.out);
        send(setParams);
    }

    private void sendPropertiesToSystem() {
        Set<SystemProperty> sentProps = new LinkedHashSet<SystemProperty>();
        for (SystemProperty sp : params.values()) {
            if (sp.getTimeDirty() > sp.getTimeSync()) {
                sendProperty(sp);
                sentProps.add(sp);
            }
        }
        ArrayList<String> secNames = new ArrayList<>();
        for (SystemProperty sp : sentProps) {
            String sectionName = sp.getCategoryId();
            if (!secNames.contains(sectionName))
                secNames.add(sectionName);
        }        
        for (String sec : secNames) {
            queryValues(sec, scopeToUse.getText(), visibility.getText());
        }
    }

    private void send(IMCMessage msg) {
        MessageDeliveryListener mdl = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
            }
            
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }
            
            @Override
            public void deliveryTimeOut(IMCMessage message) {
            }
            
            @Override
            public void deliverySuccess(IMCMessage message) {
            }
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
            }
        };
        if (sid == null)
            sid = ImcSystemsHolder.getSystemWithName(getSystemId());
        if (sid != null) {
            imcMsgManager.sendReliablyNonBlocking(msg, sid.getId(), mdl);
        }
    }
    
    public static void updatePropertyWithMessageArrived(SystemConfigurationEditorPanel systemConfEditor, IMCMessage message) {
        if (systemConfEditor == null || message == null)
            return;
        
        try {
            systemConfEditor.setRefreshing(true);
            EntityParameters eps = new EntityParameters(message);
            String section = eps.getName();
            for(EntityParameter ep : eps.getParams()) {
                SystemProperty p = systemConfEditor.getParams().get(section + "." + ep.getName());
                if(p == null) {
                    System.out.println("Property not in config: " + section + ep.getName());
                }
                else {
                    boolean isList = false;
//                    System.out.println("Prop type and if is list:: " + p.getType() + " " + (ArrayList.class.equals(p.getType())));
                    if (ArrayList.class.equals(p.getType()))
                        isList = true;
                    //Object value = ConfigurationManager.getValueTypedFromString(ep.getValue(), p.getValueType());
                    Object value = !isList ? ConfigurationManager.getValueTypedFromString(ep.getValue(),
                            p.getValueType()) : ConfigurationManager.getListValueTypedFromString(ep.getValue(),
                            p.getValueType());
                    p.setValue(value);
                    p.setTimeSync(System.currentTimeMillis());
                }
            }
            systemConfEditor.revalidate();
            systemConfEditor.repaint();
            systemConfEditor.setRefreshing(false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        ImcMsgManager.getManager().start();
//        MonitorIMCComms icmm = new MonitorIMCComms(ImcMsgManager.getManager());
//        GuiUtils.testFrame(icmm);
        
        String vehicle = "lauv-xtreme-2";
        
        final SystemConfigurationEditorPanel sc1 = new SystemConfigurationEditorPanel(vehicle, Scope.MANEUVER,
                Visibility.USER, true, true, ImcMsgManager.getManager());
        final SystemConfigurationEditorPanel sc2 = new SystemConfigurationEditorPanel(vehicle, Scope.MANEUVER,
                Visibility.USER, true, true, ImcMsgManager.getManager());
        
//        ImcMsgManager.getManager().addListener(new MessageListener<MessageInfo, IMCMessage>() {
//            @Override
//            public void onMessage(MessageInfo info, IMCMessage msg) {
//                System.out.println("---------");
//                SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(sc1, msg);
//                SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(sc2, msg);
//            }
//        }, vehicle, new MessageFilter<MessageInfo, IMCMessage>() {
//            @Override
//            public boolean isMessageToListen(MessageInfo info, IMCMessage msg) {
//                boolean ret = EntityParameter.ID_STATIC == msg.getMgid();
//                return ret;
//            }
//        });
        
        GuiUtils.testFrame(sc1);
        GuiUtils.testFrame(sc2);
    }
}
