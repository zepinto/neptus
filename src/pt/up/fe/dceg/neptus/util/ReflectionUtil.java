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
 * $Id:: ReflectionUtil.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarFile;

import pt.up.fe.dceg.neptus.console.plugins.SubPanelProvider;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.templates.AbstractPlanTemplate;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.MapTileProvider;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.renderer2d.tiles.Tile;

public class ReflectionUtil {

    public static List<Class<?>> getClassesForPackage(String pckgname) throws ClassNotFoundException {
        return getClassesForPackage(pckgname, true);
    }

    /**
     * Attempts to list all the classes in the specified package as determined by the context class loader
     * 
     * @param pckgname the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException if something went wrong
     */
    public static List<Class<?>> getClassesForPackage(String pckgname, boolean recursive) throws ClassNotFoundException {
        // This will hold a list of directories matching the pckgname. There may be more than one if a package is split
        // over multiple jars/paths
        ArrayList<File> directories = new ArrayList<File>();
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

        // System.out.println("evaluating "+pckgname);

        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = pckgname.replace('.', '/');
            // System.err.println(path);

            // Ask for all resources for the path
            Enumeration<URL> resources = cld.getResources(path);
            while (resources.hasMoreElements()) {
                String res = resources.nextElement().getPath();
                if (res.contains(".jar!")) {
                    // res = res.replaceAll("%20", " ");
                    // String path = res.substring(0, res.indexOf("r!")).replaceAll(".", replacement)
                    JarFile zf = new JarFile(URLDecoder.decode(res.substring(5, res.indexOf("!")), "UTF-8"));
                    Enumeration<?> enumer = zf.entries();
                    while (enumer.hasMoreElements()) {

                        String nextPath = enumer.nextElement().toString();
                        String next = nextPath.replaceAll("/", ".");
                        if (next.startsWith(pckgname) && next.endsWith(".class")) {
                            String clazz = next.substring(0, next.length() - 6);
                            classes.add(Class.forName(clazz));
                        }
                        else if (recursive) {

                            classes.addAll(getClassesForPackage(res, true));
                        }
                    }
                    zf.close();
                }
                else
                    directories.add(new File(URLDecoder.decode(res, "UTF-8")));
            }
        }
        catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname
                    + " does not appear to be a valid package (Null pointer exception)");
        }
        catch (UnsupportedEncodingException encex) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Unsupported encoding)");
        }
        catch (IOException ioex) {
            ioex.printStackTrace();
            throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }

        for (File directory : directories) {
            // System.out.println(directory);
            if (directory.exists()) {
                // Get the list of the files contained in the package
                String[] files = directory.list();
                for (String file : files) {
                    // we are only interested in .class files
                    if (file.endsWith(".class")) {
                        // removes the .class extension
                        classes.add(Class.forName(pckgname + '.' + file.substring(0, file.length() - 6)));
                    }
                    else if (recursive) {
                        classes.addAll(getClassesForPackage(pckgname + '.' + file, true));
                    }
                }
            }
            else {
                throw new ClassNotFoundException(pckgname + " (" + directory.getPath()
                        + ") does not appear to be a valid package");
            }
        }
        return classes;
    }

    public static List<Class<?>> getImplementationsForPackage(String pckgname, Class<?> iface)
            throws ClassNotFoundException {
        ArrayList<Class<?>> allClasses = new ArrayList<Class<?>>();
        allClasses.addAll(getClassesForPackage(pckgname));
        for (Class<?> c : allClasses) {
            boolean hasInterface = false;
            for (Class<?> itf : c.getInterfaces()) {
                if (itf.equals(iface)) {
                    hasInterface = true;
                    break;
                }
            }
            if (!hasInterface)
                allClasses.remove(c);
        }
        return allClasses;
    }

    public static String getCallerStamp() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        if (stack.length < 4)
            return "(?:?)";
        return "(" + stack[3].getFileName() + ":" + stack[3].getLineNumber() + ")";
    }

    public static Class<?>[] listSubPanels() {

        Vector<Class<?>> subpanels = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage("pt.up.fe.dceg.neptus.console.plugins");

            for (Class<?> clazz : classes) {
                if (clazz == null || clazz.getSimpleName().length() == 0)
                    continue;

                if (hasInterface(clazz, SubPanelProvider.class)) {
                    subpanels.add(clazz);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return subpanels.toArray(new Class<?>[0]);
    }

    public static Class<?>[] listMraVisualizations() {

        Vector<Class<?>> visualizations = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(MRAVisualization.class.getPackage().getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (hasInterface(c, MRAVisualization.class)) {
                    visualizations.add(c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return visualizations.toArray(new Class<?>[0]);

    }

    public static Class<?>[] listManeuvers() {
        Vector<Class<?>> maneuvers = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(Maneuver.class.getPackage().getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (hasAnySuperClass(c, Maneuver.class)) {
                    maneuvers.add(c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return maneuvers.toArray(new Class<?>[0]);
    }

    public static Class<?>[] listPlanTemplates() {

        Vector<Class<?>> templates = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(AbstractPlanTemplate.class.getPackage()
                    .getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (isSubclass(c, AbstractPlanTemplate.class) && c.getAnnotation(PluginDescription.class) != null) {
                    templates.add(c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return templates.toArray(new Class<?>[0]);

    }

    public static Class<?>[] listTileProviders() {
        Vector<Class<?>> tileProviders = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(Tile.class.getPackage().getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (hasAnnotation(c, MapTileProvider.class)) {
                    tileProviders.add(c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return tileProviders.toArray(new Class<?>[0]);
    }

    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.isAnnotationPresent(annotation);
    }

    public static boolean hasInterface(Class<?> clazz, Class<?> interf) {
        return interf.isAssignableFrom(clazz);
    }

    public static boolean isSubclass(Class<?> clazz, Class<?> superClass) {
        return superClass.isAssignableFrom(clazz);
    }

    public static boolean hasAnySuperClass(Class<?> clazz, Class<?> superClass) {
        return superClass.isAssignableFrom(clazz);
//        if (clazz == null || clazz.equals(Object.class))
//            return false;
//
//        if (clazz.getSuperclass() != null && clazz.getSuperclass().equals(superClass))
//            return true;
//        else if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(superClass)) {
//            return hasAnySuperClass(clazz.getSuperclass(), superClass);
//        }
//        return hasInterface(clazz.getSuperclass(), superClass);
    }

    public static void main(String[] args) throws Exception {

        for (Class<?> c : listPlanTemplates()) {
            System.out.println(c.getName());
        }

        for (Class<?> c : listMraVisualizations()) {
            System.out.println(c.getName());
        }

    }

}
