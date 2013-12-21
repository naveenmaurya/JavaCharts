package org.openntf.javacharts;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import com.ibm.xsp.designer.context.XSPContext;

/**
 * @author Naveen Maurya
 * This class creates JFreeChart based on the data present in the sessionScope variable and renders it as a PNG image.
 * It is used in the XPage "XJFC.xsp".
 */
public class XJFC {
	
	private FacesContext facesContext;
	private ExternalContext externalContext;
	private HttpServletResponse response;
	private OutputStream outStream; // Chart is written on this stream
	@SuppressWarnings("unchecked")
	private Map sessionScope; // Store all the chart replated data
	private XSPContext context;
	
	/**
	 * Initialize all the variables required for creation of image.
	 */
	public XJFC() {
		facesContext = FacesContext.getCurrentInstance();
		externalContext = facesContext.getExternalContext();
		response = (HttpServletResponse)externalContext.getResponse();
		try {
			outStream = response.getOutputStream();
		} catch (IOException e) {
			System.out.println("ERROR: org.openntf.javacharts.XJFC Constructor: Unable to get output stream.");
			e.printStackTrace();
		}
		sessionScope = externalContext.getSessionMap();
		context = XSPContext.getXSPContext(facesContext);
	}
	
	/**
	 * This method creates & writes the chart on the output stream.
	 */
	public void getChart() {
		String mainChartType = context.getUrlParameter("type"); // Type of the chart
		String chartID = context.getUrlParameter("chartID"); // ID of the chart
		String download = context.getUrlParameter("download");
		download = (download != null ? download.trim().toLowerCase() : null);
		
		// Get the width & height of the chart
		int width = ((Integer)sessionScope.get("width" + chartID)).intValue();
		int height = ((Integer)sessionScope.get("height" + chartID)).intValue();
		
		// Create chart
		JFreeChart chart = Common.getJFreeChart(mainChartType, sessionScope, chartID);
		
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", -1);
		response.setContentType("image/png");
		// Set the name of the image same as the title of the chart. This comes to use when user right clicks and saves the chart image.
		response.setHeader("Content-Disposition", (download.equals("true") ? "attachment" : "inline") + "; filename=\"" + sessionScope.get("title" + chartID).toString() + ".png\"" );

		try {
			// Write chart to output stream
			ChartUtilities.writeChartAsPNG(outStream, chart, width, height);
			// Close the stream
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			System.out.println("ERROR: org.openntf.javacharts.XJFC.getChart: Unable to write chart to stream and close it.");
			e.printStackTrace();
		}
		
		// This is required to indicate that response has been completed
		facesContext.responseComplete();
	}
}