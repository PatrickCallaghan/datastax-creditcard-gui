package com.datastax.creditcard.gui;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.joda.time.DateTime;

import com.datastax.creditcard.model.Transaction;
import com.datastax.creditcard.model.Transaction.Status;
import com.datastax.creditcard.services.CreditCardService;
import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.ChartType;
import com.vaadin.addon.charts.model.Configuration;
import com.vaadin.addon.charts.model.Cursor;
import com.vaadin.addon.charts.model.DataSeries;
import com.vaadin.addon.charts.model.DataSeriesItem;
import com.vaadin.addon.charts.model.Labels;
import com.vaadin.addon.charts.model.ListSeries;
import com.vaadin.addon.charts.model.PlotOptionsColumn;
import com.vaadin.addon.charts.model.PlotOptionsPie;
import com.vaadin.addon.charts.model.Tooltip;
import com.vaadin.addon.charts.model.XAxis;
import com.vaadin.addon.charts.model.YAxis;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Title("FraudUI")
@Widgetset("com.datastax.creditcard.gui.TransactionWidgetSet")
@Theme("reindeer")
@Push
public class DashboardUI extends UI {

	private DateFormat dayMonthFormatter = new SimpleDateFormat("dd-MM");
	private DateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
	Chart statusChart = new Chart(ChartType.PIE);
	Chart countByDayChart = new Chart(ChartType.COLUMN);

	VerticalLayout mainLayout;
	HorizontalLayout row1Layout;
	HorizontalLayout row2Layout;
	HorizontalLayout row3Layout;

	CreditCardService service = new CreditCardService();
	DateTime transactionDate = DateTime.now();
	List<Transaction> transactions = new ArrayList<Transaction>();
	Map<String, Long> totalNoOfTransactions;

	@Override
	protected void init(VaadinRequest request) {
		configureComponents();
		buildLayout();
	}

	private void buildLayout() {

		Label banner = new Label("Fraud Prevention Center");
		banner.setStyleName("h1");
		banner.setSizeFull();
		
		Panel statusCountPanel = new Panel();
		statusCountPanel.setContent(statusChart);
		
		Panel countByDayChartPanel = new Panel();
		countByDayChartPanel.setContent(countByDayChart);
		
		row1Layout = new HorizontalLayout(banner);
		row1Layout.setSpacing(true);
		row1Layout.setSizeFull();
		row1Layout.setComponentAlignment(banner, Alignment.MIDDLE_CENTER);
		row1Layout.setSpacing(true);
		row1Layout.setMargin(true);
		
		row2Layout = new HorizontalLayout(statusCountPanel, countByDayChartPanel);
		row2Layout.setMargin(true);
		row2Layout.setSpacing(true);
		
		row3Layout = new HorizontalLayout();
		row2Layout.setMargin(true);
		row3Layout.setSpacing(true);

		mainLayout = new VerticalLayout(row1Layout, row2Layout, row3Layout);
		Responsive.makeResponsive(mainLayout);
		setContent(mainLayout);
	}

	private void configureComponents() {

		refresh();
	}

	private void refresh() {
		this.transactions = service.getBlacklistTransactions(this.transactionDate.toDate());
		this.totalNoOfTransactions = service.getTotalNoOfTransactions(10);

		System.out.println(totalNoOfTransactions);

		this.populateStatusCountChart();
		this.populateCountDayChart();
	}

	public void createStatusCountChart() {

		statusChart.setWidth("350px");
		statusChart.setHeight("200px");
	}

	private void populateStatusCountChart() {
		Configuration conf = statusChart.getConfiguration();

		conf.setTitle("Blacklist Status - Total(" + transactions.size() + ")");

		PlotOptionsPie plotOptions = new PlotOptionsPie();
		plotOptions.setCursor(Cursor.POINTER);

		Labels dataLabels = new Labels();
		dataLabels.setEnabled(true);
		dataLabels.setFormatter("this.point.name");

		plotOptions.setDataLabels(dataLabels);
		conf.setPlotOptions(plotOptions);

		final DataSeries series = new DataSeries();

		for (Status status : Transaction.Status.values()) {

			int count = 0;

			for (Transaction transaction : transactions) {
				if (transaction.getStatus().equals(status.toString())) {
					count++;
				}
			}
			System.out.println("status.toString(), count:" + status.toString() + "-" + count);
			series.add(new DataSeriesItem(status.toString(), count));
		}

		conf.setSeries(series);
		statusChart.drawChart(conf);
	}

	public void createCountDayChart() {

		countByDayChart.setWidth("350px");
		countByDayChart.setHeight("200px");
	}

	private void populateCountDayChart() {
		Configuration conf = countByDayChart.getConfiguration();

		conf.setTitle("Count By Day");

		conf.setTitle("Transactions Total per day");
		conf.setSubTitle("");

		XAxis x = new XAxis();
		x.setCategories(getDaysCategories());
		conf.addxAxis(x);

		YAxis y = new YAxis();
		y.setMin(0);
		y.setTitle("Date");
		conf.addyAxis(y);

		Tooltip tooltip = new Tooltip();
		tooltip.setFormatter("this.x +' : '+ this.y");
		conf.setTooltip(tooltip);

		PlotOptionsColumn plot = new PlotOptionsColumn();
		plot.setPointPadding(0.2);
		plot.setBorderWidth(0);

		conf.addSeries(createListSeries());
		countByDayChart.drawChart(conf);
	}

	private ListSeries createListSeries() {

		ListSeries listSeries = new ListSeries("");
		DateTime start = DateTime.now().minusDays(9);

		for (int i = 0; i < 10; i++) {

			String date = dateFormatter.format(start.toDate());
			int value = 0;
			if (this.totalNoOfTransactions.containsKey(date)) {
				
				value = this.totalNoOfTransactions.get(date).intValue();
			}

			listSeries.addData(value);
			start = start.plusDays(1);
		}

		return listSeries;
	}

	private String[] getDaysCategories() {

		List<String> dates = new ArrayList<String>();
		DateTime start = DateTime.now().minusDays(9);

		for (int i = 0; i < 10; i++) {

			dates.add(dayMonthFormatter.format(start.toDate()));
			start = start.plusDays(1);
		}

		return dates.toArray(new String[] {});
	}

	@WebServlet(urlPatterns = {"/dashboard/*","/VAADIN/*"})
	@VaadinServletConfiguration(ui = DashboardUI.class, productionMode = false)
	public static class DashboardServlet extends VaadinServlet {
	}
}
