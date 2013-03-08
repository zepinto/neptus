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
 * $Id:: VerifyingProperty.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.gui.properties;

import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;

public abstract class VerifyingProperty extends DefaultProperty {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    public abstract Vector<String> verifyErrors(Object value);
}
