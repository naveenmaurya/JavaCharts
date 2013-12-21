package org.openntf.javacharts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import lotus.domino.DateTime;
import lotus.domino.NotesException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.gantt.GanttCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.Align;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.SortOrder;
import com.ibm.xsp.binding.javascript.JavaScriptValueBinding;

/**
 * This class holds common functions used to by other classes. All the functions in this class are static.
 * @author Naveen Maurya
 */
public class Common {
	
	/**
	 * Initialization of this class is not possible
	 */
	private Common() {
	}
	
	/**
	 * Adds generated date-time to chart as its subtitle
	 * @param chart Object of JFreeChart
	 * @param includeGeneratedDateTime What to include in date / time - date, date & time, date & time with time zone OR hide
	 */
	private static void addGeneratedDateTime(JFreeChart chart, String includeGeneratedDateTime) {
		if (includeGeneratedDateTime.trim().equalsIgnoreCase("Do not show")) {
			return;
		}
		
		String generatedDateTimeFormat = "";
		if (includeGeneratedDateTime.trim().equalsIgnoreCase("Date")) {
			generatedDateTimeFormat = "'Generated on' MMM d, yyyy";
		} else if (includeGeneratedDateTime.trim().equalsIgnoreCase("Date & time")) {
			generatedDateTimeFormat = "'Generated on' MMM d, yyyy 'at' hh:mm a";
		} else if (includeGeneratedDateTime.trim().equalsIgnoreCase("Date & time with time zone")) {
			generatedDateTimeFormat = "'Generated on' MMM d, yyyy 'at' hh:mm a (z)";
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.addGeneratedDateTime: Illegal parameter for \"includeGeneratedDateTime\".");
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(generatedDateTimeFormat);
		TextTitle generatedDateTime = new TextTitle(dateFormat.format(new Date()));
		
		generatedDateTime.setPaint(Color.gray);
		
		Font font = generatedDateTime.getFont();
		generatedDateTime.setFont(font.deriveFont(11f));
		
		chart.addSubtitle(generatedDateTime);
	}
	
