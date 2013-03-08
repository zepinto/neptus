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
 * Mar 15, 2005
 * $Id:: TransponderParameters.java 9616 2012-12-30 23:23:22Z pdias       $:
 */
package pt.up.fe.dceg.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.editors.EditorLauncher;
/**
 * @author zecarlos
 *
 */
public class TransponderParameters extends ParametersPanel {

	private static final long serialVersionUID = 7696810945439062905L;

	private LocationPanel locationPanel = null;
	private JPanel jPanel = null;
	private JLabel jLabel = null;
	private JComboBox<?> configurationFile = null;
	private JButton editBtn = null;
	private CoordinateSystem homeRef = null;
	private JButton jButton = null;
	private JLabel jLabel1 = null;
	
	/**
	 * This method initializes 
	 * 
	 */
	public TransponderParameters(CoordinateSystem homeRef) {
		super();
		this.homeRef = homeRef;
		initialize();
		setPreferredSize(new Dimension(470,530));
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new BorderLayout());
        //this.setBounds(0, 0, 441, 398);
        this.add(getLocationPanel(), java.awt.BorderLayout.CENTER);
        this.add(getJPanel(), java.awt.BorderLayout.SOUTH);
			
	}
    public String getErrors() {
        
    	if (getConfigurationFile().getSelectedItem() == null) {
    		return "A configuration file must be selected";
    	}
    	
        if (getLocationPanel().getErrors() != null)
            return getLocationPanel().getErrors();
        
        return null;
    }
    
    public void setLocation(LocationType location) {
    	getLocationPanel().setLocationType(location); 
    }

    public void setMap(MapType map) {
    	getLocationPanel().setMissionType(getMissionType());
    	if (map.getMission() != null) {
    	    getLocationPanel().setMissionType(map.getMission());
    	}
    	else {
    	    MissionType mt = new MissionType();
    	    MapMission mapm = new MapMission();
    	    mapm.setMap(map);
    	    mt.addMap(mapm);
    	    MapGroup.getMapGroupInstance(mt);
    	}
    }
    
    
	/**
	 * This method initializes locationPanel	
	 * 	
	 * @return pt.up.fe.dceg.neptus.gui.LocationPanel	
	 */    
	public LocationPanel getLocationPanel() {
		if (locationPanel == null) {
			locationPanel = new LocationPanel(getMissionType());
			locationPanel.hideButtons();
			locationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
			
		}
		return locationPanel;
	}
	
	
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel1 = new JLabel();
			FlowLayout flowLayout1 = new FlowLayout();
			jLabel = new JLabel();
			jPanel = new JPanel();
			jPanel.setLayout(flowLayout1);
			jLabel.setText("Configuration File: ");
			flowLayout1.setHgap(5);
			jLabel1.setText("           ");
			jPanel.add(getJButton(), null);
			jPanel.add(jLabel1, null);
			
			jPanel.add(jLabel, null);
			jPanel.add(getConfigurationFile(), null);
			jPanel.add(getEditBtn(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getConfigurationFile() {
		if (configurationFile == null) {
//		    String[] confs = new String[] {"lsts1.conf", "lsts2.conf", "lsts3.conf" /*, "lsts1m.conf", "lsts2m.conf", "lsts3m.conf"*/};
            String[] confs = TransponderElement.getTranspondersListArray();
			configurationFile = new JComboBox<Object>(confs);
			configurationFile.setPreferredSize(new java.awt.Dimension(90,20));
			configurationFile.setEditable(false);
			configurationFile.setEnabled(true);
			configurationFile.setSelectedIndex(0);
			
		}
		return configurationFile;
	}
	
	public String getConfiguration() {
	    return (String) getConfigurationFile().getSelectedItem();
	}
	
	public void setConfiguration(String configuration) {
		if (configuration == null)
			return;
		
		getConfigurationFile().setSelectedItem(configuration);
	}
	
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("Triangulation");
			jButton.setPreferredSize(new java.awt.Dimension(110,25));
			jButton.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
                    LocationType lt = TranspondersPositionHelper
                            .showTranspondersPositionHelperDialog(homeRef,
                                    TransponderParameters.this);
					if (lt != null)
						setLocation(lt);
				}
			});
		}
		return jButton;
	}
  	
  	public void setEditable(boolean value) {
		super.setEditable(value);
		locationPanel.setEditable(isEditable());
		if (!isEditable()) {
			getConfigurationFile().setEnabled(false);
			getJButton().setEnabled(false);
		}
		else {
			getConfigurationFile().setEnabled(true);
			getJButton().setEnabled(true);
		}
	}
	public JButton getEditBtn() {
		if (editBtn == null) {
			editBtn = new JButton("Edit file");
			editBtn.setPreferredSize(new java.awt.Dimension(110,25));
			editBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(new JFrame(), "<html><strong>Full attention</strong> when altering this file, <br>"+
							"The changes will aply to all existing missions!</html>", "Warning", JOptionPane.WARNING_MESSAGE);
						//FIXME : alterar o caminho para os ficheiros de configuração para o caminho correcto!
						(new EditorLauncher()).editFile(ConfigFetch.resolvePath("maps/"+getConfigurationFile().getSelectedItem()));					
				}
			});
		}
		return editBtn;
	}

	public CoordinateSystem getHomeRef() {
		return homeRef;
	}

	public void setHomeRef(CoordinateSystem homeRef) {
		this.homeRef = homeRef;
	}

    public static void main(String[] args) {
        
        JFrame testFrame = new JFrame("Teste Unitario");
        TransponderParameters nmp = new TransponderParameters(new CoordinateSystem());
        testFrame.add(nmp);
        testFrame.setSize(453, 450);
        testFrame.setVisible(true);

        final Vector<String> aTranspondersFiles = new Vector<String>();
        File dir = new File("maps/");
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                System.out.println(name + ": " + name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z]+[0-9]+\\.conf)$"));
                if (name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z]+[0-9]+\\.conf)$")) {
                    return true;
                }
                return false;
            }
        });
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.getName().startsWith("lsts") && !o2.getName().startsWith("lsts"))
                    return -1;
                else if (!o1.getName().startsWith("lsts") && o2.getName().startsWith("lsts"))
                    return 1;
                return o1.compareTo(o2);
            }
        });
        for (File file : files) {
            System.out.println(file.getName());
            aTranspondersFiles.add(file.getName());
        }
    }
}
