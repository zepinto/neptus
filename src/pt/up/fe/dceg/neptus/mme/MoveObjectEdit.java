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
 * $Id:: MoveObjectEdit.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.mme;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;

public class MoveObjectEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    private AbstractElement element = null;
	private LocationType oldLocation = new LocationType();
	private LocationType newLocation = new LocationType();
	
	public MoveObjectEdit(AbstractElement element, LocationType oldLoc, LocationType newLoc) {			
		this.element = element;
		this.oldLocation.setLocation(oldLoc);
		this.newLocation.setLocation(newLoc);
	}
	
	@Override
	public void undo() throws CannotUndoException {
		element.setCenterLocation(oldLocation);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
		mce.setSourceMap(element.getParentMap());
		mce.setChangedObject(element);
		mce.setChangeType(MapChangeEvent.OBJECT_MOVED);
		mce.setMapGroup(element.getMapGroup());
		element.getParentMap().warnChangeListeners(mce);
	}
	
	@Override
	public void redo() throws CannotRedoException {
		element.setCenterLocation(newLocation);
		MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
		mce.setSourceMap(element.getParentMap());
		mce.setChangedObject(element);
		mce.setMapGroup(element.getMapGroup());
		element.getParentMap().warnChangeListeners(mce);
	}
	
	@Override
	public boolean canRedo() {
		return true;
	}
	
	@Override
	public boolean canUndo() {
		return true;
	}
	
	@Override
	public String getPresentationName() {
		return "Move the "+element.getType()+" '"+element.getId()+"'";
	}
}
