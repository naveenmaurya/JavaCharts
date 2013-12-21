package org.openntf.javacharts;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import com.ibm.xsp.designer.context.XSPContext;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;

/**
 * This class creates PDF from chart and is used in the XPage - XPDF
 * @author Naveen Maurya
 *
 */
public class XPDF {
	
	private FacesContext facesContext;
	private ExternalContext externalContext;
	private HttpServletResponse response;
	private OutputStream outStream;
	@SuppressWarnings("unchecked")
	private Map sessionScope;
	private XSPContext context;

	/**
	 * Initialize variables
	 */
	public XPDF() {
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
	 * Export the chart to PDF
	 */
	public void exportToPDF() {
		String paperSize = context.getUrlParameter("exportSize");
		String mainChartType = context.getUrlParameter("mainChartType");
		String chartID = context.getUrlParameter("chartID");
		System.out.println("chartID = " + chartID);
		
		// Create chart
		JFreeChart chart = Common.getJFreeChart(mainChartType, sessionScope, chartID);
		
		// Get the dimension of chart
		int width = ((Integer)sessionScope.get("width" + chartID)).intValue();
		int height = ((Integer)sessionScope.get("height" + chartID)).intValue();
		
		response.setContentType("application/pdf");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", -1);
		// Set the name of the file as title of the chart.
		response.setHeader("Content-Disposition", "attachment; filename=\"" + sessionScope.get("title" + chartID) + ".pdf\"" );
		
		// Create a buffered image for chart
		BufferedImage bImage = chart.createBufferedImage(width, height);
		
		// Create PDF document
		Document document = null;
		if (paperSize.equalsIgnoreCase("A4P")) {
			document = new Document(PageSize.A4); // A4 - Portrait
		} else if (paperSize.equalsIgnoreCase("A4L")) {
			document = new Document(PageSize.A4.rotate()); // A4 - landscape
		} else if (paperSize.equalsIgnoreCase("ImageSize")) {
			document = new Document(new Rectangle(width, height), 0, 0, 0, 0); // Image size
		}
		
		// Get PDF writer to write PDF document to stream
		PdfWriter pdfWriter = null;
		try {
			pdfWriter = PdfWriter.getInstance(document, outStream);
		} catch (DocumentException e) {
			System.out.println("ERROR: org.openntf.javacharts.XPDF.exportToPDF: Unable to initialize PdfWriter.");
			e.printStackTrace();
		}
		
		// Add the buffered image to PDF document in PNG format
		document.open();
		Image image = null;
		try {
			image = Image.getInstance(ChartUtilities.encodeAsPNG(bImage));
			document.add(image);
		} catch (Exception e) {
			System.out.println("ERROR: org.openntf.javacharts.XPDF.exportToPDF: Unable to add chart to document.");
			e.printStackTrace();
		}
		
		// Close PDF document and writer to generate PDF file
		document.close();
		pdfWriter.close();
		
		try {
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			System.out.println("ERROR: org.openntf.javacharts.XPDF.exportToPDF: Unable to flush and/or close stream.");
			e.printStackTrace();
		}
		facesContext.responseComplete();
	}
}