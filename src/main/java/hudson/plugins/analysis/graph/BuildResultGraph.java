package hudson.plugins.analysis.graph;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.Collection;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import hudson.plugins.analysis.core.ResultAction;
import hudson.plugins.analysis.core.BuildResult;

import hudson.util.ColorPalette;
import hudson.util.Graph;
import java.io.IOException;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Base class for build results graphs.
 *
 * @author Ulli Hafner
 */
public abstract class BuildResultGraph {
    private static final int A_DAY_IN_MSEC = 24 * 3600 * 1000;

    private String rootUrl = StringUtils.EMPTY;

    /**
     * Returns whether this graph is selectable.
     *
     * @return <code>true</code> if this graph is selectable, false otherwise
     */
    public boolean isSelectable() {
        return true;
    }

    /**
     * Returns the ID of this graph.
     *
     * @return the ID of this graph
     */
    public abstract String getId();

    /**
     * Returns a human readable label describing this graph.
     *
     * @return a label for this graph
     */
    public abstract String getLabel();

    /**
     * Returns the URL to an image that shows an example of the graph.
     *
     * @return a label for this graph
     */
    public String getExampleImage() {
        return "/plugin/" + getPlugin() + "/icons/" + getId() + ".png";
    }

    /**
     * Returns the plug-in that owns this graph and provides an example image.
     *
     * @return the plug-in that owns this graph and provides an example image
     */
    protected String getPlugin() {
        return "analysis-core";
    }

    /**
     * Returns whether this graph is visible.
     *
     * @return <code>true</code> if this graph is visible
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * Sets the root URL to the specified value.
     *
     * @param rootUrl the value to set
     */
    public void setRootUrl(final String rootUrl) {
        this.rootUrl = rootUrl;
    }

    /**
     * Returns the root URL.
     *
     * @return the root URL
     */
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Creates a PNG image trend graph with clickable map.
     *
     * @param configuration
     *            the configuration parameters
     * @param resultAction
     *            the result action to start the graph computation from
     * @param pluginName
     *            the name of the plug-in (project action URL) to create links
     *            to. If set to <code>null</code> then no links are created
     * @return the graph
     */
    public abstract JFreeChart create(final GraphConfiguration configuration,
            final ResultAction<? extends BuildResult> resultAction, @CheckForNull final String pluginName);

    /**
     * Creates a PNG image trend graph with clickable map.
     *
     * @param configuration
     *            the configuration parameters
     * @param resultActions
     *            the result actions to start the graph computation from
     * @param pluginName
     *            the name of the plug-in
     * @return the graph
     */
    public abstract JFreeChart createAggregation(final GraphConfiguration configuration,
            final Collection<ResultAction<? extends BuildResult>> resultActions, final String pluginName);

    /**
     * Computes the delta between two dates in days.
     *
     * @param first
     *            the first date
     * @param second
     *            the second date
     * @return the delta between two dates in days
     */
    public static long computeDayDelta(final Calendar first, final Calendar second) {
        return Math.abs((first.getTimeInMillis() - second.getTimeInMillis()) / A_DAY_IN_MSEC);
    }

    /**
     * Computes the delta between two dates in days.
     *
     * @param first
     *            the first date
     * @param second
     *            the second date (given by the build result)
     * @return the delta between two dates in days
     */
    public static long computeDayDelta(final Calendar first, final BuildResult second) {
        return computeDayDelta(first, second.getOwner().getTimestamp());
    }

