package com.datastax.creditcard.gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.WebServlet;

import org.joda.time.DateTime;

import com.datastax.creditcard.model.Transaction;
import com.datastax.creditcard.services.CreditCardService;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Title("Blacklist Transactions")
@Widgetset("com.datastax.creditcard.gui.TransactionWidgetSet")
@Theme("reindeer")
@Push
public class BlacklistUI extends UI {

	DateTime transactionDate = DateTime.now();

	TextField filter = new TextField();
	Grid transactionList = new Grid();

	TransactionForm transactionForm = new TransactionForm();
	UserForm userForm = new UserForm();
	IssuerForm issuerForm = new IssuerForm();
	HorizontalLayout mainLayout;

	IndexedContainer container;
	CreditCardService service = new CreditCardService("52.27.155.65");

	List<Transaction> transactions = new ArrayList<Transaction>();

	/*
	 * The "Main method".
	 * 
	 * This is the entry point method executed to initialize and configure the
	 * visible user interface. Executed on every browser reload because a new
	 * instance is created for each web page loaded.
	 */
	@Override
	protected void init(VaadinRequest request) {
		configureComponents();
		buildLayout();
	}

	private void configureComponents() {
		/*
		 * Synchronous event handling.
		 * 
		 * Receive user interaction events on the server-side. This allows you
		 * to synchronously handle those events. Vaadin automatically sends only
		 * the needed changes to the web page without loading a new page.
		 */
		configureContainer();

		transactionList.setContainerDataSource(container);
		transactionList.setColumnOrder("creditCardNo", "transactionTime", "transactionId", "issuer", "location",
				"amount", "status");
		transactionList.removeColumn("items");
		transactionList.removeColumn("notes");
		transactionList.setSelectionMode(Grid.SelectionMode.SINGLE);
		transactionList.addSelectionListener(new SelectionListener() {

			@Override
			public void select(SelectionEvent event) {
				Item item = transactionList.getContainerDataSource().getItem(transactionList.getSelectedRow());

				if (item != null) {
					Transaction transaction = service.getTransaction(item.getItemProperty("transactionId").getValue()
							.toString());
					transactionForm.edit(transaction);
					userForm.view(service.getUserById(transaction.getUserId()));
					userForm.setVisible(false);

					issuerForm.view(service.getIssuerById(transaction.getIssuer()));
					issuerForm.setVisible(false);
				}
			}
		});

		new ReferesherThread().start();
	}

	class ReferesherThread extends Thread {
		@Override
		public void run() {

			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}

