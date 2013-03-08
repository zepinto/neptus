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
 * 14/Mar/2005
 * $Id:: LocationTypeTest.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.junit.types.coord;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

public class LocationTypeTest extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(LocationTypeTest.class);
	}

	public void testAbsoluteLatLonDepth() {
		LocationType myLocation = new LocationType();
    	myLocation.setLatitude(41.4234);
    	myLocation.setLongitude(-8.812);
    	
    	myLocation.translatePosition(200, 200, 5);
    	
    	LocationType myLocation2 = new LocationType();
    	myLocation2.setLatitude(41.4234);
    	myLocation2.setLongitude(-8.812);
    	
    	myLocation2.translatePosition(200, 200, 5);
    	
    	myLocation2.convertToAbsoluteLatLonDepth();
    	
    	double diff[] = myLocation2.getOffsetFrom(myLocation);
    	double error = 0.01;
    	//assertEquals(0, diff[0],error);
	    assertEquals(0, diff[1],error);
	    //assertEquals(0, diff[2],error);
	}

	
	public void testGetOffsetFrom()
    {
    	LocationType myLocation = new LocationType();
    	myLocation.setLatitude(45);
    	myLocation.setLongitude(45);
    	
    	myLocation.setOffsetDistance(10);
    	myLocation.setAzimuth(90);
        	
    	myLocation.setDepth(10);
    	myLocation.setOffsetNorth(20);
    	myLocation.setOffsetEast(10);
    	
    	LocationType otherLocation = new LocationType();
    	otherLocation.setLatitude(45);
    	otherLocation.setLongitude(45);
    	
    	otherLocation.setOffsetDistance(10);
    	otherLocation.setAzimuth(0);
    	
    	otherLocation.setDepth(10);
    	otherLocation.setOffsetNorth(10);
    	otherLocation.setOffsetEast(20);
    	
    	
    	double[] diff = myLocation.getOffsetFrom(otherLocation);
    	double error = 0.01;
    	assertEquals(0, diff[0],error);
	    assertEquals(0, diff[1],error);
	    assertEquals(0, diff[2],error);
    }
	
	public void testOffsets() {
		
		LocationType first = new LocationType();
		first.setLatitude("41N3.6117");
		first.setLongitude("8W27.4009");
		LocationType second = new LocationType();
		second.setLatitude("41N3.6117");
		second.setLongitude("-8E27.4009");
		double[] offsets2 = second.getOffsetFrom(new LocationType());
		double[] offsets1 = first.getOffsetFrom(new LocationType());
		
		assertEquals(offsets1[0], offsets2[0], 0.001);
		
	}
	
	public void testAbsoluteValues() {
		LocationType first = new LocationType();
		LocationType second = new LocationType();
		
		first.setLatitude("41N3.6117");
		first.setLongitude("8W27.4009");
		//first.setOffsetEast(100);
		//first.setOffsetSouth(100);
		
		//double offsets1[] = first.getOffsetFrom(new LocationType());
		//System.out.println("Offsets[0]="+offsets1[0]+", Offsets[1]="+offsets1[1]);
		
		second = new LocationType();
		
		//second.setOffsetNorth(offsets1[0]);
		//second.setOffsetEast(offsets1[1]);
		
		System.out.println("FIRST:\n"+first.getDebugString());
		System.out.println("SECOND:\n"+second.getDebugString());
		
		//second.setLocation(first);
		//second.setZenith(0.00);
		//second.setOffsetDistance(-10);
		//second.setOffsetDown(10);
		
		double offsets1[] = second.getOffsetFrom(first);
		double offsets2[] = first.getOffsetFrom(second);
		System.out.println("Offsets[0]="+offsets1[0]+", Offsets[1]="+offsets1[1]);
		System.out.println("Offsets[0]="+offsets2[0]+", Offsets[1]="+offsets2[1]);
		assertEquals(offsets1[0], 0, 0.0001);
		assertEquals(offsets1[1], 0, 0.0001);
		assertEquals(offsets1[2], 0, 0.0001);
		
		//LocationType third = new LocationType();
		
		//third.setOffsetNorth(552871.375);
		//System.out.println(third.getOffsetFrom(new LocationType())[0]);
	}
}
