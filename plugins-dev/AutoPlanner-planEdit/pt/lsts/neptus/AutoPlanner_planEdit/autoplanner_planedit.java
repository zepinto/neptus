package pt.lsts.neptus.AutoPlanner_planEdit;


import java.awt.Component;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
/**/
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;

import com.google.common.eventbus.Subscribe;
import com.rabbitmq.client.Method;

import net.miginfocom.swing.MigLayout;

import pt.lsts.neptus.i18n.I18n;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.containers.LayoutProfileProvider;
import pt.lsts.neptus.plugins.PluginDescription;


/**
 * @author Equipa C - SEAI 2016
 *
 */

@PluginDescription(name = "Auto Planner - Plan edition")
//@Popup(accelerator='Y',pos = POSITION.RIGHT, height=200, width = 200)
@SuppressWarnings("serial")

public class autoplanner_planedit extends ConsolePanel {

    /**
     * @param console
     */
 

    private JButton EditModeB, PausePlan, ResumePlan;

    
    public autoplanner_planedit(ConsoleLayout console) {
        super(console);
    }
    
    @Override
    public void initSubPanel() {
        setSize(300, 300);
        this.setLayout(new MigLayout("ins 0"));
        
        
        Action EditPlanAction = new AbstractAction(I18n.text("Edit Plan")) {

            @Override
            public void actionPerformed(ActionEvent e) {

                
                
               //inserir codigo aqui
                
            }
        };
        
        JButton EditPlan = new JButton(EditPlanAction);
        add(EditPlan);
        
        Action DelPlanAction = new AbstractAction(I18n.text("Delete Plan")) {

            @Override
            public void actionPerformed(ActionEvent e) {
               
               
                
                
                //inserir codigo aqui
               
            }
        };
        
        JButton DelPlan = new JButton(DelPlanAction);
        add(DelPlan);
       
      
        
   
        
    }
    
    
    @Override
    public void cleanSubPanel() {

    }
}