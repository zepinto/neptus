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
 * 21/02/2005
 * $Id:: LocationPanel.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
/**
 * @author Ze Carlos
 * @author Paulo Dias
 */
public class LocationPanel extends ParametersPanel implements ActionListener {

    private static final long serialVersionUID = 8283415165348726797L;
    public static LocationType clipboard = null;
    private JDialog dialog = new JDialog();
	private LocationType location = new LocationType();
	private JPanel jPanel = null;
	private JTabbedPane refPointTabs = null;
	private JPanel jPanel1 = null;
	private JButton cancelBtn = null;
	private JTabbedPane OffsetTabs = null;
	private JPanel nedOffsetPanel = null;
	private JPanel panel1 = null;
	private JPanel existingLocationPanel = null;
	private JButton okBtn = null;
	private MissionType missionType = null;
	private PointSelector pointSelector = null;
	private MarkSelector markSelector = null;
	private RegularOffset regularOffset = null;
	private PolarOffsetPanel polarOffsetPanel = null;
    private boolean editable = true;
    private boolean userCancel = false;
	private JButton btnCopy = null;
	private JButton btnPaste = null;
	private JButton findOnMap = null;
    private JButton makeLocAbsolute = null;
	private Component hg1 = Box.createHorizontalGlue();
	private Component hg2 = Box.createHorizontalGlue();
	private boolean hideOkCancelButtons = false;

	public LocationPanel(LocationType location, MissionType missionType) {
		super();
		this.missionType = missionType;
		setLocationType(location);
		initialize();
	}


	public LocationPanel(MissionType missionType) {
	    this.missionType = missionType;
		initialize();
	}
	
	/**
	 * This method initializes jPanel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(new BorderLayout());
			jPanel.setBounds(15, 15, 413, 225);
            jPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Reference Point"),
                    TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
			jPanel.add(getRefPointTabs(), BorderLayout.CENTER);
		}
		return jPanel;
	}
	/**
	 * This method initializes jTabbedPane
	 *
	 * @return javax.swing.JTabbedPane
	 */
	private JTabbedPane getRefPointTabs() {
		if (refPointTabs == null) {
			refPointTabs = new JTabbedPane();
			//RefPointTabs.setT
			refPointTabs.addTab(I18n.text("New Location"), null, getPointSelector(), null);
			if (getMissionType() != null)
				refPointTabs.addTab(I18n.text("Existing location"), null, getExistingLocationPanel(), null);
		}
		return refPointTabs;
	}
	/**
	 * This method initializes jPanel1
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BorderLayout());
			jPanel1.setBounds(16, 244, 412, 109);
            jPanel1.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Offset"),
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION, null, null));
			jPanel1.add(getOffsetTabs(), BorderLayout.CENTER);
		}
		return jPanel1;
	}
	/**
	 * This method initializes cancelBtn1
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setBounds(337, 375, 90, 30);
			cancelBtn.setText(I18n.text("Cancel"));
			cancelBtn.addActionListener(this);
			cancelBtn.setActionCommand("cancel");
		}
		return cancelBtn;
	}
	/**
	 * This method initializes jTabbedPane1
	 *
	 * @return javax.swing.JTabbedPane
	 */
	public JTabbedPane getOffsetTabs() {
		if (OffsetTabs == null) {
			OffsetTabs = new JTabbedPane();
			OffsetTabs.addTab(I18n.text("Orthogonal Offsets"), null, getNedOffsetPanel(), null);
			OffsetTabs.addTab(I18n.text("Spherical Offsets"), null, getJPanel2(), null);
		}
		return OffsetTabs;
	}



	/**
	 * This method initializes nedOffsetPanel
	 *
	 * @return javax.swing.JPanel
	 */
	public JPanel getNedOffsetPanel() {
		if (nedOffsetPanel == null) {
			nedOffsetPanel = new JPanel();
			nedOffsetPanel.add(getRegularOffset(), null);
		}
		return nedOffsetPanel;
	}
	/**
	 * This method initializes polarOffsetPanel
	 *
	 * @return javax.swing.JPanel
	 */
	public JPanel getJPanel2() {
		if (panel1 == null) {
			panel1 = new JPanel();
			panel1.add(getPolarOffsetPanel(), null);
		}
		return panel1;
	}
	/**
	 * This method initializes existingLocationPanel
	 *
	 * @return javax.swing.JPanel
	 */
	public JPanel getExistingLocationPanel() {
		if (existingLocationPanel == null) {
			existingLocationPanel = new JPanel();
			existingLocationPanel.add(getMarkSelector(), null);
		}
		return existingLocationPanel;
	}
	/**
	 * This method initializes okBtn1
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getOkBtn() {
		if (okBtn == null) {
			okBtn = new JButton();
			okBtn.setBounds(227, 375, 90, 30);
			okBtn.setText(I18n.text("OK"));
			okBtn.addActionListener(this);
			okBtn.setActionCommand("ok");
		}
		return okBtn;
	}
	/**
	 * This method initializes pointSelector
	 *
	 * @return pt.up.fe.dceg.neptus.gui.PointSelector
	 */
	public PointSelector getPointSelector() {
		if (pointSelector == null) {
			pointSelector = new PointSelector();
			pointSelector.setMissionType(getMissionType());
		}
		return pointSelector;
	}

