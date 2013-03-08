/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Rui Gonçalves
 * 2010/04/14
 * $Id:: VrpManager.java 9635 2013-01-02 17:52:23Z pdias                        $:
 */
package pt.up.fe.dceg.neptus.plugins.vrp.planning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import javax.vecmath.Point2d;

import drasys.or.graph.DuplicateVertexException;
import drasys.or.graph.EdgeI;
import drasys.or.graph.GraphI;
import drasys.or.graph.MatrixGraph;
import drasys.or.graph.PointGraph;
import drasys.or.graph.VertexNotFoundException;
import drasys.or.graph.vrp.BestOf;
import drasys.or.graph.vrp.ClarkeWright;
import drasys.or.graph.vrp.Composite;
import drasys.or.graph.vrp.ImproveI;
import drasys.or.graph.vrp.ImproveWithTSP;
import drasys.or.graph.vrp.SolutionNotFoundException;
import drasys.or.graph.vrp.VRPException;

/**
 * @author Rui Gonçalves
 * 
 */
public class VrpManager {

    Customer[] customers = null;

    public static double dist = 0;

    public static Vector<Vector<Point2d>> computePathsSingleDepot(Point2d depot, Vector<Point2d> pointList,
            int n_vehicles) {

        Vector<Vector<Point2d>> returnVector = new Vector<Vector<Point2d>>();

        int sizeVisitPoints = pointList.size();
        PointGraph pointGraph = new PointGraph();
        PointIdoubleI[] arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];

