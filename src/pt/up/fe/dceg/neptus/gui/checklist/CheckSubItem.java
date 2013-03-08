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
 * $Id:: CheckSubItem.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.gui.checklist;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.types.checklist.CheckAutoSubItem;
import pt.up.fe.dceg.neptus.util.ImageUtils;

public interface CheckSubItem {
	public static final ImageIcon ICON_CLOSE = new ImageIcon(ImageUtils.getScaledImage(
			"images/checklists/cancel.png", 16, 16));

	public CheckAutoSubItem getCheckAutoSubItem();
}
