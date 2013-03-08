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
 * $Id:: RMFEditor.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.fileeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.loader.FileHandler;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

public class RMFEditor extends JPanel implements FileHandler {

    private static final long serialVersionUID = -1121728041793256058L;

    private JPanel southPanel = null;
    private JButton saveButton = null;
    private JButton closeButton = null;
    private JLabel statusLabel = null;
    private JEditorPane rmfEditor = null;
    private String filename = null;

    /**
     * This method initializes
     * 
     */
    public RMFEditor() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(new java.awt.Dimension(539, 306));
        this.add(getSouthPanel(), java.awt.BorderLayout.SOUTH);
        this.add(new JScrollPane(getRMFEditorPane()), java.awt.BorderLayout.CENTER);
    }

    /**
     * This method initializes southPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getSouthPanel() {
        if (southPanel == null) {
            statusLabel = new JLabel();
            statusLabel.setText("");
            statusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
            statusLabel.setPreferredSize(new java.awt.Dimension(400, 14));
            statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
            southPanel = new JPanel();
            southPanel.setLayout(flowLayout);
            southPanel.add(statusLabel, null);
            southPanel.add(getCloseButton(), null);
            southPanel.add(getSaveButton(), null);
        }
        return southPanel;
    }

    /**
     * This method initializes saveButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton();
            saveButton.setText("Save");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (filename != null) {
                        FileUtil.saveToFile(filename, getRMFEditorPane().getText());
                    }
                };
            });
        }
        return saveButton;
    }

    /**
     * This method initializes closeButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getCloseButton() {
        if (closeButton == null) {
            closeButton = new JButton();
            closeButton.setText("Close");
        }
        return closeButton;
    }

    private JEditorPane getRMFEditorPane() {
        if (rmfEditor == null) {
            String[] keywords1 = new String[] { "Location", "Task", "Config", "Reaction" };

            String[] keywords2 = new String[] { "Surface", "Mark", "Transponder", "cal_depth", "set", "position",
                    "Goto", "hold", "dive", "Surface" };

            rmfEditor = SyntaxDocument.getCustomEditor(keywords1, keywords2, "#", "\"'");

            /*
             * rmfEditor.addKeyListener(new KeyAdapter() {
             * 
             * public void keyTyped(java.awt.event.KeyEvent e) {
             * statusLabel.setText("pos("+rmfEditor.getCaretPosition()+")"); }; });
             */
        }
        return rmfEditor;
    }

    public void setText(String text) {
        getRMFEditorPane().setText(text);
    }

    public void editFile(String filename) {
        this.filename = filename;
        String txt = FileUtil.getFileAsString(filename);
        setText(txt);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.loader.FileHandler#handleFile(java.io.File)
     */
    public void handleFile(File f) {
        final JFrame frame = new JFrame("RMF Editor - " + f.getName());
        final String filename = f.getAbsolutePath();
        statusLabel.setText(filename);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                frame.dispose();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = getRMFEditorPane().getText();
                if (FileUtil.saveToFile(filename, text))
                    GuiUtils.infoMessage(frame, "Success", "Contents have been saved to " + filename);
                else
                    GuiUtils.infoMessage(frame, "Error", "Unable to save contents to " + filename);
            }
        });

        editFile(f.getAbsolutePath());
        frame.setContentPane(this);
        frame.setSize(600, 400);
        frame.setIconImage(ImageUtils.getImage("images/neptus-icon.png"));
        GuiUtils.centerOnScreen(frame);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        RMFEditor ed = new RMFEditor();

        ed.handleFile(new File("c:/mission.rmf"));
    }

} // @jve:decl-index=0:visual-constraint="10,10"
