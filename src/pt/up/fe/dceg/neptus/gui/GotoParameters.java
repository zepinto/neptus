/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zecarlos
 * Mar 24, 2005
 * $Id:: GotoParameters.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
/**
 * @author zecarlos
 *
 */
@SuppressWarnings("serial")
public class GotoParameters extends ParametersPanel implements ActionListener {

	private JPanel jPanel = null;
	private JPanel jPanel1 = null;
	private JLabel jLabel = null;
	private JButton jButton = null;
	private JLabel jLabel1 = null;
	private JFormattedTextField velocity = null;
	private JFormattedTextField radiusTolerance = null;
	private JLabel jLabel2 = null;
	private JFormattedTextField velocityTolerance = null;
	private JComboBox<?> unitsCombo = null;
	private JLabel jLabel5 = null;
	private JLabel jLabel3 = null;
	private static NumberFormat nf = NumberFormat.getNumberInstance();
	private LocationType destination;
	/**
	 * This method initializes 
	 * 
	 */
	public GotoParameters() {
		super();
		nf.setGroupingUsed(false);
		initialize();
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setSize(398, 152);
        this.add(getJPanel(), null);
        this.add(getJPanel1(), null);
			
	}
    public String getErrors() {
        return null;
    }

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel3 = new JLabel();
			jLabel = new JLabel();
			jPanel = new JPanel();
			jLabel.setText("Destination:");
			jLabel3.setText(" Tolerance:");
			jPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Destination", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			jPanel.add(jLabel, null);
			jPanel.add(getJButton(), null);
			jPanel.add(jLabel3, null);
			jPanel.add(getRadiusTolerance(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jLabel5 = new JLabel();
			jLabel2 = new JLabel();
			jLabel1 = new JLabel();
			jPanel1 = new JPanel();
			jLabel1.setText("Velocity:");
			jLabel2.setText("  Tolerance:");
			jLabel5.setText("  Units:");
			jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Velocity", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			jPanel1.add(jLabel1, null);
			jPanel1.add(getVelocity(), null);
			jPanel1.add(jLabel2, null);
			jPanel1.add(getVelocityTolerance(), null);
			jPanel1.add(jLabel5, null);
			jPanel1.add(getUnitsCombo(), null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText("Change...");
			jButton.setPreferredSize(new java.awt.Dimension(90,20));
			jButton.addActionListener(this);
		}
		return jButton;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getVelocity() {
		if (velocity == null) {
			velocity = new JFormattedTextField(nf);
			velocity.setText("0.0");
			velocity.setColumns(4);
			velocity.setToolTipText("The desired velocity over the trajectory");
			velocity.addFocusListener(new SelectAllFocusListener());
		}
		return velocity;
	}
	/**
	 * This method initializes jTextField1	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getRadiusTolerance() {
		if (radiusTolerance == null) {
			radiusTolerance = new JFormattedTextField(nf);
			radiusTolerance.setText("0.0");
			radiusTolerance.setColumns(4);
			radiusTolerance.setToolTipText("The radius tolerance over the trajectory (in meters)");
			radiusTolerance.addFocusListener(new SelectAllFocusListener());
		}
		return radiusTolerance;
	}
	/**
	 * This method initializes jTextField2	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getVelocityTolerance() {
		if (velocityTolerance == null) {
			velocityTolerance = new JFormattedTextField(nf);
			velocityTolerance.setColumns(4);
			velocityTolerance.setText("0.0");
			velocityTolerance.setToolTipText("The maximum allowed drift in the velocity");
			velocityTolerance.addFocusListener(new SelectAllFocusListener());
		}
		return velocityTolerance;
	}
	
	public void actionPerformed(ActionEvent e) {
	    LocationType loc = LocationPanel.showLocationDialog("Goto destination", getDestination(), getMissionType());
	    if (loc != null)
	        setDestination(loc);
	}
	
	
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getUnitsCombo() {
		if (unitsCombo == null) {
		    String[] units = new String[] {"RPM", "m/s"};
			unitsCombo = new JComboBox<Object>(units);
			unitsCombo.setPreferredSize(new java.awt.Dimension(70,20));
		}
		return unitsCombo;
	}
	
	public String getUnits() {
	    return (String)getUnitsCombo().getSelectedItem();
	}
	
	public double getVelocityValue() {
	    return Double.parseDouble(getVelocity().getText());
	}
	
	public double getVelocityToleranceValue() {
	    return Double.parseDouble(getVelocityTolerance().getText());
	}
	
	public double getRadiusToleranceValue() {
	    return Double.parseDouble(getRadiusTolerance().getText());
	}
	
	public void setVelocityValue(double value) {
	    getVelocity().setText(String.valueOf(value));
	}
	
	public void setVelocityToleranceValue(double value) {
	    getVelocityTolerance().setText(String.valueOf(value));
	}
	
	public void setRadiusToleranceValue(double value) {
	    getRadiusTolerance().setText(String.valueOf(value));
	}
	
	public void setUnits(String units) {
	    getUnitsCombo().setSelectedItem(units);
	}
                 
	public static void main(String[] args) {
    
	    GuiUtils.testFrame(new GotoParameters(), "Teste Unitário");
	}
    public LocationType getDestination() {
        return destination;
    }
    public void setDestination(LocationType destination) {
        this.destination = destination;
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
