package com.datastax.creditcard.gui;

import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.joda.time.DateTime;

import com.datastax.creditcard.model.User;
import com.datastax.creditcard.model.UserRule;
import com.datastax.creditcard.services.CreditCardService;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Title("Users ")
@Widgetset("com.datastax.creditcard.gui.TransactionWidgetSet")
@Theme("reindeer")
@Push
public class UsersUI extends UI {
	
	DateTime transactionDate = DateTime.now();

	TextField filterCCNo = new TextField();	
	
	TextField filterUserID = new TextField();
	ComboBox fieldSearch = new ComboBox("");

	Button editUserRuleButton = new Button("Edit");
	Button deleteUserRuleButton = new Button("Delete");
	Button addUserRuleButton = new Button("Add");
	
	Grid userGrid = new Grid();		
	Grid userRulesGrid = new Grid();	
	
	IndexedContainer container;
	IndexedContainer rulesContainer;
	CreditCardService service = new CreditCardService();

	private String userId;
	private List<User> users;
	private List<UserRule> userRules;
	
	@Override
	protected void init(VaadinRequest request) {
		configureComponents();
		buildLayout();
	}

	private void buildLayout() {
		
		VerticalLayout top = new VerticalLayout();
		Label l1 = new Label("Users");
		
		l1.setStyleName("h2");	
		top.addComponent(l1);		
		top.addComponent(fieldSearch);
		top.addComponent(filterCCNo);
		
		Label l2 = new Label("Rules");
		l2.setStyleName("h2");
		
		userGrid.setWidth("100%");
		userGrid.setHeight("250px");
				
		userRulesGrid.setWidth("100%");
		userRulesGrid.setHeight("250px");
				
		HorizontalLayout rulesButtons = new HorizontalLayout();
		rulesButtons.addComponent(addUserRuleButton);
		rulesButtons.addComponent(editUserRuleButton);
		rulesButtons.addComponent(deleteUserRuleButton);

		Panel userPanel = new Panel(userGrid);
		Panel userRulesPanel = new Panel(userRulesGrid);
				
		VerticalLayout main = new VerticalLayout(top, userPanel, l2, userRulesGrid, rulesButtons);
		main.setMargin(new MarginInfo(false, true, false, true));
		
		setContent(main);
	}

	private void configureComponents() {
		configureContainer();
		configureRulesContainer();

		fieldSearch.setNullSelectionAllowed(false);
		fieldSearch.addItem("cc_no");
		fieldSearch.addItem("user_id");
		fieldSearch.addItem("first");
		fieldSearch.addItem("last");
		fieldSearch.addItem("city");
		fieldSearch.addItem("state");
		fieldSearch.setValue("first");
		
		userGrid.setContainerDataSource(container);
		userGrid.setColumnOrder("creditCardNo", "userId", "firstname", "lastname", "cityName",
				"stateName", "gender");
		userGrid.setSelectionMode(Grid.SelectionMode.SINGLE);		
		userGrid.addSelectionListener(new SelectionListener() {

			@Override
			public void select(SelectionEvent event) {
				Item item = userGrid.getContainerDataSource().getItem(userGrid.getSelectedRow());

				if (item != null) {		
					userId = item.getItemProperty("userId").getValue().toString();
					populateUserRules();
				}
			}
		});
		userRulesGrid.setContainerDataSource(rulesContainer);
		userRulesGrid.setColumnOrder("userId", "ruleName", "merchant", "amount",
				"noOfTransactions", "noOfDays");
		userRulesGrid.setSelectionMode(Grid.SelectionMode.SINGLE);		
		userRulesGrid.addSelectionListener(new SelectionListener() {

			@Override
			public void select(SelectionEvent event) {
				
			}
		});
		filterCCNo.addTextChangeListener(new TextChangeListener() {
			
			@Override
			public void textChange(TextChangeEvent event) {
				String filterValue = event.getText();				
				
				users = service.searchUser(fieldSearch.getValue().toString(), filterValue);
				populateUsers();
			}
		});
		
		addUserRuleButton.addClickListener(new ClickListener(){

			@Override
			public void buttonClick(ClickEvent event) {
				Item item = userGrid.getContainerDataSource().getItem(userGrid.getSelectedRow());

				if (item != null) {
					
					userId = item.getItemProperty("userId").getValue().toString();
	    	        UserRuleWindow sub = new UserRuleWindow(userId);	    	        
	    	        // Add it to the root component
	    	        getUI().addWindow(sub);		
	    	        populateUserRules();
				}
			}			
		});
		editUserRuleButton.addClickListener(new ClickListener(){

			@Override
			public void buttonClick(ClickEvent event) {
				Item item = userRulesGrid.getContainerDataSource().getItem(userRulesGrid.getSelectedRow());

				if (item != null) {
					
					UserRule userRule = service.getUserRule(item.getItemProperty("userId").getValue()
							.toString(), item.getItemProperty("ruleId").getValue()
							.toString());
					
	    	        UserRuleWindow sub = new UserRuleWindow(userRule);
	    	        
	    	        getUI().addWindow(sub);		
	    	        populateUserRules();
				}				
			}			
		});
		deleteUserRuleButton.addClickListener(new ClickListener(){

			@Override
			public void buttonClick(ClickEvent event) {
				Item item = userRulesGrid.getContainerDataSource().getItem(userRulesGrid.getSelectedRow());

				if (item != null) {		
					userId = item.getItemProperty("userId").getValue().toString();
	    	        service.deleteUserRule(userId, item.getItemProperty("ruleId").getValue().toString());	    	        
	    	        populateUserRules();
				}
			}			
		});
				
		populateUsers();
	}	
    
