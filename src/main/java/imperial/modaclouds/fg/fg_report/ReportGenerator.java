package imperial.modaclouds.fg.fg_report;

import static net.sf.dynamicreports.report.builder.DynamicReports.*;
import imperial.modaclouds.fg.fg_report.ComplexData;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.jasper.builder.export.Exporters;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.MultiPageListBuilder;
import net.sf.dynamicreports.report.builder.component.VerticalListBuilder;
import net.sf.dynamicreports.report.builder.style.FontBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.SplitType;
import net.sf.dynamicreports.report.constant.TimePeriod;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.definition.chart.DRIChartCustomizer;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;


public class ReportGenerator {

	private static ReportData data = null;

	private static FontBuilder arialFont = stl.fontArial().setFontSize(12);
	private static StyleBuilder boldStyle         = stl.style().bold();
	private static StyleBuilder boldCenteredStyle = stl.style(boldStyle).setFont(arialFont).setHorizontalAlignment(HorizontalAlignment.CENTER);
	private static StyleBuilder columnTitleStyle  = stl.style(boldCenteredStyle)
			.setBorder(stl.pen1Point())
			.setBackgroundColor(Color.LIGHT_GRAY);

	public static void main(String[] args){

//		String fileName = "C:\\Users\\weikun\\Desktop\\";
//		String reportFileName = "C:\\Users\\weikun\\Desktop\\result2.pdf";
//		long interval = 10000;
		
		if (args.length < 3) {
			System.out.println("Please input: \n "
					+ "1. where the report data is saved \n"
					+ "2. where the report will be saved \n"
					+ "3. what the time interval the generation will have.");
		}
		
		long interval = Long.valueOf(args[2]);

		while (true) {

			File file = new File(args[0]);

			String absolutePath = file.getAbsolutePath();
			//File dir = new File(absolutePath.substring(0, absolutePath.lastIndexOf(File.separator)));
			File dir = new File(absolutePath);
			File[] files = null;

			files = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.startsWith("reportData");
				}
			});

			if (files.length == 0) {
				System.out.println("Cannot find any report data.");
			}
			else {
				for (int i = 0; i < files.length; i++) {
					String fileName = files[i].toString();
					String systemInfo = files[i].getName().replaceAll("reportData-", "");
					systemInfo = systemInfo.replaceAll(".json", "");
					data = parseFile(fileName); 
					
					String reportFile = args[1]+"/"+systemInfo+".pdf";
					build(reportFile,systemInfo);
					System.out.println("Report file: "+reportFile+" is generated.");
				}
			}
			
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void build(String reportFileName, String systemInfo) {

		JasperReportBuilder mainReport = report();
		VerticalListBuilder builder = cmp.verticalList();
		builder.add(cmp.subreport(reportHeader()));
		builder.add(cmp.subreport(buildBasicTable(systemInfo)));
		builder.add(cmp.subreport(buildDemandTable()));
		builder.add(cmp.subreport(buildResponseTimeTable()));
		builder.add(cmp.subreport(buildThroughputTable()));
		builder.setGap(24);

		ArrayList<JasperReportBuilder> builders = buildReponsetimeFigure();
		for (int i = 0; i < builders.size(); i++) {
			builder.add(cmp.subreport(builders.get(i)));
		}

		builders = buildThroughputFigure();
		for (int i = 0; i < builders.size(); i++) {
			builder.add(cmp.subreport(builders.get(i)));
		}

		builder.add(cmp.subreport(buildTermTable()));
		
		mainReport.title(builder);
		try {
			mainReport.toPdf(Exporters.pdfExporter(reportFileName));
		} catch (DRException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<JasperReportBuilder> buildReponsetimeFigure() {
		ArrayList<JasperReportBuilder> builders = new ArrayList<JasperReportBuilder>();

		int nbClasses = data.getNbClasses();
		int nbBuilder = (int) Math.ceil(nbClasses/5d);

		int indexFull = nbBuilder - 2;
		if (indexFull < 0) {
			builders.add(generateBuilder(nbClasses, 0, data.getResponseTime(),"Response time (s)"));
		}
		else {
			for (int i = 0; i < indexFull; i++) {
				builders.add(generateBuilder(5, 0+5*i,data.getResponseTime(),"Response time (s)"));
			}

			int temp = nbClasses - 5*indexFull;
			int first = (int) Math.ceil(temp/2);
			int second = temp - first;
			builders.add(generateBuilder(first, 0+5*indexFull,data.getResponseTime(),"Response time (s)"));
			builders.add(generateBuilder(second, 0+5*indexFull+first,data.getResponseTime(),"Response time (s)"));

		}
		return builders;
	}

	private static ArrayList<JasperReportBuilder> buildThroughputFigure() {
		ArrayList<JasperReportBuilder> builders = new ArrayList<JasperReportBuilder>();

		int nbClasses = data.getNbClasses();
		int nbBuilder = (int) Math.ceil(nbClasses/5d);

		int indexFull = nbBuilder - 2;
		if (indexFull < 0) {
			builders.add(generateBuilder(nbClasses, 0, data.getThroughput(),"Throughput"));
		}
		else {
			for (int i = 0; i < indexFull; i++) {
				builders.add(generateBuilder(5, 0+5*i,data.getThroughput(),"Throughput"));
			}

			int temp = nbClasses - 5*indexFull;
			int first = (int) Math.ceil(temp/2);
			int second = temp - first;
			builders.add(generateBuilder(first, 0+5*indexFull,data.getThroughput(),"Throughput"));
			builders.add(generateBuilder(second, 0+5*indexFull+first,data.getThroughput(),"Throughput"));

		}
		return builders;
	}

	private static JasperReportBuilder generateBuilder(int nbClass, int startIndex, HashMap<String, ComplexData> compData, String title) {
		JasperReportBuilder report = report();

		DRDataSource dataSource = null;
		ArrayList<ArrayList<Double>> valueSet = null;
		Map<String, Color> seriesColors = null;
		TextColumnBuilder<String> timeColumn = null;
		TextColumnBuilder<Double> data1Column = null;
		TextColumnBuilder<Double> data2Column = null;
		TextColumnBuilder<Double> data3Column = null;
		TextColumnBuilder<Double> data4Column = null;
		TextColumnBuilder<Double> data5Column = null;

		switch (nbClass){
		case 1:
			dataSource = new DRDataSource("time","data1");
			valueSet = new ArrayList<ArrayList<Double>>();

			for (int i = startIndex; i < startIndex+nbClass; i++) {
				String className = data.getClassName().get(i);
				valueSet.add(compData.get(className).getValues());
			}

			for (int i = 0 ; i < valueSet.get(0).size(); i++) {
				dataSource.add(data.getTime().get(i),valueSet.get(0).get(i));
			}

			timeColumn = col.column("Time", "time", type.stringType());
			data1Column = col.column(data.getClassName().get(startIndex), "data1", type.doubleType());

		    seriesColors = new HashMap<String, Color>();
		    seriesColors.put(data.getClassName().get(startIndex), Color.BLUE);
			
			try {
				report
				.setTemplate(Templates.reportTemplate)
				.summary(
						cht.lineChart()
						.setTitle(title)
						.seriesColorsByName(seriesColors)
						.setTitleFont(arialFont)
						.setCategory(timeColumn)
						.series(
								cht.serie(data1Column)))
								//.pageFooter(Templates.footerComponent)
								.setDataSource(dataSource);
				//.show();
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		case 2:
			dataSource = new DRDataSource("time","data1","data2");
			valueSet = new ArrayList<ArrayList<Double>>();

			for (int i = startIndex; i < startIndex+nbClass; i++) {
				String className = data.getClassName().get(i);
				valueSet.add(compData.get(className).getValues());
			}

			for (int i = 0 ; i < valueSet.get(0).size(); i++) {
				dataSource.add(data.getTime().get(i),valueSet.get(0).get(i),valueSet.get(1).get(i));
			}
			
			timeColumn = col.column("Time", "time", type.stringType());
			data1Column = col.column(data.getClassName().get(startIndex), "data1", type.doubleType());
			data2Column = col.column(data.getClassName().get(startIndex+1), "data2", type.doubleType());

		    seriesColors = new HashMap<String, Color>();
		    seriesColors.put(data.getClassName().get(startIndex), Color.BLUE);
		    seriesColors.put(data.getClassName().get(startIndex+1), Color.RED);
			
			try {
				report
				.setTemplate(Templates.reportTemplate)
				.summary(
						cht.lineChart()
						.setTitle(title)
						.seriesColorsByName(seriesColors)
						.setTitleFont(arialFont)
						.setCategory(timeColumn)
						.series(
								cht.serie(data1Column), cht.serie(data2Column)))
								//.pageFooter(Templates.footerComponent)
								.setDataSource(dataSource);
				//.show();
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		case 3:
			dataSource = new DRDataSource("time","data1","data2","data3");
			valueSet = new ArrayList<ArrayList<Double>>();

			for (int i = startIndex; i < startIndex+nbClass; i++) {
				String className = data.getClassName().get(i);
				valueSet.add(compData.get(className).getValues());
			}

			for (int i = 0 ; i < valueSet.get(0).size(); i++) {
				dataSource.add(data.getTime().get(i),valueSet.get(0).get(i),valueSet.get(1).get(i),valueSet.get(2).get(i));
			}

			timeColumn = col.column("Time", "time", type.stringType());
			data1Column = col.column(data.getClassName().get(startIndex), "data1", type.doubleType());
			data2Column = col.column(data.getClassName().get(startIndex+1), "data2", type.doubleType());
			data3Column = col.column(data.getClassName().get(startIndex+2), "data3", type.doubleType());

		    seriesColors = new HashMap<String, Color>();
		    seriesColors.put(data.getClassName().get(startIndex), Color.BLUE);
		    seriesColors.put(data.getClassName().get(startIndex+1), Color.RED);
		    seriesColors.put(data.getClassName().get(startIndex+2), Color.GREEN);
			
			try {
				report
				.setTemplate(Templates.reportTemplate)
				.summary(
						cht.lineChart()
						.setTitle(title)
						.setTitleFont(arialFont)
						.seriesColorsByName(seriesColors)
						.setCategory(timeColumn)
						.series(
								cht.serie(data1Column), cht.serie(data2Column), cht.serie(data3Column)))
								//.pageFooter(Templates.footerComponent)
								.setDataSource(dataSource);
				//.show();
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		case 4:
			dataSource = new DRDataSource("time","data1","data2","data3","data4");
			valueSet = new ArrayList<ArrayList<Double>>();

			for (int i = startIndex; i < startIndex+nbClass; i++) {
				String className = data.getClassName().get(i);
				valueSet.add(compData.get(className).getValues());
			}

			for (int i = 0 ; i < valueSet.get(0).size(); i++) {
				dataSource.add(data.getTime().get(i),valueSet.get(0).get(i),valueSet.get(1).get(i),valueSet.get(2).get(i),valueSet.get(3).get(i));
			}

			timeColumn = col.column("Time", "time", type.stringType());
			data1Column = col.column(data.getClassName().get(startIndex), "data1", type.doubleType());
			data2Column = col.column(data.getClassName().get(startIndex+1), "data2", type.doubleType());
			data3Column = col.column(data.getClassName().get(startIndex+2), "data3", type.doubleType());
			data4Column = col.column(data.getClassName().get(startIndex+3), "data4", type.doubleType());

		    seriesColors = new HashMap<String, Color>();
		    seriesColors.put(data.getClassName().get(startIndex), Color.BLUE);
		    seriesColors.put(data.getClassName().get(startIndex+1), Color.RED);
		    seriesColors.put(data.getClassName().get(startIndex+2), Color.GREEN);
		    seriesColors.put(data.getClassName().get(startIndex+3), Color.GRAY);

			
			try {
				report
				.setTemplate(Templates.reportTemplate)
				.summary(
						cht.lineChart()
						.addCustomizer(new ChartCustomizer())
						.setTitle(title)
						.seriesColorsByName(seriesColors)
						.setTitleFont(arialFont)
						.setCategory(timeColumn)
						.series(
								cht.serie(data1Column), cht.serie(data2Column), cht.serie(data3Column), cht.serie(data4Column)))
								//.pageFooter(Templates.footerComponent)
								.setDataSource(dataSource);
				//.show();
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		case 5:
			dataSource = new DRDataSource("time","data1","data2","data3","data4","data5");
			valueSet = new ArrayList<ArrayList<Double>>();

			for (int i = startIndex; i < startIndex+nbClass; i++) {
				String className = data.getClassName().get(i);
				valueSet.add(compData.get(className).getValues());
			}

			for (int i = 0 ; i < valueSet.get(0).size(); i++) {
				dataSource.add(data.getTime().get(i),valueSet.get(0).get(i),valueSet.get(1).get(i),valueSet.get(2).get(i),valueSet.get(3).get(i),valueSet.get(4).get(i));
			}

			timeColumn = col.column("Time", "time", type.stringType());
			data1Column = col.column(data.getClassName().get(startIndex), "data1", type.doubleType());
			data2Column = col.column(data.getClassName().get(startIndex+1), "data2", type.doubleType());
			data3Column = col.column(data.getClassName().get(startIndex+2), "data3", type.doubleType());
			data4Column = col.column(data.getClassName().get(startIndex+3), "data4", type.doubleType());
			data5Column = col.column(data.getClassName().get(startIndex+4), "data5", type.doubleType());

		    seriesColors = new HashMap<String, Color>();
		    seriesColors.put(data.getClassName().get(startIndex), Color.BLUE);
		    seriesColors.put(data.getClassName().get(startIndex+1), Color.RED);
		    seriesColors.put(data.getClassName().get(startIndex+2), Color.GREEN);
		    seriesColors.put(data.getClassName().get(startIndex+3), Color.GRAY);
		    seriesColors.put(data.getClassName().get(startIndex+4), Color.BLACK);
			
			try {
				report
				.setTemplate(Templates.reportTemplate)
				.summary(
						cht.lineChart()
						.addCustomizer(new ChartCustomizer())
						.setTitle(title)
						.seriesColorsByName(seriesColors)
						.setTitleFont(arialFont)
						.setCategory(timeColumn)
						.series(
								cht.serie(data1Column), cht.serie(data2Column), cht.serie(data3Column), cht.serie(data4Column), cht.serie(data5Column)))
								//.pageFooter(Templates.footerComponent)
								.setDataSource(dataSource);
				//.show();
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		}
		return report;
	}

	private static JasperReportBuilder buildResponseTimeTable() {
		JasperReportBuilder report = report();

		DRDataSource dataSource = new DRDataSource("class", "average", "min", "max", "var");
		for (int i = 0; i < data.getNbClasses(); i++) {
			String className = data.getClassName().get(i);
			ComplexData comData = data.getResponseTime().get(className);
			dataSource.add(className,comData.getMean(),comData.getMin(),comData.getMax(),comData.getStd());
		}

		try {
			report//create new report design
			.setTemplate(Templates.reportTemplate)
			.columns(//add columns
					//            title,     field name     data type
					col.column("Class name",       "class",      type.stringType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Average",   "average",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Minimum",   "min",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Maximum",   "max",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Variance",   "var",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER))
					.title(cmp.text("Response time of jobs (s)").setHorizontalAlignment(HorizontalAlignment.CENTER).setStyle(boldCenteredStyle))//shows report title
					//.pageFooter(cmp.pageXofY())//shows number of page at page footer
					.setDataSource(dataSource);//set datasource
			//.show();//create and show report
		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}

	private static JasperReportBuilder buildThroughputTable() {
		JasperReportBuilder report = report();

		DRDataSource dataSource = new DRDataSource("class", "average", "min", "max", "var");
		for (int i = 0; i < data.getNbClasses(); i++) {
			String className = data.getClassName().get(i);
			ComplexData comData = data.getThroughput().get(className);
			dataSource.add(className,comData.getMean(),comData.getMin(),comData.getMax(),comData.getStd());
		}

		try {
			report//create new report design
			.setTemplate(Templates.reportTemplate)
			.columns(//add columns
					//            title,     field name     data type
					col.column("Class name",       "class",      type.stringType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Average",   "average",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Minimum",   "min",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Maximum",   "max",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Variance",   "var",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER))
					.title(cmp.text("Throughput of jobs").setHorizontalAlignment(HorizontalAlignment.CENTER).setStyle(boldCenteredStyle))//shows report title

					//.pageFooter(cmp.pageXofY())//shows number of page at page footer
					.setDataSource(dataSource);//set datasource
			//.show();//create and show report
		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}


	private static JasperReportBuilder buildDemandTable() {
		JasperReportBuilder report = report();

		DRDataSource dataSource = new DRDataSource("class", "nbJobs","demand", "think");
		for (int i = 0; i < data.getNbClasses(); i++) {
			String className = data.getClassName().get(i);
			dataSource.add(className,data.getNbUsersPerClass().get(className),data.getDemand().get(className),
					data.getThinkTime().get(className));
		}

		try {
			report//create new report design
			.setTemplate(Templates.reportTemplate)
			.columns(//add columns
					//            title,     field name     data type
					col.column("Class name",       "class",      type.stringType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Number of jobs",   "nbJobs",  type.integerType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Resource demand (s)",   "demand",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Think time (s)",   "think",  type.doubleType()).setHorizontalAlignment(HorizontalAlignment.CENTER))
					.title(cmp.text("Detailed information about jobs").setHorizontalAlignment(HorizontalAlignment.CENTER).setStyle(boldCenteredStyle))//shows report title
					//.pageFooter(cmp.pageXofY())//shows number of page at page footer
					.setDataSource(dataSource);//set datasource
			//.show();//create and show report
		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}

	private static JasperReportBuilder buildBasicTable(String systemInfo) {
		JasperReportBuilder report = report();

		DRDataSource dataSource = new DRDataSource("system","nbUser", "nbClasses");
		dataSource.add(systemInfo,data.getNbUsers(), data.getNbClasses());

		try {
			report//create new report design
			.setTemplate(Templates.reportTemplate)
			.columns(//add columns
					//            title,     field name     data type
					col.column("System information",       "system",      type.stringType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Number of jobs in total",       "nbUser",      type.integerType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Number of job classes",   "nbClasses",  type.integerType()).setHorizontalAlignment(HorizontalAlignment.CENTER))
					.title(cmp.text("Basic information").setHorizontalAlignment(HorizontalAlignment.CENTER).setStyle(boldCenteredStyle))//shows report title
					//.pageFooter(cmp.pageXofY())//shows number of page at page footer
					.setDataSource(dataSource);//set datasource
			//.show();//create and show report
		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}
	
	private static JasperReportBuilder reportHeader() {
		JasperReportBuilder report = report();

		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:00");
		
		String startObs = data.getTime().get(0);
		String endObs = data.getTime().get(data.getTime().size()-1);
		
		try {
			report//create new report design
			.setTemplate(Templates.reportTemplate)
					.title(Templates.createTitleComponent("FG Report - "+sdf.format(date)+" Period: "+startObs+"-"+endObs));
			//.show();//create and show report
		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}

	private static JasperReportBuilder buildTermTable() {
		JasperReportBuilder report = report();

		DRDataSource dataSource = new DRDataSource("term","desc");
		dataSource.add("Number of jobs", "Maximum number of jobs circulating in the system");
		dataSource.add("Resource demand", "The cumulative execution time a job seizes from a server, excluding contention overheads due to other concurrently executing requests.");
		dataSource.add("Think time", "The time interval between two successive requests");
		dataSource.add("Response time", "The total amount of time it takes to respond to a request for service");
		dataSource.add("Throughput", "The number of completed jobs per second");

		try {
			report//create new report design
			.setTemplate(Templates.reportTemplate)
			.columns(//add columns
					//            title,     field name     data type
					col.column("Term",       "term",      type.stringType()).setHorizontalAlignment(HorizontalAlignment.CENTER),
					col.column("Description",       "desc",      type.stringType()).setHorizontalAlignment(HorizontalAlignment.CENTER))
					.title(cmp.text("Term description").setHorizontalAlignment(HorizontalAlignment.CENTER).setStyle(boldCenteredStyle))//shows report title
					//.pageFooter(cmp.pageXofY())//shows number of page at page footer
					.setDataSource(dataSource);//set datasource
			//.show();//create and show report
		} catch (Exception e) {
			e.printStackTrace();
		}

		return report;
	}
	
	private static ReportData parseFile(String fileName) {
		ReportData data = new ReportData();

		JSONParser jsonParser = new JSONParser();
		JSONObject jsonObject;
		try {
			jsonObject = (JSONObject) jsonParser.parse(new FileReader(fileName));

			data.setNbUsers(Integer.valueOf(String.valueOf(jsonObject.get("nbUsers"))));
			data.setNbClasses(Integer.valueOf(String.valueOf(jsonObject.get("nbClasses"))));

			JSONArray jsonArray = (JSONArray) jsonObject.get("classes");
			ArrayList<String> classes = new ArrayList<String>();

			for (int i = 0; i < jsonArray.size(); i++) {
				classes.add(String.valueOf(jsonArray.get(i)));
			}
			data.setClassName(classes);

			jsonArray = (JSONArray) jsonObject.get("time");
			ArrayList<String> time = new ArrayList<String>();

			for (int i = 0; i < jsonArray.size(); i++) {
				time.add(String.valueOf(jsonArray.get(i)));
			}
			data.setTime(time);

			jsonArray = (JSONArray) jsonObject.get("demand");
			HashMap<String, Double> demand = new HashMap<String, Double>();

			for (int i = 0; i < jsonArray.size(); i++) {
				demand.put(classes.get(i),Double.valueOf(String.valueOf(jsonArray.get(i))));
			}
			data.setDemand(demand);

			jsonArray = (JSONArray) jsonObject.get("think_time");
			HashMap<String, Double> thinkTime = new HashMap<String, Double>();

			for (int i = 0; i < jsonArray.size(); i++) {
				thinkTime.put(classes.get(i),Double.valueOf(String.valueOf(jsonArray.get(i))));
			}
			data.setThinkTime(thinkTime);

			jsonArray = (JSONArray) jsonObject.get("nbUsersPerClass");
			HashMap<String, Integer> nbUsersPerClass = new HashMap<String, Integer>();

			for (int i = 0; i < jsonArray.size(); i++) {
				nbUsersPerClass.put(classes.get(i),Integer.valueOf(String.valueOf(jsonArray.get(i))));
			}
			data.setNbUsersPerClass(nbUsersPerClass);

			jsonArray = (JSONArray) jsonObject.get("response_time");
			HashMap<String, ComplexData> response_time = generateComplexData(jsonArray);
			data.setResponseTime(response_time);

			jsonArray = (JSONArray) jsonObject.get("throughput");
			HashMap<String, ComplexData> throughput = generateComplexData(jsonArray);
			data.setThroughput(throughput);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		return data;
	}

	private static HashMap<String, ComplexData> generateComplexData(JSONArray jsonArray) {
		HashMap<String, ComplexData> complexData = new HashMap<String, ComplexData>();

		for (int i = 0; i < jsonArray.size(); i++) {
			ComplexData comData = new ComplexData();

			JSONObject jsonSubObject = (JSONObject) jsonArray.get(i);
			String className = (String) jsonSubObject.get("classes");
			String value = String.valueOf(jsonSubObject.get("value"));
			String[] values_str = value.split(",");
			ArrayList<Double> values = new ArrayList<Double>();
			for (int j = 0; j < values_str.length; j++) {
				if (values_str[j].contains("[")){
					values_str[j] = values_str[j].substring(1);
				}
				if (values_str[j].contains("]")){
					int index = values_str[j].indexOf("]");
					values_str[j] = values_str[j].substring(0,index);
				}
				values.add(Double.valueOf(values_str[j]));
			}

			comData.setValues(values);
			comData.setClassName(className);
			comData.setMax(Double.valueOf(String.valueOf(jsonSubObject.get("max"))));
			comData.setMin(Double.valueOf(String.valueOf(jsonSubObject.get("min"))));
			comData.setMean(Double.valueOf(String.valueOf(jsonSubObject.get("mean"))));
			comData.setStd(Double.valueOf(String.valueOf(jsonSubObject.get("std"))));

			complexData.put(className, comData);
		}

		return complexData;
	}

	private static class ChartCustomizer implements DRIChartCustomizer, Serializable {  
		public void customize(JFreeChart chart, ReportParameters reportParameters) {  
			CategoryAxis domainAxis = chart.getCategoryPlot().getDomainAxis();  
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI/6.0));
		}  
	}
}