	/**
	 * This method is called by classes XJFC & XPDF for getting the chart image
	 * @param mainChartType Type of chart - Pie, histogram, etc
	 * @param sessionScope The XPage sessionScope object
	 * @param chartID ID of the chart
	 * @return Object of JFreeChart
	 */
	@SuppressWarnings("unchecked")
	public static JFreeChart getJFreeChart(String mainChartType, Map sessionScope, String chartID) {
		JFreeChart chart = null;
		if (mainChartType.trim().equalsIgnoreCase("pie")) {
			chart = getPieJFreeChart(sessionScope, chartID);
		} else if (mainChartType.trim().equalsIgnoreCase("bal")) {
			chart = getBALJFreeChart(sessionScope, chartID);
		} else if (mainChartType.trim().equalsIgnoreCase("gantt")) {
			chart = getGanttJFreeChart(sessionScope, chartID);
		} else if (mainChartType.trim().equalsIgnoreCase("histogram")) {
			chart = getHistogramJFreeChart(sessionScope, chartID);
		} else if (mainChartType.trim().equalsIgnoreCase("scatter")) {
			chart = getScatterJFreeChart(sessionScope, chartID);
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getJFreeChart: Illegal value in \"mainChartType\".");
		}
		return chart;
	}
	
	/**
	 * Create pie chart
	 * @param sessionScope The XPage sessionScope object
	 * @param chartID ID of the chart
	 * @return Object of JFreeChart
	 */
	@SuppressWarnings("unchecked")
	private static JFreeChart getPieJFreeChart(Map sessionScope, String chartID) {
		// Get the chart data from the sessionScope object
		Collection keys = getCollectionFromObject(sessionScope.get("chartItems" + chartID));
		Collection values = getCollectionFromObject(sessionScope.get("chartValues" + chartID));
		String title = sessionScope.get("title" + chartID).toString();
		String includeGeneratedDateTime = sessionScope.get("includeGeneratedDateTime" + chartID).toString();
		String displayPieValues = sessionScope.get("displayPieValues" + chartID).toString();
		String type = sessionScope.get("type" + chartID).toString();
		Object subTitle = sessionScope.get("subTitle" + chartID);
		String legendPosition = sessionScope.get("legendPosition" + chartID).toString();
		
		// Number of elements in keys and values should match. If it does not then throw exception
		if (keys.size() != values.size()) {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getBALJFreeChart: The number of elements in keys and values are different.");
		}
		
		// Create a data set and add all the keys and values to it
		DefaultPieDataset dataset = new DefaultPieDataset();
		Iterator itrKeys = keys.iterator();
		Iterator itrValues = values.iterator();
		while (itrKeys.hasNext()) {
			dataset.setValue(itrKeys.next().toString(), Double.parseDouble(String.valueOf(itrValues.next())));
		}
		
		// Depending on option selected create chart
		JFreeChart chart = null;
		if (type.trim().equalsIgnoreCase("Pie")) {
			chart = ChartFactory.createPieChart(title, dataset, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Pie 3D")) {
			chart = ChartFactory.createPieChart3D(title, dataset, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Ring")) {
			chart = ChartFactory.createRingChart(title, dataset, false, false, false);
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getBALJFreeChart: Illegal value in property \"type\".");
		}
		chart.setBackgroundPaint(Color.white);
		addSubTitle(subTitle, chart);
		PiePlot plot = (PiePlot)chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setOutlineVisible(false);
		plot.setCircular(true); // Pie 3D should be circular and NOT elliptical
		
		// Display labels in chart
		String labelFormat = null;
		if (displayPieValues.trim().equalsIgnoreCase("Only values")) {
			labelFormat = "{1}";
		} else if (displayPieValues.trim().equalsIgnoreCase("Only percentage")) {
			labelFormat = "{2}";
		} else if (displayPieValues.trim().equalsIgnoreCase("Values and percentage")) {
			labelFormat = "{1} ({2})";
		} else if (displayPieValues.trim().equalsIgnoreCase("None")) {
			// Do nothing. Keep "labelFormat" as null
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getBALJFreeChart: Illegal value in \"displayPieValues\"");
		}
		// If label is to be shown then add label generator else do not
		if (labelFormat != null) {
			plot.setLabelGenerator(new StandardPieSectionLabelGenerator(labelFormat, new DecimalFormat("##.##"), new DecimalFormat("##.##%")));
		} else {
			plot.setLabelGenerator(null);
		}

		// If user has selected 3D pie chart then make it 3D
		if (plot instanceof PiePlot3D) {
			PiePlot3D plot3d = (PiePlot3D)plot;
			plot3d.setForegroundAlpha(0.5f);
			plot3d.setBackgroundAlpha(0.2f);
		}
		
		addGeneratedDateTime(chart, includeGeneratedDateTime);
		addLegend(chart, legendPosition);
		
		// Return the created chart
		return chart;
	}
	
	/**
	 * Creates Bar-Area-Line (BAL) charts
	 * @param sessionScope The XPage sessionScope object
	 * @param chartID ID of the chart
	 * @return Object of JFreeChart
	 */
	@SuppressWarnings("unchecked")
	private static JFreeChart getBALJFreeChart(Map sessionScope, String chartID) {
		// Get the data from the sessionScope object
		Collection categories = getCollectionFromObject(sessionScope.get("chartCategories" + chartID));
		Collection values = getCollectionFromObject(sessionScope.get("chartValues" + chartID));
		Collection series = getCollectionFromObject(sessionScope.get("chartSeries" + chartID));
		String title = sessionScope.get("title" + chartID).toString();
		String includeGeneratedDateTime = sessionScope.get("includeGeneratedDateTime" + chartID).toString();
		String orientation = sessionScope.get("orientation" + chartID).toString();
		String labelX = sessionScope.get("labelx" + chartID).toString();
		String labelY = sessionScope.get("labely" + chartID).toString();
		String type = sessionScope.get("type" + chartID).toString();
		Object subTitle = sessionScope.get("subTitle" + chartID);
		boolean displayValues = ((Boolean)sessionScope.get("displayValues" + chartID)).booleanValue();
		String legendPosition = sessionScope.get("legendPosition" + chartID).toString();
		String backgroundColor = getValueFromSessionScope(sessionScope.get("backgroundColor" + chartID), "ffffff");
		String gridLineColor = sessionScope.get("gridLineColor" + chartID).toString();
		Object yAxisTickUnit = sessionScope.get("yAxisTickUnit" + chartID).toString();
		Object yAxisStartPoint = sessionScope.get("yAxisStartPoint" + chartID).toString();
		String backgroundImage = getValueFromSessionScope(sessionScope.get("backgroundImage" + chartID), "");
		String backgroundImageAlignment = getValueFromSessionScope(sessionScope.get("backgroundImageAlignment" + chartID), "Center");
		String backgroundImageFit = getValueFromSessionScope(sessionScope.get("backgroundImageFit" + chartID), "Do not fit");
		
		// Number of elements in series, categories and values should be same
		if (series.size() != values.size() || series.size() != categories.size()) {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getBALJFreeChart: The number of elements in series, categories and values are different.");
		}
		
		// Create a data set and add all the series, categories and values to it
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		Iterator itrSeries = series.iterator();
		Iterator itrCategories = categories.iterator();
		Iterator itrValues = values.iterator();
		while (itrSeries.hasNext()) {
			dataset.setValue(Double.parseDouble(String.valueOf(itrValues.next())), itrSeries.next().toString(), itrCategories.next().toString());
		}
		
		// Set orientation for the chart
		PlotOrientation pOrientation = null;
		if (orientation.equalsIgnoreCase("Horizontal")) {
			pOrientation = PlotOrientation.HORIZONTAL;
		} else if (orientation.equalsIgnoreCase("Vertical")) {
			pOrientation = PlotOrientation.VERTICAL;
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getBALJFreeChart: Invalid value in \"orientation\" property.");
		}
		
		// Depending on option selected create chart
		JFreeChart chart = null;
		if (type.trim().equalsIgnoreCase("Area")) {
			chart = ChartFactory.createAreaChart(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Bar") || type.trim().equalsIgnoreCase("Bar Layered")) {
			chart = ChartFactory.createBarChart(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Bar 3D")) {
			chart = ChartFactory.createBarChart3D(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Line")) {
			chart = ChartFactory.createLineChart(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Line 3D")) {
			chart = ChartFactory.createLineChart3D(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Stacked Area")) {
			chart = ChartFactory.createStackedAreaChart(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Stacked Bar")) {
			chart = ChartFactory.createStackedBarChart(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else if (type.trim().equalsIgnoreCase("Waterfall")) {
			chart = ChartFactory.createWaterfallChart(title, labelX, labelY, dataset, pOrientation, false, false, false);
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getBALJFreeChart: Illegal value in the property \"type\".");
		}
				
		addSubTitle(subTitle, chart);
		CategoryPlot plot = chart.getCategoryPlot();
		if (backgroundImage != null && !backgroundImage.trim().equals("")) {
			setBackgroundImage(chart, backgroundImage, backgroundImageAlignment, backgroundImageFit);
			plot.setBackgroundAlpha(0);
		} else {
			plot.setBackgroundPaint(Color.decode("0x" + backgroundColor));
			chart.setBackgroundPaint(Color.white);
		}
		plot.setRangeGridlinePaint(Color.decode("0x" + gridLineColor));
		plot.setRowRenderingOrder(SortOrder.DESCENDING); // Required for Bar (Layered) chart
		
		// Set font of labels on X & Y axis
		setXYAxisProperties(plot, yAxisTickUnit, yAxisStartPoint, null);
		
		// Set value on top of each bar
		CategoryItemRenderer ciRenderer = plot.getRenderer();
		if (displayValues) { // If values are to be displayed on bars then only display
			ciRenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", NumberFormat.getInstance()));
		}
		ciRenderer.setBaseItemLabelsVisible(true);
		Font font = ciRenderer.getBaseItemLabelFont();
		ciRenderer.setBaseItemLabelFont(font.deriveFont(10f));
		
		if (type.trim().equalsIgnoreCase("Bar Layered")) { // Bar Layered chart
			LayeredBarRenderer lbRenderer = new LayeredBarRenderer();
			lbRenderer.setDrawBarOutline(false);
			plot.setRenderer(lbRenderer);
		}
		
		addGeneratedDateTime(chart, includeGeneratedDateTime);
		
		// Legend is NOT required for Waterfall charts
		if (!type.trim().equalsIgnoreCase("Waterfall")) {
			addLegend(chart, legendPosition);
		}
		
		// Return the created chart
		return chart;
	}
	
	/**
	 * Create Gantt chart
	 * @param sessionScope The XPage sessionScope object
	 * @param chartID ID of the chart
	 * @return Object of JFreeChart
	 */
	@SuppressWarnings("unchecked")
	private static JFreeChart getGanttJFreeChart(Map sessionScope, String chartID) {
		// Get the data from the sessionScope object
		Collection taskSeriesList = getCollectionFromObject(sessionScope.get("taskSeries" + chartID));
		String title = sessionScope.get("title" + chartID).toString();
		String labelX = sessionScope.get("labelX" + chartID).toString();
		String labelY = sessionScope.get("labelY" + chartID).toString();
		String includeGeneratedDateTime = sessionScope.get("includeGeneratedDateTime" + chartID).toString();
		boolean taskCompletionData = ((Boolean)sessionScope.get("taskCompletionData" + chartID)).booleanValue();
		Object subTitle = sessionScope.get("subTitle" + chartID);
		boolean displayStartEndDates = ((Boolean)sessionScope.get("displayStartEndDates" + chartID)).booleanValue();
		String legendPosition = sessionScope.get("legendPosition" + chartID).toString();
		String backgroundColor = getValueFromSessionScope(sessionScope.get("backgroundColor" + chartID), "ffffff");
		String gridLineColor = sessionScope.get("gridLineColor" + chartID).toString();
		String backgroundImage = getValueFromSessionScope(sessionScope.get("backgroundImage" + chartID), "");
		String backgroundImageAlignment = getValueFromSessionScope(sessionScope.get("backgroundImageAlignment" + chartID), "Center");
		String backgroundImageFit = getValueFromSessionScope(sessionScope.get("backgroundImageFit" + chartID), "Do not fit");
		
		// Create a task series collection
		TaskSeriesCollection tsCollection = new TaskSeriesCollection();
		
		// Loop through all the task series entered by user
		Iterator itrTaskSeriesList = taskSeriesList.iterator();
		while (itrTaskSeriesList.hasNext()) {
			// Get individual task series
			Map taskSeries = getMapFromObject(itrTaskSeriesList.next());
			
			// Get details of task series entered by user
			String taskSeriesName = taskSeries.get("taskSeriesName").toString();
			Collection taskNameList = getCollectionFromObject(taskSeries.get("taskNameList"));
			Collection taskStartDateList = getCollectionFromObject(taskSeries.get("taskStartDateList"));
			Collection taskEndDateList = getCollectionFromObject(taskSeries.get("taskEndDateList"));
			Collection taskPercentCompletionList = null;
			if (taskCompletionData) {
				taskPercentCompletionList = getCollectionFromObject(taskSeries.get("taskPercentCompletionList"));
			}
			
			// The number of elements of name, start date, end date in a task series should be same
			if (taskNameList.size() != taskStartDateList.size() || taskNameList.size() != taskEndDateList.size()) {
				throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getGanttJFreeChart: The number of elements in taskNameList, taskStartDateList & taskEndDateList are different in \"" + taskSeriesName + "\".");
			}
			
			// Loop through name, start date, end date in task series and create individual task for each of them
			Iterator itrTaskNameList = taskNameList.iterator();
			Iterator itrTaskStartDateList = taskStartDateList.iterator();
			Iterator itrTaskEndDateList = taskEndDateList.iterator();
			Iterator itrTaskPercentCompletionList = null;
			if (taskCompletionData) {
				itrTaskPercentCompletionList = taskPercentCompletionList.iterator();
			}
			TaskSeries tSeries = new TaskSeries(taskSeriesName);
			while (itrTaskNameList.hasNext()) {
				Task task = new Task(itrTaskNameList.next().toString(),
						getDateFromObject(itrTaskStartDateList.next()),
						getDateFromObject(itrTaskEndDateList.next()));
				if (taskCompletionData) { // If completion information is to be shown then add the same
					task.setPercentComplete(Double.parseDouble(itrTaskPercentCompletionList.next().toString()) / 100);
				}
				tSeries.add(task);
			}
			
			// Add task series o task series collection
			tsCollection.add(tSeries);
		}
		
		// Create Gantt chart
		JFreeChart chart = ChartFactory.createGanttChart(title, labelX, labelY, tsCollection, false, false, false);
		addSubTitle(subTitle, chart);
		CategoryPlot plot = chart.getCategoryPlot();
		
		if (backgroundImage != null && !backgroundImage.trim().equals("")) {
			setBackgroundImage(chart, backgroundImage, backgroundImageAlignment, backgroundImageFit);
			plot.setBackgroundAlpha(0);
		} else {
			plot.setBackgroundPaint(Color.decode("0x" + backgroundColor));
			chart.setBackgroundPaint(Color.white);
		}
		
		plot.setRangeGridlinePaint(Color.decode("0x" + gridLineColor));
		
		// The size of tick labels on X & Y Axis is small to accommodate more text
		setXYAxisProperties(plot, null, null, null);
		
		// Set colors, font, label for task completion data
		GanttRenderer gRenderer = (GanttRenderer)plot.getRenderer();
		gRenderer.setShadowXOffset(1.5);
		gRenderer.setShadowYOffset(1.5);
		if (taskCompletionData) {
			gRenderer.setStartPercent(0.45);
			gRenderer.setEndPercent(0.65);
			gRenderer.setCompletePaint(new Color(0, 224, 0));
			gRenderer.setIncompletePaint(new Color(224, 224, 224));
		}
		gRenderer.setBaseItemLabelGenerator(new Common.GanttCategoryItemLabelGenerator(displayStartEndDates, taskCompletionData));
		gRenderer.setBaseItemLabelsVisible(true);
		
		Font font = gRenderer.getBaseItemLabelFont();
		gRenderer.setBaseItemLabelFont(font.deriveFont(9f));
		
		addGeneratedDateTime(chart, includeGeneratedDateTime);
		addLegend(chart, legendPosition);
		
		// Return the created chart
		return chart;
	}
	
	/**
	 * This class is used to generate the labels that appear on top of the Gantt chart and format it accordingly
	 * @author Naveen Maurya
	 *
	 */
	private static class GanttCategoryItemLabelGenerator implements CategoryItemLabelGenerator {
		private boolean taskCompletionData;
		private boolean displayStartEndDates;
		public GanttCategoryItemLabelGenerator(boolean displayStartEndDates, boolean taskCompletionData) {
			this.taskCompletionData = taskCompletionData;
			this.displayStartEndDates = displayStartEndDates;
		}
		
		public String generateColumnLabel(CategoryDataset dataset, int column) {
			return null;
		}

		// Generate labels as start to end date & percent completion of the activity
		public String generateLabel(CategoryDataset dataset, int row, int column) {
			GanttCategoryDataset ganttDataset = (GanttCategoryDataset)dataset;
			StringBuffer label = new StringBuffer("");
			Number completion = null;
			Date startDate = null, endDate = null;
			SimpleDateFormat dateFormat;
			if (displayStartEndDates) {
				startDate = new Date(ganttDataset.getStartValue(row, column).longValue());
				endDate = new Date(ganttDataset.getEndValue(row, column).longValue());
				dateFormat = new SimpleDateFormat("d-MMM");
				label.append(dateFormat.format(startDate) + " to " + dateFormat.format(endDate));
			}
			if (taskCompletionData) {
				completion = ganttDataset.getPercentComplete(row, column);
				if (displayStartEndDates) {
					label.append(" - ");
				}
				label.append(NumberFormat.getPercentInstance().format(completion));
			}
			
			return label.toString();
		}

		public String generateRowLabel(CategoryDataset dataset, int row) {
			return null;
		}
	}
	
	/**
	 * Create a histogram chart
	 * @param sessionScope The XPage sessionScope object
	 * @param chartID ID of the chart
	 * @return Object of JFreeChart
	 */
	@SuppressWarnings("unchecked")
	private static JFreeChart getHistogramJFreeChart(Map sessionScope, String chartID) {
		// Get the data from the sessionScope object
		String title = sessionScope.get("title" + chartID).toString();
		String includeGeneratedDateTime = sessionScope.get("includeGeneratedDateTime" + chartID).toString();
		String labelX = sessionScope.get("labelx" + chartID).toString();
		String labelY = sessionScope.get("labely" + chartID).toString();
		Object subTitle = sessionScope.get("subTitle" + chartID);
		String type = sessionScope.get("type" + chartID).toString();
		boolean displayValues = ((Boolean)sessionScope.get("displayValues" + chartID)).booleanValue();
		Collection histogramData = getCollectionFromObject(sessionScope.get("histogramData" + chartID));
		String legendPosition = sessionScope.get("legendPosition" + chartID).toString();
		String backgroundColor = getValueFromSessionScope(sessionScope.get("backgroundColor" + chartID), "ffffff");
		String gridLineColor = sessionScope.get("gridLineColor" + chartID).toString();
		Object yAxisTickUnit = sessionScope.get("yAxisTickUnit" + chartID).toString();
		String backgroundImage = getValueFromSessionScope(sessionScope.get("backgroundImage" + chartID), "");
		String backgroundImageAlignment = getValueFromSessionScope(sessionScope.get("backgroundImageAlignment" + chartID), "Center");
		String backgroundImageFit = getValueFromSessionScope(sessionScope.get("backgroundImageFit" + chartID), "Do not fit");
		int histogramDataItems = 0;
		
		HistogramDataset dataset = new HistogramDataset();
		// Set the type of histogram
		if (type.trim().equalsIgnoreCase("Frequency")) {
			dataset.setType(HistogramType.FREQUENCY);
		} else if (type.trim().equalsIgnoreCase("Relative Frequency")) {
			dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		} else if (type.trim().equalsIgnoreCase("Scale Area to 1")) {
			dataset.setType(HistogramType.SCALE_AREA_TO_1);
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getHistogramJFreeChart: Illegal value present in the \"type\" property.");
		}
		
		// Loop through all the observation and add them to the chart
		Iterator itrHistogramData = histogramData.iterator();
		while (itrHistogramData.hasNext()) {
			Map mapHistogramData = getMapFromObject(itrHistogramData.next());
			
			Collection observations = getCollectionFromObject(mapHistogramData.get("observations"));
			String observationsLabel = mapHistogramData.get("observationsLabel").toString();
			int bins = ((Integer)mapHistogramData.get("bins")).intValue();
			double binRangeLower = ((Double)mapHistogramData.get("binRangeLower")).doubleValue();
			double binRangeUpper = ((Double)mapHistogramData.get("binRangeUpper")).doubleValue();
			
			// Get the double value array
			double doubleValues[] = new double[observations.size()];
			Iterator itrObservations = observations.iterator();
			for (int i=0 ; itrObservations.hasNext() ; i++) {
				doubleValues[i] = Double.parseDouble(itrObservations.next().toString());
			}
			// Add the data in histogram and generate chart
			dataset.addSeries(observationsLabel, doubleValues, bins, binRangeLower, binRangeUpper);
			
			histogramDataItems++;
		}
		
		// Create histogram chart
		JFreeChart chart = ChartFactory.createHistogram(title, labelX, labelY, dataset, PlotOrientation.VERTICAL, false, false, false);
		
		// Set formatting for chart
		addSubTitle(subTitle, chart);
		XYPlot plot = (XYPlot)chart.getPlot();
		if (backgroundImage != null && !backgroundImage.trim().equals("")) {
			setBackgroundImage(chart, backgroundImage, backgroundImageAlignment, backgroundImageFit);
			plot.setBackgroundAlpha(0);
		} else {
			plot.setBackgroundPaint(Color.decode("0x" + backgroundColor));
			chart.setBackgroundPaint(Color.white);
		}
		plot.setRangeGridlinePaint(Color.decode("0x" + gridLineColor));
		
		// If there are more than 1 histograms being displayed then make bars partially transparent
		if (histogramDataItems > 1) {
			plot.setForegroundAlpha(0.6F);
		}
		
		// Font for X & Y axis
		setXYAxisProperties(plot, yAxisTickUnit, null, null);
		
		// Formatting for the bars in histogram
		XYBarRenderer xybRenderer = (XYBarRenderer) plot.getRenderer();
		xybRenderer.setShadowVisible(false); // No shadow
		xybRenderer.setDrawBarOutline(true); // Outline
		xybRenderer.setBarPainter(new StandardXYBarPainter()); // Flat color for bars
		xybRenderer.setMargin(0.1); // Spacing between bars
		if (displayValues) { // If values are to be displayed on the bars
			xybRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator());
			xybRenderer.setBaseItemLabelsVisible(true);
			Font font = xybRenderer.getBaseItemLabelFont();
			xybRenderer.setBaseItemLabelFont(font.deriveFont(9f));
		}
		
		addGeneratedDateTime(chart, includeGeneratedDateTime);
		addLegend(chart, legendPosition);
		
		return chart;
	}
	
	/**
	 * Create scatter chart
	 * @param sessionScope The XPage sessionScope object
	 * @param chartID ID of the chart
	 * @return Object of JFreeChart
	 */
	@SuppressWarnings("unchecked")
	private static JFreeChart getScatterJFreeChart(Map sessionScope, String chartID) {
		// Get the data from the sessionScope object
		String title = sessionScope.get("title" + chartID).toString();
		String includeGeneratedDateTime = sessionScope.get("includeGeneratedDateTime" + chartID).toString();
		String labelX = sessionScope.get("labelx" + chartID).toString();
		String labelY = sessionScope.get("labely" + chartID).toString();
		Object subTitle = sessionScope.get("subTitle" + chartID);
		String legendPosition = sessionScope.get("legendPosition" + chartID).toString();
		String orientation = sessionScope.get("orientation" + chartID).toString();
		String scatterDisplay = sessionScope.get("scatterDisplay" + chartID).toString();
		Collection scatterData = getCollectionFromObject(sessionScope.get("scatterData" + chartID));
		String backgroundColor = getValueFromSessionScope(sessionScope.get("backgroundColor" + chartID), "ffffff");
		String gridLineColor = sessionScope.get("gridLineColor" + chartID).toString();
		Object yAxisTickUnit = sessionScope.get("yAxisTickUnit" + chartID).toString();
		Object xAxisStartPoint = sessionScope.get("xAxisStartPoint" + chartID).toString();
		Object yAxisStartPoint = sessionScope.get("yAxisStartPoint" + chartID).toString();
		String backgroundImage = getValueFromSessionScope(sessionScope.get("backgroundImage" + chartID), "");
		String backgroundImageAlignment = getValueFromSessionScope(sessionScope.get("backgroundImageAlignment" + chartID), "Center");
		String backgroundImageFit = getValueFromSessionScope(sessionScope.get("backgroundImageFit" + chartID), "Do not fit");
		
		// Set orientation for the chart
		PlotOrientation pOrientation = null;
		if (orientation.equalsIgnoreCase("Horizontal")) {
			pOrientation = PlotOrientation.HORIZONTAL;
		} else if (orientation.equalsIgnoreCase("Vertical")) {
			pOrientation = PlotOrientation.VERTICAL;
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getScatterJFreeChart: Invalid value in \"orientation\" property.");
		}
		
		// Create data set for scatter chart
		DefaultXYDataset dataset = new DefaultXYDataset();
		// Loop through all the data provided by user and put it in data set
		Iterator itrScatterData = scatterData.iterator();
		while (itrScatterData.hasNext()) {
			Map mapScatterData = getMapFromObject(itrScatterData.next());
			
			String scatterDataLabel = mapScatterData.get("scatterDataLabel").toString();
			Collection xPoints = getCollectionFromObject(mapScatterData.get("xPoints"));
			Collection yPoints = getCollectionFromObject(mapScatterData.get("yPoints"));
			
			if (xPoints.size() != yPoints.size()) { // Number of points in X & Y should be same
				throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getScatterJFreeChart: The number of elements in xPoints & yPoints are different in " + scatterDataLabel);
			}
			
			double doubleValues[][] = new double[2][xPoints.size()];
			Iterator itrXPoints = xPoints.iterator();
			Iterator itrYPoints = yPoints.iterator();
			for (int i=0 ; itrXPoints.hasNext() ; i++) {
				doubleValues[0][i] = Double.parseDouble(itrXPoints.next().toString());
				doubleValues[1][i] = Double.parseDouble(itrYPoints.next().toString());
			}
			
			dataset.addSeries(scatterDataLabel, doubleValues);
		}
		
		// Create chart
		JFreeChart chart = ChartFactory.createScatterPlot(title, labelX, labelY, dataset, pOrientation, false, false, false);
		addSubTitle(subTitle, chart);
		XYPlot plot = (XYPlot)chart.getPlot();
		if (backgroundImage != null && !backgroundImage.trim().equals("")) {
			setBackgroundImage(chart, backgroundImage, backgroundImageAlignment, backgroundImageFit);
			plot.setBackgroundAlpha(0);
		} else {
			plot.setBackgroundPaint(Color.decode("0x" + backgroundColor));
			chart.setBackgroundPaint(Color.white);
		}
		plot.setRangeGridlinePaint(Color.decode("0x" + gridLineColor));
		plot.setDomainGridlinePaint(Color.decode("0x" + gridLineColor));
		
		// Display of scatter data - Dots OR Shapes
		if (scatterDisplay.trim().equalsIgnoreCase("Dots")) {
			XYDotRenderer dotRenderer = new XYDotRenderer();
			dotRenderer.setDotHeight(3);
			dotRenderer.setDotWidth(3);
			plot.setRenderer(dotRenderer);
		} else if (scatterDisplay.trim().equalsIgnoreCase("Shapes")) {
			XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer)plot.getRenderer();
			xyRenderer.setSeriesOutlinePaint(0, Color.black);
			xyRenderer.setUseOutlinePaint(true);
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getScatterJFreeChart: Invalid value in \"scatterDisplay\" property.");
		}
		
		// Font for X & Y axis
		setXYAxisProperties(plot, yAxisTickUnit, yAxisStartPoint, xAxisStartPoint);
		
		addGeneratedDateTime(chart, includeGeneratedDateTime);
		addLegend(chart, legendPosition);
		
		return chart;
	}
	
	/**
	 * java.util.Date from Object
	 * @param obj Object which can be cast to Date
	 * @return Object of java.uti.Date
	 */
	private static Date getDateFromObject(Object obj) {
		Date date = null;
		if (obj instanceof DateTime) { // If it is a Notes date-time object
			try {
				date = ((DateTime)obj).toJavaDate(); // Convert to java date
			} catch (NotesException e) {
				System.out.println("ERROR: org.openntf.javacharts.Common.getDateFromObject: Unable to convert lotus.domino.DateTime to java.util.Date.");
				e.printStackTrace();
			}
		} else if (obj instanceof Date) { // If it is hava.util.Date
			date = (Date)obj;
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getDateFromObject: Date needs to be in either java.util.Date OR lotus.domino.DateTime.");
		}
		date = removeTimeComponent(date); // Remove the time component from date
		return date;
	}
	
	/**
	 * Get java.util.Collection from Object
	 * @param obj Object which can be cast to Collection
	 * @return Object of java.util.Collection
	 */
	@SuppressWarnings("unchecked")
	private static Collection getCollectionFromObject(Object obj) {
		Collection c = null;
		if (obj instanceof JavaScriptValueBinding) { // This case will occur if the property is inside a group
			// NOTE: Only when the entire group is stored in sessionScope then only the type of object 
			// is com.ibm.xsp.binding.javascript.JavaScriptValueBinding else it is an implementation of 
			// java.util.Collection
			JavaScriptValueBinding jvb = ((JavaScriptValueBinding)obj);
			c = (Collection)jvb.getValue(FacesContext.getCurrentInstance());
		} else if (obj instanceof Collection) { // This case will occur when property is not inside a group
			c = (Collection)obj;
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getCollectionFromObject: Object needs to be either com.ibm.xsp.binding.javascript.JavaScriptValueBinding OR java.util.Collection.");
		}
		return c;
	}
	
	/**
	 * Get java.util.Map from Object
	 * @param obj Object which can be cast to Map
	 * @return Object of java.util.Map
	 */
	@SuppressWarnings("unchecked")
	private static Map getMapFromObject(Object obj) {
		Map m = null;
		if (obj instanceof Map) {
			m = (Map)obj;
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.getMapFromObject: Object needs to be java.util.Map.");
		}
		return m;
	}
	
	/**
	 * Remove the time component from java.util.Date
	 * @param date Date object from which time needs to be removed
	 * @return Object of java.uti.Date
	 */
	private static Date removeTimeComponent(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
	
	/**
	 * Add sub title for the chart
	 * @param subTitle Sub-title of the chart
	 * @param chart Chart in which sub title needs to be added
	 */
	private static void addSubTitle(Object subTitle, JFreeChart chart) {
		if (subTitle != null && !subTitle.toString().trim().equalsIgnoreCase("")) {
			chart.addSubtitle(new TextTitle(subTitle.toString()));
		}
	}
	
	/**
	 * Set the font size of the labels on X & Y axis to smaller size and set the tick units on Y-Axis
	 * @param plot Plot of the chart
	 * @param xAxisStartPoint Start point for X axis
	 * @param yAxisStartPoint Start point for Y axis
	 * @param tickUnit Unit after which the ticks needs to be generated on Y-Axis
	 */
	private static void setXYAxisProperties(Plot plot, Object yAxisTickUnit, Object yAxisStartPoint, Object xAxisStartPoint) {
		Axis xAxis = null;
		Axis yAxis = null;
		
		if (plot instanceof CategoryPlot) {
			xAxis = ((CategoryPlot)plot).getDomainAxis();
			yAxis = ((CategoryPlot)plot).getRangeAxis();
			
		} else if (plot instanceof XYPlot) {
			xAxis = ((XYPlot)plot).getDomainAxis();
			yAxis = ((XYPlot)plot).getRangeAxis();
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.setXYAxisProperties: Illegal sub-class of org.jfree.chart.plot.Plot passed on to method setXYAxisLabelFont. It can be either org.jfree.chart.plot.CategoryPlot or org.jfree.chart.plot.XYPlot.");
		}
		
		// Set the font size of the tick labels
		Font font = xAxis.getTickLabelFont();
		xAxis.setTickLabelFont(font.deriveFont(11f));
		font = yAxis.getTickLabelFont();
		yAxis.setTickLabelFont(font.deriveFont(11f));
		
		// Set the Y-Axis tick units
		if (yAxisTickUnit != null) {
			String temp = yAxisTickUnit.toString();
			if (!temp.trim().equalsIgnoreCase("Auto-generated")) {
				int tickUnit = 0;
				try {
					tickUnit = Integer.parseInt(temp);
				} catch (NumberFormatException nfe) {}
				if (yAxis instanceof NumberAxis && tickUnit != 0) {
					((NumberAxis)yAxis).setTickUnit(new NumberTickUnit(tickUnit));
				}
			}
		}
		
		if (xAxisStartPoint != null) {
			setStartPoint(xAxis, xAxisStartPoint);
		}
		if (yAxisStartPoint != null) {
			setStartPoint(yAxis, yAxisStartPoint);
		}
	}
	
	private static void setStartPoint(Axis axis, Object axisStartPoint) {
		if (axisStartPoint != null) {
			String temp = axisStartPoint.toString();
			if (!temp.trim().equalsIgnoreCase("Auto-generated")) {
				double startPoint = 0;
				try {
					startPoint = Double.parseDouble(temp);
				} catch (NumberFormatException nfe) {}
				if (axis instanceof NumberAxis) {
					((NumberAxis)axis).setLowerBound(startPoint);
				}
			}
		}
	}
	
	/**
	 * To add the legend at required position
	 * @param chart Chart in which legend needs to be added
	 * @param legendPosition Position where the legend is to be added
	 */
	private static void addLegend(JFreeChart chart, String legendPosition) {
		if (legendPosition.equalsIgnoreCase("Hide Legend")) {
			return; // If legend is to be hidden then do not do anything
		}
		
		LegendTitle legendTitle = new LegendTitle(chart.getPlot());
		if (legendPosition.equalsIgnoreCase("Right")) {
			legendTitle.setPosition(RectangleEdge.RIGHT);
		} else if (legendPosition.equalsIgnoreCase("Bottom")) {
			legendTitle.setPosition(RectangleEdge.BOTTOM);
		} else if (legendPosition.equalsIgnoreCase("Top")) {
			legendTitle.setPosition(RectangleEdge.TOP);
		} else if (legendPosition.equalsIgnoreCase("Left")) {
			legendTitle.setPosition(RectangleEdge.LEFT);
		} else {
			throw new IllegalArgumentException("ERROR: org.openntf.javacharts.Common.addLegend: Illegal value present in \"legendPosition\". Value an be either Right, Left, Bottom or Top.");
		}
		
		BlockContainer bContainer = new BlockContainer(new BorderArrangement());
		bContainer.setFrame(new BlockBorder(1.0D, 1.0D, 1.0D, 1.0D));
		bContainer.add(legendTitle.getItemContainer());
		legendTitle.setWrapper(bContainer);
		
		chart.addSubtitle(legendTitle);
	}
	
	/**
	 * To get value from session scope variables
	 * @param o Object from session scope
	 * @param valueIfNull Value if the object is null
	 * @return
	 */
	private static String getValueFromSessionScope(Object o, String valueIfNull) {
		return ((o == null) ? valueIfNull : o.toString());
	}
	
	/**
	 * Sets the background image of the chart
	 * @param chart Chart object on which background image needs to be set
	 * @param imageName Name of the image
	 * @param imageAlignment Alignment of the image
	 * @param imageFit Fitting image on chart
	 */
	private static void setBackgroundImage(JFreeChart chart, String imageName, String imageAlignment, String imageFit) {
		InputStream inStream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(imageName);
		
		if (inStream == null) {
			throw new RuntimeException("ERROR: org.openntf.javacharts.Common.setBackgroundImage: Unable to find image resource at - " + imageName);
		}
		
		Image img = null;
		try {
			img = ImageIO.read(inStream);
		} catch (IOException e) {
			System.out.println("ERROR: org.openntf.javacharts.Common.setBackgroundImage: Error while getting image");
			e.printStackTrace();
		}
		chart.setBackgroundImage(img);
		
		int imageAlignFit = 0;
		imageFit = imageFit.trim();
		if (imageFit.equalsIgnoreCase("Do not fit")) {
			// Do nothing
		} else if (imageFit.equalsIgnoreCase("Fit entire chart")) {
			imageAlignFit = Align.FIT;
		} else if (imageFit.equalsIgnoreCase("Fit horizontally")) {
			imageAlignFit = Align.FIT_HORIZONTAL;
		} else if (imageFit.equalsIgnoreCase("Fit vertically")) {
			imageAlignFit = Align.FIT_VERTICAL;
		}
		imageAlignment = imageAlignment.trim();
		if (imageAlignment.equalsIgnoreCase("Center")) {
			imageAlignFit = imageAlignFit + Align.CENTER;
		} else if (imageAlignment.equalsIgnoreCase("East")) {
			imageAlignFit = imageAlignFit + Align.EAST;
		} else if (imageAlignment.equalsIgnoreCase("North")) {
			imageAlignFit = imageAlignFit + Align.NORTH;
		} else if (imageAlignment.equalsIgnoreCase("North-East")) {
			imageAlignFit = imageAlignFit + Align.NORTH_EAST;
		} else if (imageAlignment.equalsIgnoreCase("North-West")) {
			imageAlignFit = imageAlignFit + Align.NORTH_WEST;
		} else if (imageAlignment.equalsIgnoreCase("South-East")) {
			imageAlignFit = imageAlignFit + Align.SOUTH_EAST;
		} else if (imageAlignment.equalsIgnoreCase("South-West")) {
			imageAlignFit = imageAlignFit + Align.SOUTH_WEST;
		} else if (imageAlignment.equalsIgnoreCase("South")) {
			imageAlignFit = imageAlignFit + Align.SOUTH;
		} else if (imageAlignment.equalsIgnoreCase("West")) {
			imageAlignFit = imageAlignFit + Align.WEST;
		}
		chart.setBackgroundImageAlignment(imageAlignFit);
	}
	
	/*
	// This function is used for testing purpose ONLY to print all the entries
	// present in sessionScope variable
	@SuppressWarnings({ "unused", "unchecked" })
	private static void printSessionScopeEntries(Map sessionScope) {
		java.util.Set s = sessionScope.keySet();
		Iterator itr = s.iterator();
		System.out.println("ELEMENTS IN SESSION SCOPE:");
		while (itr.hasNext()) {
			System.out.println(itr.next().toString());
		}
	}
	*/
}