    /**
     * Sets properties common to all plots of this plug-in.
     *
     * @param plot
     *            the plot to set the properties for
     */
    // CHECKSTYLE:OFF
    protected void setPlotProperties(final Plot plot) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));
    }
    // CHECKSTYLE:ON

    /**
     * Creates a XY graph from the specified data set.
     *
     * @param dataset
     *            the values to display
     * @return the created graph
     */
    public JFreeChart createXYChart(final XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYAreaChart(
                null,                      // chart title
                null,                      // unused
                "count",                   // range axis label
                dataset,                   // data
                PlotOrientation.VERTICAL,  // orientation
                false,                     // include legend
                true,                      // tooltips
                false                      // urls
        );
        chart.setBackgroundPaint(Color.white);

        XYPlot plot = chart.getXYPlot();
        plot.setRenderer(new XYDifferenceRenderer(ColorPalette.BLUE, ColorPalette.RED, false));
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        setPlotProperties(plot);

        return chart;
    }
    
    public class MyGraph extends Graph {
        private final GraphConfiguration configuration;
        private final ResultAction<?> lastAction;
        private final String pluginName;
        private final long timestamp;
        private final Collection<ResultAction<?>> actions;
        protected JFreeChart createGraph() {
            if (lastAction != null)
              return create(configuration, lastAction, pluginName);
            else
              return createAggregation(configuration, actions, pluginName);
        }
        public MyGraph(long timestamp, int defaultW, int defaultH, final GraphConfiguration configuration, final String pluginName, final ResultAction<?> lastAction) {
          super(timestamp, defaultW, defaultH);
          this.configuration = configuration;
          this.lastAction = lastAction;
          this.pluginName = pluginName;
          this.timestamp = timestamp;
          this.actions = null;
        }
        public MyGraph(long timestamp, int defaultW, int defaultH, final GraphConfiguration configuration, final String pluginName, final Collection<ResultAction<?>> actions) {
          super(timestamp, defaultW, defaultH);
          this.configuration = configuration;
          this.actions = actions;
          this.pluginName = pluginName;
          this.timestamp = timestamp;
          this.lastAction = null;
        }
        @Override
        public BufferedImage createImage(int width, int height) {
          return createGraph().createBufferedImage(width, height);
        }

//            For 3.0.0 M4 it is simplified 
//            public String createMap(String mapName, int width, int height){
//                ChartRenderingInfo info = new ChartRenderingInfo();
//                // Done to get the ChartRenderingIngo which contains ImageMap
//                createGraph().createBufferedImage(width, height, info);
//                return ChartUtilities.getImageMap(mapName, info);
//            }

        /**
          * Renders a clickable map.
          */
        @Override
        public void doMap(StaplerRequest req, StaplerResponse rsp) throws IOException {
            if (req.checkIfModified(timestamp, rsp)) {
                return;
            }

            String w = req.getParameter("width");
            if (w == null) {
                w = String.valueOf(300);
            }
            String h = req.getParameter("height");
            if (h == null) {
                h = String.valueOf(300);
            }

            ChartRenderingInfo info = new ChartRenderingInfo();
            // Done to get the ChartRenderingIngo which contains ImageMap
            createGraph().createBufferedImage(Integer.parseInt(w),Integer.parseInt(h),info);

            rsp.setContentType("text/plain;charset=UTF-8");
            rsp.getWriter().println(ChartUtilities.getImageMap("map", info));
        }
    }

    /**
     * Returns the new graph object that wraps the actual {@link JFreeChart}
     * into a PNG image or map.
     *
     * @param timestamp
     *            the last build time
     * @param configuration
     *            the graph configuration
     * @param pluginName
     *            the name of the plug-in
     * @param lastAction
     *            the last valid action for this project
     * @return the graph to render
     */
    public Graph getGraph(final long timestamp, final GraphConfiguration configuration, final String pluginName, final ResultAction<?> lastAction) {
        return new MyGraph(timestamp, configuration.getWidth(), configuration.getHeight(), configuration, pluginName, lastAction);
    }

    /**
     * Returns the new graph object that wraps the actual {@link JFreeChart}
     * into a PNG image or map.
     *
     * @param timestamp
     *            the last build time
     * @param configuration
     *            the graph configuration
     * @param pluginName
     *            the name of the plug-in
     * @param actions
     *            the actions to get the summary graph for
     * @return the graph to render
     */
    public Graph getGraph(final long timestamp, final GraphConfiguration configuration, final String pluginName, final Collection<ResultAction<?>> actions) {
        return new MyGraph(timestamp, configuration.getWidth(), configuration.getHeight(), configuration, pluginName, actions);
    }

    /**
     * Returns whether the graph is deactivated. If the graph is deactivated,
     * then no "enable graph" link is shown.
     *
     * @return <code>true</code> if the graph is deactivated, <code>false</code>
     *         otherwise
     */
    public boolean isDeactivated() {
        return false;
    }
}