	/**
	 * This method initializes markSelector
	 *
	 * @return pt.up.fe.dceg.neptus.gui.MarkSelector
	 */
	private MarkSelector getMarkSelector() {
		if (markSelector == null) {
			markSelector = new MarkSelector(missionType);
			markSelector.addMarksComboListener(new MarksComboListener() {
				public void MarkComboChanged(MarkElement selectedMarkObject) {
					if (selectedMarkObject != null) {
						setLocationType(new LocationType(selectedMarkObject.getCenterLocation()));
					}
				};
			});			
		}
		return markSelector;
	}

	public void hideButtons() {
	    hideOkCancelButtons = true;
	    getOkBtn().setVisible(false);
        getCancelBtn().setVisible(false);
	}

	/**
	 * This method initializes regularOffset
	 *
	 * @return pt.up.fe.dceg.neptus.gui.RegularOffset
	 */
	private RegularOffset getRegularOffset() {
		if (regularOffset == null) {
			regularOffset = new RegularOffset();
		}
		return regularOffset;
	}

	/**
	 * This method initializes polarOffsetPanel1
	 *
	 * @return pt.up.fe.dceg.neptus.gui.PolarOffsetPanel
	 */
	private PolarOffsetPanel getPolarOffsetPanel() {
		if (polarOffsetPanel == null) {
			polarOffsetPanel = new PolarOffsetPanel();
		}
		return polarOffsetPanel;
	}
   	/**
	 * This method initializes copyBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnCopy() {
		if (btnCopy == null) {
			btnCopy = new JButton();
			btnCopy.setMargin(new Insets(0,0,0,0));
			btnCopy.setSize(new Dimension(20,20));
			btnCopy.setLocation(new Point(16,360));
			btnCopy.setToolTipText(I18n.text("Copy this location to the clipboard"));
			btnCopy.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png")));
			btnCopy.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					btnPaste.setEnabled(isEditable());
					ClipboardOwner owner = new ClipboardOwner() {
						public void lostOwnership(Clipboard clipboard, Transferable contents) {};						
					};
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(getLocationType().getClipboardText()), owner);
				};
			});
		}
		return btnCopy;
	}


	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnPaste() {
		if (btnPaste == null) {
			btnPaste = new JButton();
			btnPaste.setPreferredSize(new Dimension(20,20));
			btnPaste.setEnabled(isEditable());// && LocationPanel.clipboard != null);	
			btnPaste.setMargin(new Insets(0,0,0,0));
			btnPaste.setBounds(new java.awt.Rectangle(42,360,20,20));
			btnPaste.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/editpaste.png")));
			btnPaste.setToolTipText(I18n.text("Paste from clipboard"));
			btnPaste.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					btnPaste.setEnabled(isEditable());

					@SuppressWarnings({ "unused" })
                    ClipboardOwner owner = new ClipboardOwner() {
						public void lostOwnership(Clipboard clipboard, Transferable contents) {};						
					};
					
					Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
					
                    boolean hasTransferableText = (contents != null)
                            && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
					
					if ( hasTransferableText ) {
						try {
							String text = (String)contents.getTransferData(DataFlavor.stringFlavor);
							LocationType lt = new LocationType();
							lt.fromClipboardText(text);
							setLocationType(lt);
						}
						catch (Exception e) {
							NeptusLog.pub().error(e);
						}
					}				
				}
			});
		}
		return btnPaste;
	}


	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getFindOnMap() {
		if (findOnMap == null) {
			findOnMap = new JButton();
			findOnMap.setBounds(new Rectangle(68,360,20,20));
			findOnMap.setIcon(new ImageIcon(ImageUtils.getImage("images/buttons/findOnMap.png")));
			findOnMap.setMargin(new Insets(0, 0, 0, 0));
			findOnMap.setEnabled(false);
			findOnMap.setToolTipText(I18n.text("Select a location on map..."));
			findOnMap.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					Window w = SwingUtilities.getWindowAncestor(LocationPanel.this);				
					@SuppressWarnings("unused")
                    LocationType lt = MapLocationSelector.showDialog((w instanceof Frame) ? (Frame) w : null,
                            MapGroup.getMapGroupInstance(missionType));
				}
			});
		}
		return findOnMap;
	}

	/**
	 * @return
	 */
	private JButton getMakeLocAbsolute() {
        if (makeLocAbsolute == null) {
            makeLocAbsolute = new JButton();
            makeLocAbsolute.setBounds(new Rectangle(68,360,80,20));
            //makeLocAbsolute.setIcon(new ImageIcon(GuiUtils.getImage("images/buttons/findOnMap.png")));
            
            /// Abs is the absolute value, don't use more than 3 letters
            makeLocAbsolute.setText(I18n.text("Abs"));
            makeLocAbsolute.setFont(makeLocAbsolute.getFont().deriveFont(10));
            makeLocAbsolute.setMargin(new Insets(0, 0, 0, 0));
            makeLocAbsolute.setEnabled(true);
            makeLocAbsolute.setToolTipText(I18n.text("Make the location absolute"));
            makeLocAbsolute.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    setLocationType((LocationType) getLocationType().getNewAbsoluteLatLonDepth());                    
                }
            });
        }
        return makeLocAbsolute;
    }

	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private  void initialize() {
		this.setBounds(0, 0, 467, 428);
		
		GuiUtils.reactEnterKeyPress(getOkBtn());
		GuiUtils.reactEscapeKeyPress(getCancelBtn());
		
		GroupLayout gl = new GroupLayout(this);
		gl.setAutoCreateGaps(true);
		gl.setAutoCreateContainerGaps(true);
		this.setLayout(gl);
		gl.setHorizontalGroup(gl.createParallelGroup(Alignment.CENTER)
		        .addComponent(getJPanel())
		        .addComponent(getJPanel1())
		        .addGroup(gl.createSequentialGroup()
		                .addComponent(getBtnCopy())
		                .addComponent(getBtnPaste())
		                .addComponent(getFindOnMap())
		                .addGap(20)
		                .addComponent(getMakeLocAbsolute())
		                .addComponent(hg1))
		        .addGroup(gl.createSequentialGroup()
                        .addComponent(hg2)
		                .addComponent(getOkBtn())
		                .addComponent(getCancelBtn())
		                ));

		gl.setVerticalGroup(gl.createSequentialGroup()
		        .addComponent(getJPanel())
		        .addComponent(getJPanel1(), 100, 100, 100)
		        .addGroup(gl.createParallelGroup(Alignment.LEADING)
		                .addComponent(getBtnCopy())
		                .addComponent(getBtnPaste())
		                .addComponent(getFindOnMap())
                        .addComponent(getMakeLocAbsolute())
                        .addComponent(hg1))
                .addGroup(gl.createParallelGroup(Alignment.TRAILING)
                        .addComponent(hg2)
                        .addComponent(getOkBtn())
                        .addComponent(getCancelBtn())
                        ));
		
		gl.linkSize(SwingConstants.HORIZONTAL, getOkBtn(), getCancelBtn());
		gl.linkSize(SwingConstants.HORIZONTAL, getBtnCopy(), getBtnPaste(), getFindOnMap());
        gl.linkSize(SwingConstants.VERTICAL, getBtnCopy(), getBtnPaste(), getFindOnMap(),
                getMakeLocAbsolute());
	}

	public void actionPerformed(ActionEvent ae) {
		
		if ("ok".equals(ae.getActionCommand())) {

			if (getErrors() != null) {
				JOptionPane.showMessageDialog(this, getErrors(), I18n.text("Errors in the parameters"), JOptionPane.ERROR_MESSAGE);
				return;
			}

			LocationType loc2 = getPointSelector().getLocationType();
			location.setLocation(loc2);
			char[] selectedOffsets = regularOffset.getSelectedOrientations();

			location.setOffsetNorth(regularOffset.getNorthOffset(), selectedOffsets[0] == 'N');
			location.setOffsetEast(regularOffset.getEastOffset(), selectedOffsets[1] == 'E');
			location.setOffsetDown(regularOffset.getDownOffset(), selectedOffsets[2] == 'D');

			location.setAzimuth(getPolarOffsetPanel().getAzimuthOffset());
			location.setOffsetDistance(getPolarOffsetPanel().getDistanceOffset());
			location.setZenith(getPolarOffsetPanel().getZenithOffset());

			dialog.setVisible(false);
			dialog.dispose();
		}

		if ("cancel".equals(ae.getActionCommand())) {
			location = null;
			setUserCancel(true);
			dialog.setVisible(false);
			dialog.dispose();
		}
	}

	public LocationType getLocationType() {
	    LocationType loc2 = getPointSelector().getLocationType();
		location.setLocation(loc2);
		char[] selectedOffsets = regularOffset.getSelectedOrientations();

		location.setOffsetNorth(regularOffset.getNorthOffset(), selectedOffsets[0] == 'N');
		location.setOffsetEast(regularOffset.getEastOffset(), selectedOffsets[1] == 'E');
		location.setOffsetDown(regularOffset.getDownOffset(), selectedOffsets[2] == 'D');

		location.setAzimuth(getPolarOffsetPanel().getAzimuthOffset());
		location.setOffsetDistance(getPolarOffsetPanel().getDistanceOffset());
		location.setZenith(getPolarOffsetPanel().getZenithOffset());

		return location;
	}

	public void setLocationType(LocationType location) {
		this.location = location;
		getPointSelector().setLocationType(location);
		getPolarOffsetPanel().setLocationType(location);
		getRegularOffset().setLocationType(location);
	}


    public void setEditable(boolean value) {
        this.editable = value;
        getMarkSelector().setEnabled(editable);
        getPointSelector().setEditable(editable);
        getRegularOffset().setEditable(editable);
        getPolarOffsetPanel().setEditable(editable);
        getCancelBtn().setVisible(!hideOkCancelButtons && editable);
        getBtnPaste().setEnabled(editable);
        //getFindOnMap().setEnabled(editable);
        getMakeLocAbsolute().setEnabled(editable);
    }

	private JDialog getLocationDialog(Component parent, String title) {
		
		if (parent instanceof Window)
			dialog = new JDialog((Window)parent);
		else
			dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
		
		dialog.setTitle(title);
		dialog.setSize(this.getWidth() + 5, this.getHeight() + 35);
		dialog.setLayout(new BorderLayout());
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		//dialog.setModal(true);
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
		dialog.setAlwaysOnTop(false);
		GuiUtils.centerOnScreen(dialog);
		dialog.setVisible(true);
		dialog.setResizable(false);
		//dialog.setAlwaysOnTop(true);
		
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				location = null;
				setUserCancel(true);
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		return dialog;
	}

	public String getErrors() {
	    if (regularOffset.getErrors() != null)
	        return I18n.textf("<html>Error in the <b>offset</b> parameters:<br>%errors</html>", regularOffset.getErrors());

	    if (polarOffsetPanel.getErrors() != null)
	        return I18n.textf("<html>Error in the <b>polar offset</b> parameters:<br>%errors</html>", polarOffsetPanel.getErrors());

	    if (pointSelector.getErrors() != null)
	        return I18n.textf("<html>The entered <b>location</b> is invallid:<br>%errors</html>", pointSelector.getErrors());

	    return null;
	}

	public static LocationType showLocationDialog(String title, LocationType previousLocation, MissionType mt) {
	    return showLocationDialog(title, previousLocation, mt, true);
	}
	
	public static LocationType showLocationDialog(Component parent, String title, LocationType previousLocation, MissionType mt, boolean editable) {
		LocationType location = new LocationType();

		location.setLocation(previousLocation);

		location.setOffsetDown(previousLocation.getOffsetDown(), !previousLocation.isOffsetUpUsed());
		location.setOffsetNorth(previousLocation.getOffsetNorth(), previousLocation.isOffsetNorthUsed());
		location.setOffsetEast(previousLocation.getOffsetEast(), previousLocation.isOffsetEastUsed());

		LocationPanel locPanel = new LocationPanel(location, mt);
		locPanel.setEditable(editable);
		if (parent == null)
			locPanel.getLocationDialog(ConfigFetch.getSuperParentAsFrame(), title);
		else
			locPanel.getLocationDialog(parent, title);
		
		if (locPanel.isUserCancel())
			return null;
		return locPanel.getLocationType();
	}

	public static LocationType showLocationDialog(String title, LocationType previousLocation, MissionType mt, boolean editable) {
		return showLocationDialog(null, title, previousLocation, mt, editable);
	}

    public MissionType getMissionType() {
        return missionType;
    }

    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
        getMarkSelector().setMissionType(this.missionType);
    }

    public boolean isUserCancel() {
        return userCancel;
    }

    public void setUserCancel(boolean userCancel) {
        this.userCancel = userCancel;
    }

    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        LocationType loc = new LocationType(41, -8);
        loc.setDepth(-45);
        
        System.out.println(loc.asXML());
        
        LocationType other = new LocationType();
        other.setLocation(loc);
        System.out.println(other.asXML());

        loc = LocationPanel.showLocationDialog("Testing..", loc, null);
        System.out.println("AFTER:");
        System.out.println(loc.asXML());
    }
}
