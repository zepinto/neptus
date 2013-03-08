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
 * Nov 13, 2012
 * $Id:: MraTimeSeriesPlot.java 9764 2013-01-24 17:50:33Z jqcorreia             $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MraChartPanel;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.LogStatisticsItem;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.llf.chart.LLFChart;

/**
 * @author zp
 *
 */
public abstract class MraTimeSeriesPlot implements LLFChart, LogMarkerListener {

    protected Vector<String> forbiddenSeries = new Vector<>();
    protected LsfIndex index;
    protected double timestep = 0;
    protected TimeSeriesCollection tsc = new TimeSeriesCollection();
    protected LinkedHashMap<String, TimeSeries> series = new LinkedHashMap<>();

    protected JFreeChart chart;
    protected MRAPanel mraPanel;

    /**
     * 
     */
    public MraTimeSeriesPlot(MRAPanel panel) {
        this.mraPanel = panel;
    }

    public Vector<String> getForbiddenSeries() {
        return forbiddenSeries;
    }

    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }

    public String getTitle() {
        return I18n.textf("%plotname plot", getName());
    }

    public final Collection<String> getSeriesNames() {
        LinkedHashSet<String> series = new LinkedHashSet<>();
        series.addAll(this.series.keySet());
        series.addAll(forbiddenSeries);
        Vector<String> col = new Vector<>();
        col.addAll(series);
        Collections.sort(col);
        return col;
    }
    
    public void addTrace(String trace) {
        series.put(trace, new TimeSeries(trace));
        tsc.addSeries(series.get(trace));
    }
 
    public void addValue(long timeMillis, String trace, double value) {

        if (forbiddenSeries.contains(trace))
            return;

        if (!series.containsKey(trace)) {
            addTrace(trace);
        }
        
        series.get(trace).addOrUpdate(new Millisecond(new Date(timeMillis)), value);
    }

    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        MraChartPanel fcp = new MraChartPanel(this, source, mraPanel);
        return fcp;
    }

    @Override
    public final boolean canBeApplied(IMraLogGroup source) {     
        return canBeApplied(source.getLsfIndex());
    }

    public abstract boolean canBeApplied(LsfIndex index);

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/graph.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.1;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return true;
    }

    public JFreeChart createChart() {
        return ChartFactory.createTimeSeriesChart(I18n.text(getTitle()), I18n.text("Time of day"), I18n.text(getVerticalAxisName()),
                tsc, true, true, false);
    }

    public String getVerticalAxisName() {
        return "";
    }

    @Override
    public JFreeChart getChart(IMraLogGroup source, double timestep) {
        this.timestep = timestep;
        this.index = source.getLsfIndex();
        tsc = new TimeSeriesCollection();
        series.clear();
        process(index);
        chart = createChart();
        
        // Do this here to make sure we have a built chart.. //FIXME FIXME FIXME
        for(LogMarker marker : mraPanel.getMarkers()) {
           addLogMarker(marker);
        }
        return chart;
    }

    public abstract void process(LsfIndex source);

    @Override
    public Vector<LogStatisticsItem> getStatistics() {
        return null;
    }
    
    public Type getType() {
        return Type.CHART;
    }
    
    @Override
    public void onCleanup() {
        mraPanel = null;
    }
    
    @Override
    public void onHide() {
        //nothing
    }
    
    public void onShow() {
        //nothing
    }

    
    @Override 
    public void addLogMarker(LogMarker e) {
        ValueMarker marker = new ValueMarker(e.timestamp);
        marker.setLabel(e.label);
        if(chart != null)
            chart.getXYPlot().addDomainMarker(marker);
    }
    
    @Override
    public void removeLogMarker(LogMarker e) {
        if(chart != null) {
            chart.getXYPlot().clearDomainMarkers();
        
            for (LogMarker m : mraPanel.getMarkers())
                addLogMarker(m);
        }
    }

    @Override
    public void GotoMarker(LogMarker marker) {
        
    }

}