				if (!somethingUIhappending()) {

					access(new Runnable() {
						@Override
						public void run() {
							refreshTransactions();
						}
					});
				}
			}
		}
	}

	private void configureContainer() {
		container = new IndexedContainer();
		container.addContainerProperty("creditCardNo", String.class, "none");
		container.addContainerProperty("transactionTime", Date.class, new Date());
		container.addContainerProperty("transactionId", String.class, "none");
		container.addContainerProperty("issuer", String.class, "none");
		container.addContainerProperty("location", String.class, "none");
		container.addContainerProperty("amount", Double.class, 0.0);
		container.addContainerProperty("status", String.class, "none");
		container.addContainerProperty("items", Map.class, new HashMap());
		container.addContainerProperty("notes", String.class, "");
	}

	/*
	 * Robust layouts.
	 * 
	 * Layouts are components that contain other components. HorizontalLayout
	 * contains TextField and Button. It is wrapped with a Grid into
	 * VerticalLayout for the left side of the screen. Allow user to resize the
	 * components with a SplitPanel.
	 * 
	 * In addition to programmatically building layout in Java, you may also
	 * choose to setup layout declaratively with Vaadin Designer, CSS and HTML.
	 */
	@SuppressWarnings("deprecation")
	private void buildLayout() {

		Label label = new Label("Transactions     ");
		label.setStyleName("h1");
		label.setWidth("100%");

		Label labelDate = new Label("   Date : ");
		labelDate.setStyleName("h3");

		DateField date = new DateField();
		date.setValue(transactionDate.toDate());
		date.setDateFormat("yyyy-MM-dd");
		date.addListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				transactionDate = new DateTime(date.getValue());
				refreshTransactions();
			}
		});

		HorizontalLayout dateLayout = new HorizontalLayout(labelDate, date);
		dateLayout.setMargin(true);
		HorizontalLayout top = new HorizontalLayout(label, dateLayout);
		top.setMargin(true);

		VerticalLayout left = new VerticalLayout(top, transactionList);
		Responsive.makeResponsive(left);
		left.setSizeFull();
		transactionList.setSizeFull();

		// Create a header row to hold column filters
		HeaderRow filterRow = transactionList.appendHeaderRow();

		// Set up a filter for all columns
		for (Object pid : transactionList.getContainerDataSource().getContainerPropertyIds()) {
			HeaderCell cell = filterRow.getCell(pid);

			if (cell == null) {
				continue;
			}
			// Have an input field to use for filter
			TextField filterField = new TextField();
			filterField.setColumns(0);
			filterField.setWidth("50px");

			// Update filter When the filter input is changed
			filterField.addTextChangeListener(change -> {
				// Can't modify filters so need to replace
					container.removeContainerFilters(pid);

					// (Re)create the filter if necessary
					if (!change.getText().isEmpty())
						container.addContainerFilter(new SimpleStringFilter(pid, change.getText(), true, false));
				});
			cell.setComponent(filterField);
		}

		// transactionList.set
		left.setExpandRatio(transactionList, 1);

		HorizontalLayout userLayout = new HorizontalLayout(left, transactionForm);
		userLayout.setSizeFull();
		userLayout.setExpandRatio(left, 1);

		HorizontalLayout issuerLayout = new HorizontalLayout(userLayout, userForm);
		issuerLayout.setSizeFull();
		issuerLayout.setExpandRatio(userLayout, 1);

		HorizontalLayout mainLayout = new HorizontalLayout(issuerLayout, issuerForm);
		mainLayout.setSizeFull();
		mainLayout.setExpandRatio(issuerLayout, 1);

		// Split and allow resizing
		setContent(mainLayout);
	}

	/*
	 * Choose the design patterns you like.
	 * 
	 * It is good practice to have separate data access methods that handle the
	 * back-end access and/or the user interface updates. You can further split
	 * your code into classes to easier maintenance. With Vaadin you can follow
	 * MVC, MVP or any other design pattern you choose.
	 */
	void refreshTransactions() {

		if (somethingUIhappending()) {
			// Don't update as we are doing something
			return;
		}

		refreshTransactions(filter.getValue(), false);
	}

	void refreshTransactionsOverride() {
		refreshTransactions("", true);
	}

	@SuppressWarnings("unchecked")
	private void refreshTransactions(String stringFilter, boolean refresh) {

		List<Transaction> newTransactions = service.getBlacklistTransactions(this.transactionDate.toDate());

		if  (refresh){
			transactions = newTransactions;			
		}else if (newTransactions.size() == this.transactions.size()) {
			//Check the number of each status in the new Transactions
			if (checkBatchSizesChanged(newTransactions, transactions)){
				transactions = newTransactions;
			}else{
				return;
			}
		}else if (newTransactions.size() != this.transactions.size()) {
			transactions = newTransactions;			
		}else {
			return;
		}

		System.out.println("Refreshing");
		configureContainer();

		for (Transaction t : transactions) {
			if (t == null)
				continue;

			Item newItem = container.getItem(container.addItem());
			newItem.getItemProperty("creditCardNo").setValue(t.getCreditCardNo());
			newItem.getItemProperty("transactionTime").setValue(t.getTransactionTime());
			newItem.getItemProperty("transactionId").setValue(t.getTransactionId());
			newItem.getItemProperty("issuer").setValue(t.getIssuer());
			newItem.getItemProperty("location").setValue(t.getLocation());
			newItem.getItemProperty("amount").setValue(t.getAmount());
			newItem.getItemProperty("status").setValue(t.getStatus());
			newItem.getItemProperty("items").setValue(t.getItems());
			newItem.getItemProperty("notes").setValue(t.getNotes());
		}

		transactionList.setContainerDataSource(container);
		transactionForm.setVisible(false);
	}

	private boolean checkBatchSizesChanged(List<Transaction> newTransactions, List<Transaction> oldTransactions) {
		Map<String, Integer> sizeOfNewStatues = new HashMap<String, Integer>();
		Map<String, Integer> sizeOfOldStatues = new HashMap<String, Integer>();

		
		for (Transaction transaction : newTransactions){
			
			if(sizeOfNewStatues.containsKey(transaction.getStatus())){
				int count = sizeOfNewStatues.get(transaction.getStatus());
				sizeOfNewStatues.put(transaction.getStatus(), count+1);
			}else{			
				sizeOfNewStatues.put(transaction.getStatus(), 1);
			}
		}

		for (Transaction transaction : oldTransactions){
			
			if(sizeOfOldStatues.containsKey(transaction.getStatus())){
				int count = sizeOfOldStatues.get(transaction.getStatus());
				sizeOfOldStatues.put(transaction.getStatus(), count+1);
			}else{			
				sizeOfOldStatues.put(transaction.getStatus(), 1);
			}
		}
				
		Set<String> keySet = sizeOfNewStatues.keySet();
		Iterator<String> iterator = keySet.iterator();
		
		while(iterator.hasNext()){
			
			String status = iterator.next();
			Integer countNew =  sizeOfNewStatues.get(status);
			Integer countOld =  sizeOfOldStatues.get(status);
			
			//Should be in New as we are going over the keys in new.
			if (countOld == null || countOld != countNew){
				return true;
			}			
		}

		return false;
	}

	private boolean somethingUIhappending() {

		if (userForm.isVisible() || issuerForm.isVisible() || transactionForm.isVisible()) {
			return true;
		}
		return false;
	}

	/*
	 * Deployed as a Servlet or Portlet.
	 * 
	 * You can specify additional servlet parameters like the URI and UI class
	 * name and turn on production mode when you have finished developing the
	 * application.
	 */
	@WebServlet(urlPatterns = { "/blacklist/*" })
	@VaadinServletConfiguration(ui = BlacklistUI.class, productionMode = false)
	public static class BlacklistServlet extends VaadinServlet {
	}
}
