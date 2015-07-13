package com.datastax.creditcard.gui;

import java.util.Date;

import javax.servlet.annotation.WebServlet;

import org.joda.time.DateTime;

import com.datastax.creditcard.services.CreditCardService;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Title("Users ")
@Widgetset("com.datastax.creditcard.gui.TransactionWidgetSet")
@Theme("reindeer")
@Push
public class MerchantUI extends UI {
	
	DateTime transactionDate = DateTime.now();

	TextField filterIssuerID = new TextField("Filter IssuerId : ");

	Grid userGrid = new Grid();
	
	IndexedContainer container;
	CreditCardService service = new CreditCardService();
	
	@Override
	protected void init(VaadinRequest request) {

		configureComponents();
		buildLayout();

	}

	private void buildLayout() {
		
		HorizontalLayout top = new HorizontalLayout();
		top.setSpacing(true);
		top.addComponent(filterIssuerID);
		
		VerticalLayout main = new VerticalLayout(top, userGrid);
		setContent(main);
	}

	private void configureComponents() {
		configureContainer();
		
		userGrid.setContainerDataSource(container);
		userGrid.setColumnOrder("id", "name", "location");
		userGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
		userGrid.addSelectionListener(new SelectionListener() {

			@Override
			public void select(SelectionEvent event) {
				Item item = userGrid.getContainerDataSource().getItem(userGrid.getSelectedRow());

				if (item != null) {
					//Select * from rules where credit card no - 
					//ShowRules - with Add button
					
				}
			}
		});

		refreshIssuers();		
	}	
    
	private void refreshIssuers() {
		if (this.filterIssuerID.getValue().length() < 3){
			return;
		}
		
		//this.service.getUsersByFilter(this.filterCCNo.getValue());
	}

	private void configureContainer() {
		container = new IndexedContainer();
		container.addContainerProperty("id", String.class, "none");
		container.addContainerProperty("name", String.class, "none");
		container.addContainerProperty("location", String.class,"none");
	}

	@WebServlet(urlPatterns = { "/issuers/*" })
	@VaadinServletConfiguration(ui = MerchantUI.class, productionMode = false)
	public static class IssuersServlet extends VaadinServlet {
	}
}
