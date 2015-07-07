package com.datastax.creditcard.gui;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.annotation.WebServlet;

import org.joda.time.DateTime;

import com.datastax.creditcard.model.Issuer;
import com.datastax.creditcard.model.Transaction;
import com.datastax.creditcard.model.Transaction.Status;
import com.datastax.creditcard.model.User;
import com.datastax.creditcard.services.CreditCardService;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Title("Enter Transaction")
@Widgetset("com.datastax.creditcard.gui.TransactionWidgetSet")
@Theme("reindeer")
@Push
public class TransactionUI extends UI {

	private static final int NO_OF_ISSUERS = 5000000;
	protected static final int TIMEOUT_IN_SEC = 60;

	Button save = new Button("Save");
	Button confirm = new Button("Confirm");
	Label status = new Label("");

	TextField userIdF = new TextField("User");
	TextField issuerF = new TextField("Issuer");
	TextField amount = new TextField("Amount");

	DateTime transactionDate = DateTime.now();
	CreditCardService service = new CreditCardService();

	Transaction t;

	private String issuerId;
	private String userId;

	private User user;
	private Issuer issuer;

	@Override
	protected void init(VaadinRequest request) {
		
		this.issuerId = "Issuer" + new Double(Math.random() * NO_OF_ISSUERS).intValue();
		this.userIdF.setValue("" + new Double(Math.random() * 5).intValue());
		this.issuer = this.service.getIssuerById(issuerId);
		this.user = this.service.getUserById(userIdF.getValue());

		configureComponents();
		buildLayout();
	}

	private void buildLayout() {

		confirm.setVisible(false);
		Label ccNo = new Label("Transaction for " + userIdF.getValue());
		ccNo.setStyleName("h2");

		FormLayout form = new FormLayout();
		form.setSizeUndefined();
		form.setMargin(true);

		HorizontalLayout actions = new HorizontalLayout(save, confirm);
		actions.setSpacing(true);
		amount.setNullSettingAllowed(false);

		form.addComponents(userIdF, issuerF, amount, actions);
		
		VerticalLayout mainLayout = new VerticalLayout(ccNo, form);
		mainLayout.setSpacing(true);
		setContent(mainLayout);

		save.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				if (amount.getValue() == null || amount.getValue().equals("")){
					displayNotification("Amount is needed");
					return;
				}
								
				issuer = service.getIssuerById(issuerF.getValue());
				user = service.getUserById(userIdF.getValue());
				
				double amountValue = new Double(amount.getValue());
				Map<String, Double> items = new HashMap<String, Double>();
				items.put("item0", amountValue);
				
				t.setLocation(issuer.getLocation());
				t.setIssuer(issuer.getId());
				t.setCreditCardNo(user.getCreditCardNo());
				t.setUserId(user.getUserId());		
				t.setAmount(amountValue);
				t.setItems(items);
				t.setStatus(Status.APPROVED.toString());

				t = service.processTransaction(t);
								
				if (t.getStatus().equals(Status.CLIENT_APPROVAL.toString())){
					displayNotification("Approval Needed for " + t.getAmount() + " at " + t.getIssuer() + "-" + t.getLocation());
					confirm.setVisible(true);

					int count = 0;					
					Transaction pendingT = service.getTransaction(t.getTransactionId());
					while (pendingT.getStatus().equals(Status.CHECK.toString())){
						
						sleep(1000);
						
						if (count++ > TIMEOUT_IN_SEC){
							break;
						}
						pendingT = service.getTransaction(t.getTransactionId());
						displayNotification("Checking", 1000);
					}

				}else if (t.getStatus().equals(Status.CHECK.toString())){
					displayNotification("Checking");
					confirm.setVisible(false);
										
					int count = 0;					
					Transaction pendingT = service.getTransaction(t.getTransactionId());
					while (pendingT.getStatus().equals(Status.CHECK.toString())){
						
						sleep(1000);
						
						if (count++ > TIMEOUT_IN_SEC){
							displayNotification("Transaction failed due to Timeout");
							confirm.setVisible(false);
							pendingT.setStatus(Status.CLIENT_DECLINED.toString());
							break;
						}
						pendingT = service.getTransaction(t.getTransactionId());
					}
					
					if (pendingT.getStatus().equals(Status.APPROVED.toString()) 
							|| pendingT.getStatus().equals(Status.CLIENT_APPROVED.toString())){
						
						displayNotification("Transaction Approved");
						confirm.setVisible(false);
						resetTransaction();
						
					}else if (pendingT.getStatus().equals(Status.DECLINED.toString())){
						displayNotification("Transaction Declined");
						confirm.setVisible(false);
						resetTransaction();					
					}
					
				}else if (t.getStatus().equals(Status.APPROVED.toString())){
					displayNotification("Transaction Approved");
					confirm.setVisible(false);
					resetTransaction();
				}else{
					displayNotification("Status : " + t.getStatus().toString());
					confirm.setVisible(true);
				}				
			}
		});

		confirm.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				System.out.println("Confirming : " + t.toString());
				t.setStatus(Status.CLIENT_APPROVED.toString());				
				Transaction result = service.processTransaction(t);
				
				if (result.getStatus().equals(Transaction.Status.DECLINED.toString())){
					confirm.setVisible(false);	
					displayNotification("Transaction " + t.getTransactionId() + " failed due to " + t.getNotes());
					
				}else if (result.getStatus().equals(Transaction.Status.APPROVED.toString()) || 
						result.getStatus().equals(Transaction.Status.CLIENT_APPROVED.toString())){
					
					confirm.setVisible(false);
					displayNotification("Transaction " + t.getTransactionId() + " approved.");					
				}else{
					displayNotification("Transaction " + t.getTransactionId() + ".");
				}
				
				//Reseting after confirm.
				resetTransaction();
			}
		});
	}
	
	private void displayNotification (String msg, int delay){
		
		Notification n = new Notification(msg,Type.HUMANIZED_MESSAGE);
		n.setDelayMsec(delay);
		n.show(Page.getCurrent());
	}

	private void displayNotification (String msg){
		this.displayNotification(msg, 2500);
	}

	private void configureComponents() {
		resetTransaction();
	}

	private void resetTransaction() {
		t = new Transaction();
		t.setTransactionTime(new Date());
		t.setTransactionId(UUID.randomUUID().toString());
		t.setLocation(issuer.getLocation());
		t.setIssuer(issuer.getId());
		t.setCreditCardNo(user.getCreditCardNo());
		t.setUserId(user.getUserId());		
		
		this.amount.setValue("");

		amount.focus();
	}

	private void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@WebServlet(urlPatterns = { "/transaction/*" })
	@VaadinServletConfiguration(ui = TransactionUI.class, productionMode = false)
	public static class TransactionServlet extends VaadinServlet {
	}
}