	@SuppressWarnings("unchecked")
	private void populateUsers() {
		
		if (users==null){
			return;
		}
		
		configureContainer();
		
		for (User u : users) {
			if (u == null)
				continue;

			Item newItem = container.getItem(container.addItem());
			newItem.getItemProperty("creditCardNo").setValue(u.getCreditCardNo());
			newItem.getItemProperty("userId").setValue(u.getUserId());
			newItem.getItemProperty("firstname").setValue(u.getFirstname());
			newItem.getItemProperty("lastname").setValue(u.getLastname());
			newItem.getItemProperty("cityName").setValue(u.getCityName());
			newItem.getItemProperty("stateName").setValue(u.getStateName());
			newItem.getItemProperty("gender").setValue(u.getGender());
		}

		userGrid.setContainerDataSource(container);
	}

	private void configureContainer() {
		container = new IndexedContainer();
		container.addContainerProperty("creditCardNo", String.class, "none");
		container.addContainerProperty("userId", String.class, "none");
		container.addContainerProperty("firstname", String.class, "none");
		container.addContainerProperty("lastname", String.class, "none");
		container.addContainerProperty("cityName", String.class, "none");
		container.addContainerProperty("stateName", String.class, "none");		
		container.addContainerProperty("gender", String.class, "none");
	}

	@SuppressWarnings("unchecked")
	public void populateUserRules() {
		
		System.out.println("Populating user rules");
				
		this.userRules = service.getUserRules(userId);

		configureRulesContainer();
		
		for (UserRule u : userRules) {
			if (u == null)
				continue;

			Item newItem = rulesContainer.getItem(rulesContainer.addItem());
			newItem.getItemProperty("userId").setValue(u.getUserId());
			newItem.getItemProperty("ruleId").setValue(u.getRuleId());
			newItem.getItemProperty("ruleName").setValue(u.getRuleName());
			newItem.getItemProperty("merchant").setValue(u.getMerchant());
			newItem.getItemProperty("amount").setValue(u.getAmount());
			newItem.getItemProperty("noOfTransactions").setValue(u.getNoOfTransactions());
			newItem.getItemProperty("noOfDays").setValue(u.getNoOfDays());			
		}

		userRulesGrid.setContainerDataSource(rulesContainer);
	}
	
	private void configureRulesContainer() {
		rulesContainer = new IndexedContainer();
		rulesContainer.addContainerProperty("userId", String.class, "none");
		rulesContainer.addContainerProperty("ruleId", String.class, "none");
		rulesContainer.addContainerProperty("ruleName", String.class, "none");
		rulesContainer.addContainerProperty("merchant", String.class, "none");
		rulesContainer.addContainerProperty("amount", Double.class, "none");		
		rulesContainer.addContainerProperty("noOfTransactions", Integer.class, "none");
		rulesContainer.addContainerProperty("noOfDays", Integer.class, "none");
	}
	
	@WebServlet(urlPatterns = { "/users/*" })
	@VaadinServletConfiguration(ui = UsersUI.class, productionMode = false)
	public static class UsersServlet extends VaadinServlet {
	}
}
