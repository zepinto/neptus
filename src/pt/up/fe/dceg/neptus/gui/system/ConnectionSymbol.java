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
 * 2010/05/28
 * $Id:: ConnectionSymbol.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class ConnectionSymbol extends SymbolLabel {

	/**
	 * Connection strength 
	 */
	public enum ConnectionStrengthEnum {LOW, MEDIAN, HIGH, FULL};
	
	private ConnectionStrengthEnum strength = ConnectionStrengthEnum.FULL;
	
	private boolean activeAnnounce = false;
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#initialize()
	 */
	@Override
	protected void initialize() {
		setSize(10, 10);
		setPreferredSize(new Dimension(10, 10));
		super.initialize();
	}
	
	/**
	 * @return the strength
	 */
	public ConnectionStrengthEnum getStrength() {
		return strength;
	}
	
	/**
	 * @param strength the strength to set
	 */
	public void setStrength(ConnectionStrengthEnum strength) {
		this.strength = strength;
		repaint();
	}
	
	/**
	 * 
	 */
	public void setFullStrength() {
		setStrength(ConnectionStrengthEnum.FULL);
	}

	/**
	 * @return
	 */
	public ConnectionStrengthEnum reduceStrength() {
		switch (strength) {
		case FULL:
			setStrength(ConnectionStrengthEnum.HIGH);
			break;
		case HIGH:
			setStrength(ConnectionStrengthEnum.MEDIAN);
			break;
		case MEDIAN:
			setStrength(ConnectionStrengthEnum.LOW);
			break;
		default:
			break;
		}
		return strength;
	}
	
	/**
     * @return the activeAnnounce
     */
    public boolean isActiveAnnounce() {
        return activeAnnounce;
    }
    
    /**
     * @param activeAnnounce the activeAnnounce to set
     */
    public void setActiveAnnounce(boolean activeAnnounce) {
        this.activeAnnounce = activeAnnounce;
    }
	
	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
	 */
	@Override
	public void paint(Graphics2D g, JXPanel c, int width, int height) {
		Graphics2D g2 = (Graphics2D)g.create();

		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
		g2.setColor(new Color(0,0,0,0));
		g2.fill(rect);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.scale(width/10.0, height/10.0);

		if (activeAnnounce) {
	        g2.setColor(getActiveColor());
	        GeneralPath sp = new GeneralPath();
            sp.moveTo(0, 8);
            sp.lineTo(0, 2);
            //sp.lineTo(5, 0);
            g2.draw(sp);
		}
		
		g2.translate(2, 5);
		
		Ellipse2D el1 = new Ellipse2D.Double(-1d, -1d, 2d, 2d);
		g2.setColor(getActiveColor());
		g2.fill(el1);
		
		if (isActive()) {
			Arc2D.Double arc1;
			if (strength.ordinal() >= ConnectionStrengthEnum.MEDIAN.ordinal()) {
				arc1 = new Arc2D.Double(0d, -3d/2d, 3d, 3d, -45d, 90, Arc2D.OPEN);
				g2.draw(arc1);
			}
			if (strength.ordinal() >= ConnectionStrengthEnum.HIGH.ordinal()) {
				arc1 = new Arc2D.Double(0d, -5d/2d, 5d, 5d, -45d, 90, Arc2D.OPEN);
				g2.draw(arc1);
			}
			if (strength.ordinal() >= ConnectionStrengthEnum.FULL.ordinal()) {
				arc1 = new Arc2D.Double(0d, -7d/2d, 7d, 7d, -45d, 90, Arc2D.OPEN);
				g2.draw(arc1);
			}
		}
		else {
			g2.translate(4, 0);
			GeneralPath sp = new GeneralPath();
			sp.moveTo(-3, -3);
			sp.lineTo(3, 3);
			sp.moveTo(3, -3);
			sp.lineTo(-3, 3);
			//g2.setColor(Color.WHITE);
			g2.draw(sp);
		}

		if (activeAnnounce) {
		    g2.setFont(new Font("Arial", Font.BOLD, 3));
            g2.drawString("A", -1, 4);
		}
	}
	
	public static void main(String[] args) {
		ConnectionSymbol symb1 = new ConnectionSymbol();
		symb1.setSize(50, 50);
		symb1.setActive(true);
		JXPanel panel = new JXPanel();
		panel.setBackground(Color.BLACK);
		panel.setLayout(new BorderLayout());
		panel.add(symb1, BorderLayout.CENTER);
		GuiUtils.testFrame(panel,"",400,400);
		
		try {
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.reduceStrength();
			Thread.sleep(2000);
			symb1.setFullStrength();
            Thread.sleep(2000);
            symb1.setActive(false);
            Thread.sleep(2000);
            symb1.setActiveAnnounce(true);
            Thread.sleep(2000);
            symb1.setActive(true);
            symb1.setFullStrength();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