        Object key = "Depot";
        arrayVRP[0] = new PointIdoubleI(1, depot);
        try {
            pointGraph.addVertex(key, arrayVRP[0]);
        }
        catch (DuplicateVertexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            for (int i = 1; i < sizeVisitPoints + 1; i++) {

                pointGraph.addVertex(new Integer(i), new PointIdoubleI(1, pointList.get(i - 1)));
            }
        }
        catch (DuplicateVertexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        GraphI graph;
        graph = new MatrixGraph(pointGraph, null);
        graph.setSymmetric(false);

        Composite vrp;
        BestOf bestOf = new BestOf();
        int iterations = 10, strength = 4;
        drasys.or.graph.tsp.ImproveI subalgorithm = new drasys.or.graph.tsp.TwoOpt();
        try {
            bestOf.addConstruct(new ClarkeWright(iterations, strength, subalgorithm));
            // bestOf.addConstruct(new
            // GillettMiller(iterations,strength,subalgorithm));
        }
        catch (VRPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ImproveI improve = new ImproveWithTSP(new drasys.or.graph.tsp.ThreeOpt());
        ;
        vrp = new Composite(bestOf, improve);
        vrp.setCostConstraint(Double.MAX_VALUE/* rangeConstraint*1000 */);

        vrp.setCapacityConstraint(5000 /* capacityConstraint */);

        vrp.setGraph(graph);

        Vector<?>[] tours = null;
        try {
            System.out.println("chamada ao solver");
            vrp.constructClosedTours("Depot");
            System.out.println("passou");
            tours = vrp.getTours();
        }
        catch (SolutionNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println("solution not found");
            e.printStackTrace();
        }
        catch (VertexNotFoundException e) {
            System.out.println("Vertex not found");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        double meters = 0;
        if (n_vehicles == tours.length)
            System.out.println("OK - One path for each vehicle");

        for (int i = 0; i < tours.length; i++) {
            Vector<Point2d> path = new Vector<Point2d>();
            Enumeration<?> e = tours[i].elements();
            e.nextElement(); // Skip Vertex
            // PointIdoubleI customer_aux=
            // (PointIdoubleI)edge_aux.getToVertex().getValue();
            // customer_aux.getPoint2d()
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                PointIdoubleI customer1 = (PointIdoubleI) edge.getToVertex().getValue();

                PointIdoubleI customer2 = (PointIdoubleI) edge.getFromVertex().getValue();
                meters += customer1.distanceTo(customer2);

                path.add(customer1.getPoint2d());
                /*
                 * int x1 = (int)customer1.screenPoint.x(); int y1 = (int)customer1.screenPoint.y(); int x2 =
                 * (int)customer2.screenPoint.x(); int y2 = (int)customer2.screenPoint.y();
                 */
                // g.drawLine(x1, y1, x2, y2);
                e.nextElement(); // Skip Vertex
            }
            returnVector.add(path);
        }
        String msg = "Vehicles - " + tours.length + ", ";
        msg += "Distance(Km) - " + meters / 1000;
        System.out.println(msg);

        dist = meters;
        // ------------------------------------------------------------------
        double rangeConstraint = meters;
        double step = rangeConstraint / n_vehicles;

        rangeConstraint -= step;

        int last = -1;

        while (returnVector.size() != n_vehicles) {

            vrp.setCostConstraint(rangeConstraint/* rangeConstraint*1000 */);
            System.out.println("range:" + rangeConstraint);
            System.out.println("step:" + step);
            System.out.println("Vehicles:" + returnVector.size());
            System.out.println("Last:" + last);

            try {
                System.out.println("chamada ao solver");
                vrp.constructClosedTours("Depot");
                System.out.println("passou");
                tours = vrp.getTours();

                if (last == 0)
                    last = 1;
            }
            catch (SolutionNotFoundException e) {
                // TODO Auto-generated catch block
                System.out.println("solution not found : increasin distance");
                // e.printStackTrace();
                if (last != 0)
                    step /= 2;
                rangeConstraint += step;
                last = 0;

            }
            catch (VertexNotFoundException e) {
                System.out.println("Vertex not found");
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            returnVector.clear();

            for (int i = 0; i < tours.length; i++) {
                Vector<Point2d> path = new Vector<Point2d>();
                Enumeration<?> e = tours[i].elements();
                e.nextElement(); // Skip Vertex
                // PointIdoubleI customer_aux=
                // (PointIdoubleI)edge_aux.getToVertex().getValue();
                // customer_aux.getPoint2d()
                while (e.hasMoreElements()) {
                    EdgeI edge = (EdgeI) e.nextElement();
                    PointIdoubleI customer1 = (PointIdoubleI) edge.getToVertex().getValue();

                    PointIdoubleI customer2 = (PointIdoubleI) edge.getFromVertex().getValue();
                    meters += customer1.distanceTo(customer2);

                    path.add(customer1.getPoint2d());
                    /*
                     * int x1 = (int)customer1.screenPoint.x(); int y1 = (int)customer1.screenPoint.y(); int x2 =
                     * (int)customer2.screenPoint.x(); int y2 = (int)customer2.screenPoint.y();
                     */
                    // g.drawLine(x1, y1, x2, y2);
                    e.nextElement(); // Skip Vertex
                }
                returnVector.add(path);
            }

            if (last != 0) {
                if (returnVector.size() > n_vehicles) {
                    if (last < 0) {
                        step /= 2;
                    }

                    rangeConstraint += step;
                    last = 1;
                }

                if (returnVector.size() < n_vehicles) {

                    if (last > 0) {
                        step /= 2;
                    }

                    rangeConstraint -= step;

                    last = -1;
                }
            }
        }

        dist = meters;

        // ------------------------------------------------------------------------
        /*
         * if(returnVector.size()!=n_vehicles) return null; else
         */
        return returnVector;
    }

    /*
     * public static Vector<Vector<Point2d>> computePathsSingleDepot2( Point2d depot, Vector<Point2d> pointList, int
     * n_vehicles)
     * 
     * {
     * 
     * Vector<Vector<Point2d>> returnVector = computePathsSingleDepot2(depot, pointList, n_vehicles, Double.MAX_VALUE);
     * 
     * double rangeConstraint = dist; double step = rangeConstraint / n_vehicles;
     * 
     * int last = 0;
     * 
     * while (returnVector.size() != n_vehicles) {
     * 
     * returnVector = computePathsSingleDepot2(depot, pointList, n_vehicles, rangeConstraint);
     * 
     * System.out.println("Vehicles foud"+returnVector.size()); System.out.println("Total dist"+dist);
     * 
     * if (returnVector.size() > n_vehicles) { if (last < 0) { step /= 2; }
     * 
     * rangeConstraint += step; last = 1; }
     * 
     * if (returnVector.size() < n_vehicles) {
     * 
     * if (last > 0) { step /= 2; }
     * 
     * rangeConstraint -= step;
     * 
     * last = -1; } }
     * 
     * return returnVector;
     * 
     * }
     */

    /**
     * @param depot Vehicle depot (start and end location)
     * @param pointList (points to be visited)
     * @param n_vehicles (Number of available vehicles)
     * @return A list of waypoint lists to be visited by each vehicle
     */
    public static Vector<Vector<Point2d>> computePathsSingleDepot3(Point2d depot, Vector<Point2d> pointList,
            int n_vehicles) {

        int sizeVisitPoints = pointList.size();
        PointGraph pointGraph = new PointGraph();
        PointIdoubleI[] arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];

        ArrayList<Point2d> arrayCHull = new ArrayList<Point2d>(sizeVisitPoints + 1);
        arrayCHull.add(0, depot);

        for (int i = 1; i <= sizeVisitPoints; i++) {
            arrayCHull.add(i, pointList.get(i - 1));

        }
        Collections.sort(arrayCHull, new Comparator<Point2d>() {
            public int compare(Point2d pt1, Point2d pt2) {
                double r = pt1.x - pt2.x;
                if (r != 0) {
                    if (r < 0)
                        return -1;
                    else
                        return 1;
                }
                else {
                    if ((pt1.y - pt2.y) < 0)
                        return -1;
                    else
                        return 1;
                }
            }
        });
        for (int i = 0; i <= sizeVisitPoints; i++) {
            if (arrayCHull.get(i) == depot)
                System.out.println("encontrei depot");
        }

        ArrayList<Point2d> hull = CHull.cHull(arrayCHull);

        boolean depot_out_hull = false;
        for (int i = 0; i < hull.size(); i++) {
            if (hull.get(i) == depot)
                depot_out_hull = true;
        }

        Object key = "Depot";
        arrayVRP[0] = new PointIdoubleI(0, depot);
        try {
            pointGraph.addVertex(key, arrayVRP[0]);
        }
        catch (DuplicateVertexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (depot_out_hull) {
            System.out.println("Depot fora");
            hull = CHull.resizePath(n_vehicles + 1, hull, arrayCHull, depot);

            // int index_aux=-1; // must find depot on hull
            // for (int i=0;i<hull.size();i++)
            // {
            // if( hull.get(i)==d)
            // {
            // System.out.println("encontrei depot no resize hull");
            // index_aux=i; // FOUND
            // }
            // }
            try {
                for (int i = 1; i < sizeVisitPoints + 1; i++) {
                    arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];
                    key = new Integer(i);
                    arrayVRP[i] = new PointIdoubleI(0, pointList.get(i - 1));
                    for (int x = 0; x < hull.size(); x++) {
                        if (hull.get(x) == arrayVRP[i].getPoint2d()) {
                            System.out.println("encontrei listpoint em hull (ponto com carga 1)");
                            arrayVRP[i].setLoad(1);
                        }

                    }
                    pointGraph.addVertex(key, arrayVRP[i]);
                }
            }
            catch (DuplicateVertexException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        else {
            System.out.println("Depot dentro");
            hull = CHull.resizePath(n_vehicles, hull, arrayCHull, null);

            arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];
            try {
                for (int i = 1; i < sizeVisitPoints + 1; i++) {

                    key = new Integer(i);
                    arrayVRP[i] = new PointIdoubleI(0, pointList.get(i - 1));
                    for (int x = 0; x < hull.size(); x++) {
                        if (hull.get(x) == arrayVRP[i].getPoint2d()) {
                            System.out.println("encontrei listpoint em hull (ponto com carga 1)");
                            arrayVRP[i].setLoad(1);
                        }

                    }
                    pointGraph.addVertex(key, arrayVRP[i]);
                }
            }
            catch (DuplicateVertexException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        GraphI graph;
        graph = new MatrixGraph(pointGraph, null);
        graph.setSymmetric(false);

        Composite vrp;
        BestOf bestOf = new BestOf();
        int iterations = 10, strength = 4;
        drasys.or.graph.tsp.ImproveI subalgorithm = new drasys.or.graph.tsp.Us(5);
        try {
            bestOf.addConstruct(new ClarkeWright(iterations, strength, subalgorithm));
            // bestOf.addConstruct(new GillettMiller(iterations,strength,subalgorithm));
        }
        catch (VRPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ImproveI improve = new ImproveWithTSP(new drasys.or.graph.tsp.ThreeOpt());
        ;
        vrp = new Composite(bestOf, improve);
        vrp.setCostConstraint(Double.MAX_VALUE/* rangeConstraint*1000 */);

        vrp.setCapacityConstraint(1 /* capacityConstraint */);

        vrp.setGraph(graph);

        Vector<?>[] tours = null;
        try {
            System.out.println("chamada ao solver");
            vrp.constructClosedTours("Depot");
            System.out.println("passou");
            tours = vrp.getTours();
        }
        catch (SolutionNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (VertexNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (n_vehicles == tours.length)
            System.out.println("OK - One path for each vehicle");

        Vector<Vector<Point2d>> returnVector = new Vector<Vector<Point2d>>();

        for (int i = 0; i < tours.length; i++) {
            Vector<Point2d> path = new Vector<Point2d>();
            Enumeration<?> e = tours[i].elements();
            e.nextElement(); // Skip Vertex
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                PointIdoubleI customer1 = (PointIdoubleI) edge.getToVertex().getValue();

                path.add(customer1.getPoint2d());
                e.nextElement(); // Skip Vertex
            }
            returnVector.add(path);
        }
        if (returnVector.size() != n_vehicles)
            return null;
        else
            return returnVector;
    }

    public static int totalDist(Vector<?>[] tours) throws SolutionNotFoundException {
        int meters = 0;
        for (int i = 0; i < tours.length; i++) {
            Enumeration<?> e = tours[i].elements();
            e.nextElement(); // Skip Vertex
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                Customer customer1 = (Customer) edge.getToVertex().getValue();
                Customer customer2 = (Customer) edge.getFromVertex().getValue();
                meters += customer1.distanceTo(customer2);
                e.nextElement(); // Skip Vertex
            }
        }
        return meters;
    }
}
