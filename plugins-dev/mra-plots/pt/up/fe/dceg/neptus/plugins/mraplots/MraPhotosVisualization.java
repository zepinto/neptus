/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 8, 2012
 * $Id:: MraPhotosVisualization.java 10080 2013-03-08 17:40:27Z zepinto         $:
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.imgscalr.Scalr;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.mra.replay.MraVehiclePosHud;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.VideoCreator;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Photos", icon="pt/up/fe/dceg/neptus/plugins/mraplots/camera.png")
public class MraPhotosVisualization extends JComponent implements MRAVisualization, LogMarkerListener {

    private static final long serialVersionUID = 1L;
    protected PriorityBlockingQueue<File> files = new PriorityBlockingQueue<>();
    protected PriorityBlockingQueue<LoadedImage> imgs = new PriorityBlockingQueue<>(100);
    protected int loadingThreads = 2;
    protected File photosDir;
    protected LsfIndex index;
    protected Image imageToDisplay = null;
    protected double curTime = 0;
    protected Vector<Thread> running = new Vector<>();
    protected File curFile = null;
    protected PhotoTimeline timeline = null;
    protected double speedMultiplier = 1.0;
    protected boolean brighten = false;
    protected boolean grayscale = false;
    protected boolean contrast = false;
    protected boolean sharpen = false;
    protected boolean showLegend = true;
    protected BufferedImageOp contrastOp = ImageUtils.contrastOp();
    protected BufferedImageOp sharpenOp = ImageUtils.sharpenOp();
    protected BufferedImageOp brightenOp = ImageUtils.brightenOp(1.2f, 0);
    protected BufferedImageOp grayscaleOp = ImageUtils.grayscaleOp();
    protected BufferedImageOp whiteBalanceOp = null;
    protected LinkedHashMap<File, SystemPositionAndAttitude> states = null;
    protected MRAPanel panel;
    protected MraVehiclePosHud hud;
    protected Point2D zoomPoint = null;
    protected boolean fullRes = false;
    protected Vector<LogMarker> markers = new Vector<>();
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("Photos") != null && source.getFile("Photos").isDirectory();
    }

    @Override
    public void onCleanup() {
        stop();
    }
    
    @Override
    public void addLogMarker(LogMarker marker) {
        repaint();
        synchronized (markers) {
            markers.clear();
            markers.addAll(panel.getMarkers());
            Collections.sort(markers);
        }
    }
    
    @Override
    public void removeLogMarker(LogMarker marker) {
        repaint();
        synchronized (markers) {
            markers.clear();
            markers.addAll(panel.getMarkers());
            Collections.sort(markers);
        }
    }
    
    @Override
    public void GotoMarker(LogMarker marker) {
        if (photosDir == null)
            return;
        File[] allFiles = photosDir.listFiles();
        Arrays.sort(allFiles);
        
        for (int i = 0; i < allFiles.length; i++) {
            if (timestampOf(allFiles[i]) >= marker.timestamp/1000) {
                setCurFile(allFiles[i]);
                return;
            }
        }        
    }

    /**
     * @return the curFile
     */
    public File getCurFile() {
        return curFile;
    }

    /**
     * @param curFile the curFile to set
     */
    public void setCurFile(File curFile) {
        this.curFile = curFile;
       
        try {
            stop();
            imageToDisplay = loadImage(curFile);
            curTime = timestampOf(curFile);
            if (timeline != null)
                timeline.fileChanged(curFile);
            repaint();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the curTime
     */
    public double getCurTime() {
        return curTime;
    }

    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        this.photosDir = source.getFile("Photos");
        this.index = source.getLsfIndex();
        this.hud = new MraVehiclePosHud(index, 150, 150);
        timeline = new PhotoTimeline(this);
        timeline.setVisible(false);
        loadStates();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(this, BorderLayout.CENTER);
        panel.add(timeline, BorderLayout.SOUTH);
        
        synchronized (markers) {
            markers.clear();
            markers.addAll(this.panel.getMarkers());
            Collections.sort(markers);
        }
        
        return panel;
    }
    
    protected void loadStates() {
        File[] files = photosDir.listFiles();
        Arrays.sort(files);
        int lastIndex = 0, stateId = index.getDefinitions().getMessageId("EstimatedState");
        int lastBDistanceIndex = 0, bdistId = index.getDefinitions().getMessageId("BottomDistance"), dvlId = index.getEntityId("DVL");
        
        states = new LinkedHashMap<>();
        for (int i = 0; i < files.length; i++) {
            int msgIndex = index.getMessageAtOrAfer(stateId, 0xFF, lastIndex, timestampOf(files[i]));
            
            if (msgIndex == -1) {
                states.put(files[i], null);                
            }
            else {
                lastIndex = msgIndex;
                IMCMessage m = index.getMessage(msgIndex);
                LocationType loc = new LocationType(Math.toDegrees(m.getDouble("lat")), Math.toDegrees(m.getDouble("lon")));
                loc.setDepth(m.getDouble("depth"));
                loc.translatePosition(m.getDouble("x"), m.getDouble("y"), m.getDouble("z"));
                loc.convertToAbsoluteLatLonDepth();
                SystemPositionAndAttitude state = new SystemPositionAndAttitude(loc, m.getDouble("phi"), m.getDouble("theta"), m.getDouble("psi"));
                if (m.getTypeOf("alt") == null) {
                    state.setU(Double.NaN);
                    int bdIndex = index.getMessageAtOrAfer(bdistId, dvlId, lastBDistanceIndex, timestampOf(files[i]));
                    
                    if (bdIndex != -1) {
                        state.setU(index.getMessage(bdIndex).getDouble("value"));
                        lastBDistanceIndex = bdIndex;
                    }
                }
                else
                    state.setU(m.getDouble("alt"));
                states.put(files[i], state);
                
            }
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                timeline.setVisible(true);
                revalidate();
            }
        });
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.25;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 16, 16);
    }

    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    
    public MraPhotosVisualization(MRAPanel panel) {
        this.panel = panel;
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                setCurFile(curFile);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                super.componentHidden(e);
                stop();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                super.componentShown(e);
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    showPopup(curFile, e);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    return;
                
                if (imageToDisplay == null)
                    return;
                zoomPoint = e.getPoint();    
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    return;
                
                zoomPoint = null;
                repaint();
            }
            
        });
        
        addMouseMotionListener(new MouseAdapter() {
            
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if (e.getButton() == MouseEvent.BUTTON3)
                    return;
                
                if (imageToDisplay == null)
                    return;
                zoomPoint = e.getPoint();
                repaint();
            }
        });
        
    }
    
    
    
    protected void showPopup(final File f, MouseEvent evt) {
        JPopupMenu popup = new JPopupMenu();
        popup.add("Add marker").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String resp = JOptionPane.showInputDialog(panel, "Enter marker name", "Add marker", JOptionPane.QUESTION_MESSAGE);
                if (resp == null)
                    return;
                SystemPositionAndAttitude state = states.get(f);
                LogMarker marker = new LogMarker(resp, timestampOf(f)*1000, state.getPosition().getLatitudeAsDoubleValueRads(), state.getPosition().getLongitudeAsDoubleValueRads(), 0, 0, 0, 0);
                panel.addMarker(marker);
            }
        });
        JMenuItem exportVideo = new JMenuItem("Export video", ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/mraplots/film.png"));
        popup.add(exportVideo);
        exportVideo.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                final File[] allFiles = photosDir.listFiles();
                Arrays.sort(allFiles);
                final double startTime = timestampOf(allFiles[0]);
                try {
                    final VideoCreator creator = new VideoCreator(new File(photosDir.getParentFile(), "Video.mp4"), 800, 600);
                    final ProgressMonitor monitor = new ProgressMonitor(panel, "Creating video", "Starting up", 0, allFiles.length);
                    
                    Thread videoMaker = new Thread(new Runnable() {
                        
                        @Override
                        public void run() {
                            fullRes = true;
                            Image watermark = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/mraplots/lsts-watermark.png");
                            //System.out.println(watermark.getWidth(null));
                            
                            BufferedImage tmp = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
                            Graphics2D g = tmp.createGraphics();
                            int count = 0;
                            double speedMult = speedMultiplier;
                            
                            for (File f: allFiles) {
                                if (monitor.isCanceled())
                                    break;
                                try {
                                    Image m = loadImage(f);
                                    g.drawImage(m, 0, 0, tmp.getWidth(), tmp.getHeight(), 0, 0, m.getWidth(null), m.getHeight(null), null);
                                    if (showLegend) {
                                        drawLegend(g, f);
                                        g.drawImage(hud.getImage(timestampOf(f)), 10, tmp.getHeight()-160, null);
                                        g.drawImage(watermark, tmp.getWidth()-55, tmp.getHeight()-55, null);
                                    }
                                    monitor.setNote("Processing "+f.getName());
                                    monitor.setProgress(count++);
                                    creator.addFrame(tmp, (long)((timestampOf(f)-startTime)*1000/speedMult));
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }                                
                            }
                            creator.closeStreams();
                            monitor.close();
                            fullRes = false;
                            GuiUtils.infoMessage(panel, "Production completed", "Video saved in "+new File(photosDir.getParentFile(), "Video.mp4").getAbsolutePath());
                        }
                    });
                    videoMaker.start();
                    
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(panel, ex);
                }
                
            }
        });
        popup.show(MraPhotosVisualization.this, evt.getX(), evt.getY());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Image copy = imageToDisplay;
        
       
        if (copy != null)
            g.drawImage(copy, 0, 0, getWidth(), getHeight(), 0, 0, copy.getWidth(this), copy.getHeight(this), this);
       
        int countMarkers = 0;
        for (int i = 0; i < markers.size(); i++) {
            
            if (markers.get(i).timestamp/1000 > curTime+2)
                break;
            if (markers.get(i).timestamp/1000 < curTime - 2)
                continue;
            
            countMarkers++;
            int alpha = (int)(127.5 * Math.abs(markers.get(i).timestamp/1000 - curTime));
            
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255,255,128,255-alpha));
            g.drawString(markers.get(i).label, 10, 150 + countMarkers*15);            
        }
        
        
        if (showLegend) {
            drawLegend((Graphics2D)g, curFile);
            g.drawImage(hud.getImage(curTime), 10, getHeight()-160, null);
        }
        
        if (zoomPoint != null && imageToDisplay != null) {
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int imgX =(int) ((zoomPoint.getX() / getWidth()) * imageToDisplay.getWidth(null));
            int imgY =(int) ((zoomPoint.getY() / getHeight()) * imageToDisplay.getHeight(null));
            ((Graphics2D)g).drawImage(imageToDisplay, getWidth() - 210, getHeight()-210, getWidth()-10, getHeight()-10, imgX-25, imgY-25, imgX+25, imgY+25, null);
        }
    }

    public double timestampOf(File f) {
        return Double.parseDouble(f.getName().substring(0, f.getName().lastIndexOf('.')));
    }

    protected void stop() {
        while (!running.isEmpty()) {
            Thread t = running.firstElement();
            t.interrupt();
            running.remove(t);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    protected void play(double startTime) {
        imgs.clear();
        files.clear();
        files.addAll(Arrays.asList(photosDir.listFiles()));
        while (!files.isEmpty() && timestampOf(files.peek()) < startTime) {
            try {
                files.take();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < loadingThreads; i++) {
            Thread loader = loadingThread("loader#" + i);
            loader.setDaemon(true);
            loader.start();
            running.add(loader);
        }

        Thread player = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (!files.isEmpty() || !imgs.isEmpty()) {
                        LoadedImage next = imgs.take();
                        curTime = next.timestamp;
                        curFile = next.file;
                        imageToDisplay = next.image;
                        repaint();
                        timeline.fileChanged(curFile);
                        Thread.sleep((long)(250 / speedMultiplier));
                    }
                }
                catch (Exception e) {
                   NeptusLog.pub().info("Player thread stopped");
                }
            }
        });
        player.setDaemon(true);
        running.add(0, player);
        player.start();
    }

    protected Image loadImage(File f) throws IOException {
        Vector<BufferedImageOp> ops = new Vector<>();
        if (whiteBalanceOp != null)
            ops.add(whiteBalanceOp);
        if (contrast)
            ops.add(contrastOp);
        if (sharpen)
            ops.add(sharpenOp);
        if (brighten)
            ops.add(brightenOp);
        if (grayscale)
            ops.add(grayscaleOp);
        
        final BufferedImageOp[] operations = ops.toArray(new BufferedImageOp[0]);

        BufferedImage original = ImageIO.read(f);
        if (fullRes) {
            if (!ops.isEmpty())
                original = Scalr.apply(original, operations);
        }
        else {
            original = Scalr.resize(original, getWidth(), getHeight(), operations);
        }
        
        return original;
    }
    
    
    protected void drawLegend(Graphics2D g, File f) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        SystemPositionAndAttitude state = states.get(f);
        if (state == null)
            return;
        
        String depth = GuiUtils.getNeptusDecimalFormat(1).format(state.getPosition().getDepth());
        String alt = Double.isNaN(state.getU())? "N/A" : GuiUtils.getNeptusDecimalFormat(1).format(state.getU());
        int roll = (int)Math.toDegrees(state.getRoll());
        int pitch = (int)Math.toDegrees(state.getPitch());
        int yaw = (int)Math.toDegrees(state.getYaw());
        String lat = CoordinateUtil.latitudeAsString(state.getPosition().getLatitudeAsDoubleValue(), false, 2);
        String lon = CoordinateUtil.longitudeAsString(state.getPosition().getLongitudeAsDoubleValue(), false, 2);
        
        Rectangle2D r = g.getFontMetrics().getStringBounds(lat, g);
        g.setColor(new Color(0,0,0,128));
        g.fill(new RoundRectangle2D.Double(10, 10, r.getWidth()+20, 125, 20, 20));
        g.setColor(Color.white);
        g.drawString(lat, 20, 30);
        g.drawString(lon, 20, 45);
        g.drawString("Depth: "+depth, 20, 60);
        
        g.drawString("Altitude: "+alt, 20, 75);
        g.drawString("Roll: "+roll, 20, 90);
        g.drawString("Pitch: "+pitch, 20, 105);
        g.drawString("Yaw: "+yaw, 20, 120);
    }

    protected Thread loadingThread(final String name) {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    Vector<BufferedImageOp> ops = new Vector<>();
                    if (whiteBalanceOp != null)
                        ops.add(whiteBalanceOp);
                    if (contrast)
                        ops.add(contrastOp);
                    if (sharpen)
                        ops.add(sharpenOp);
                    if (brighten)
                        ops.add(brightenOp);
                    if (grayscale)
                        ops.add(grayscaleOp);
                    final BufferedImageOp[] operations = ops.toArray(new BufferedImageOp[0]);

                    while (!files.isEmpty()) {
                        File nextFile = files.take();
                        LoadedImage img = new LoadedImage();
                        BufferedImage original = ImageIO.read(nextFile);
                        
                        if (fullRes) {
                            if (!ops.isEmpty())
                                img.image = Scalr.apply(original, operations);
                            else
                                img.image = original;
                        }
                        else {
                            img.image = Scalr.resize(original, getWidth(), getHeight(), operations);
                        }
                        
                        img.timestamp = timestampOf(nextFile);
                        img.file = nextFile;
                        while (imgs.size() > 10)
                            Thread.sleep(10);
                        addImage(img);
                    }
                    running.remove(this);
                }
                catch (Exception e) {
                    NeptusLog.pub().info("Thread '" + name + "' stopped");
                    running.remove(this);
                }
            }
        }, name);
        t.setDaemon(true);
        return t;
    }

    protected synchronized boolean addImage(LoadedImage img) {
        try {
            return imgs.offer(img, 60, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    class LoadedImage implements Comparable<LoadedImage> {
        @Override
        public int compareTo(LoadedImage o) {
            if (timestamp == o.timestamp)
                return 0;
            if (timestamp > o.timestamp)
                return 1;
            return -1;
        }

        public double timestamp;
        public Image image;
        public File file;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    /**
     * @return the speedMultiplier
     */
    public final double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * @param speedMultiplier the speedMultiplier to set
     */
    public final void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    /**
     * @return the photosDir
     */
    public File getPhotosDir() {
        return photosDir;
    }
    
    @Override
    public void onHide() {
        if (timeline.playToggle.isSelected())
            timeline.playToggle.doClick();
    }
    
    public void onShow() {
        //nothing
    }

}
