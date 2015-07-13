package com.datastax.creditcard.gui;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.annotation.WebServlet;

import org.joda.time.DateTime;

import com.datastax.creditcard.model.Merchant;
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
	ExecutorService executor = Executors.newCachedThreadPool();

	Transaction t;

	private String issuerId;
	private String userId;

	private User user;
	private Merchant merchant;

	@Override
	protected void init(VaadinRequest request) {

		this.issuerId = "Issuer" + new Double(Math.random() * NO_OF_ISSUERS).intValue();
		this.userIdF.setValue("" + new Double(Math.random() * 5).intValue());
		this.merchant = this.service.getMerchantById(issuerId);
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

				if (amount.getValue() == null || amount.getValue().equals("")) {
					displayNotification("Amount is needed");
					return;
				}
				
				merchant = service.getMerchantById(issuerF.getValue());
				user = service.getUserById(userIdF.getValue());

				double amountValue = new Double(amount.getValue());
				Map<String, Double> items = new HashMap<String, Double>();
				items.put("item0", amountValue);

				t.setLocation(merchant.getLocation());
				t.setMerchant(merchant.getId());
				t.setCreditCardNo(user.getCreditCardNo());
				t.setUserId(user.getUserId());
				t.setAmount(amountValue);
				t.setItems(items);
				t.setStatus(Status.APPROVED.toString());

				t = service.processTransaction(t);

				if (t.getStatus().equals(Status.CLIENT_APPROVAL.toString())) {
					displayNotification(t.getNotes());
					confirm.setVisible(true);

					TransactionWaiter waiter = new TransactionWaiter();					
					new Thread(waiter);
				}

				if (t.getStatus().equals(Status.CHECK.toString())) {
					displayNotification("Checking");
					confirm.setVisible(false);

					TransactionWaiter waiter = new TransactionWaiter();					
					new Thread(waiter);

				} else if (t.getStatus().equals(Status.APPROVED.toString())) {
					displayNotification("Transaction Approved");
					confirm.setVisible(false);
					resetTransaction();
				}
			}
		});

		confirm.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {

				System.out.println("Confirming : " + t.toString());
				t.setStatus(Status.CLIENT_APPROVED.toString());
				Transaction result = service.processTransaction(t);

				if (result.getStatus().equals(Transaction.Status.DECLINED.toString())) {
					confirm.setVisible(false);
					displayNotification("Transaction " + t.getTransactionId() + " failed due to " + t.getNotes());

				} else if (result.getStatus().equals(Transaction.Status.APPROVED.toString())
						|| result.getStatus().equals(Transaction.Status.CLIENT_APPROVED.toString())) {

					confirm.setVisible(false);
					displayNotification("Transaction " + t.getTransactionId() + " approved.");
				} else {
					displayNotification("Transaction " + t.getTransactionId() + ".");
				}

				// Reseting after confirm.
				resetTransaction();
			}
		});
	}

	private void displayNotification(String msg, int delay) {

		Notification n = new Notification(msg, Type.HUMANIZED_MESSAGE);
		n.setDelayMsec(delay);
		n.show(Page.getCurrent());
	}

	private void displayNotification(String msg) {
		this.displayNotification(msg, 2500);
	}

	private void configureComponents() {
		resetTransaction();
	}

	private void resetTransaction() {
		t = new Transaction();
		t.setTransactionTime(new Date());
		t.setTransactionId(UUID.randomUUID().toString());
		t.setLocation(merchant.getLocation());
		t.setMerchant(merchant.getId());
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

	class TransactionWaiter implements Runnable {

		@Override
		public void run(){
			int count = 0;
			Transaction pendingT = service.getTransaction(t.getTransactionId());

			while (pendingT.getStatus().equals(Status.CHECK.toString())
					|| pendingT.getStatus().equals(Status.CLIENT_APPROVAL.toString())) {

				sleep(1000);

				if (count++ > TIMEOUT_IN_SEC) {
					displayNotification("Transaction failed due to Timeout");
					confirm.setVisible(false);
					pendingT.setStatus(Status.DECLINED.toString());

					break;
				}
				pendingT = service.getTransaction(t.getTransactionId());
				
			}
			
			if (pendingT.getStatus().equals(Status.APPROVED.toString())
					|| pendingT.getStatus().equals(Status.CLIENT_APPROVED.toString())) {

				displayNotification("Transaction Approved");
				confirm.setVisible(false);
				resetTransaction();				
				System.out.println("Transaction Approved");

			} else if (pendingT.getStatus().equals(Status.DECLINED.toString())) {
				displayNotification("Transaction Declined");
				confirm.setVisible(false);
				resetTransaction();
				System.out.println("Transaction Declined");
			}
		}
	}

	@WebServlet(urlPatterns = { "/transaction/*" })
	@VaadinServletConfiguration(ui = TransactionUI.class, productionMode = false)
	public static class TransactionServlet extends VaadinServlet {
	}
}
