// This function opens a new window calling XPDF.xsp to create PDF of the chart
function exportChartToPDF(exportSize, width, height, mainChartType, chartID) {
	var url = "XPDF.xsp?exportSize=" + exportSize + "&mainChartType=" + mainChartType + "&chartID=" + chartID;
	if (exportSize == "A4P" || exportSize == "A4L") {
		if (width > 500 || height > 750) {
			if (confirm("The size of chart is not compatible with A4 (" + (exportSize == "A4P" ? "portrait" : "landscape") + ") paper size. Exporting it may result in chart being cropped.\nDo you wish to continue?")) {
				window.open(url);
			}
		} else {
			window.open(url);
		}
	} else {
		window.open(url);
	}
}