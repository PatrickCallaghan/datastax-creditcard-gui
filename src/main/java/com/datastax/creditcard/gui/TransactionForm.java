package com.datastax.creditcard.gui;

import com.datastax.creditcard.model.Transaction;
import com.datastax.creditcard.model.Transaction.Status;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

/* Create custom UI Components.
 *
 * Create your own Vaadin components by inheritance and composition.
 * This is a form component inherited from VerticalLayout. Use
 * Use BeanFieldGroup to bind data fields from DTO to UI fields.
 * Similarly named field by naming convention or customized
 * with @PropertyId annotation.
 */
public class TransactionForm extends FormLayout {

    Button save = new Button("Save");
    Button cancel = new Button("Cancel");
    
    Button openUser = new Button("User");
    Button openIssuer =  new Button("Issuer");
    
    TextField transactionId = new TextField("Transaction Id");
    TextField transactionTime = new TextField("Time");
    TextField issuer = new TextField("Issuer");
    TextField location = new TextField("Location");
    TextField amount = new TextField("Amount");
    TextArea notes = new TextArea("Notes");
    ComboBox status = new ComboBox("Status");
    
    Transaction transaction;

    // Easily bind forms to beans and manage validation and buffering
    BeanFieldGroup<Transaction> formFieldBindings;

    public TransactionForm() {
    	
    	notes.setRows(3);
    	
    	for (Status statusType : Transaction.Status.values()){
    		status.addItem(statusType.toString());
    	}
    	
    	save.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
		        try {
		            // Commit the fields from UI to DAO
		            formFieldBindings.commit();

		            // Save DAO to backend with direct synchronous service API
		            getUI().service.updateStatusNotes(status.getValue().toString(), notes.getValue(), transaction.getTransactionId());
		            
		            String msg = String.format("Saved '%s'.",
		                    transaction.getTransactionId());
		            Notification.show(msg,Type.TRAY_NOTIFICATION);
		            getUI().transactionForm.setVisible(false);
		            getUI().refreshTransactionsOverride();
		            
		        } catch (FieldGroup.CommitException e) {
		            // Validation exceptions could be shown here
		        }
			}
		});
    	
    	cancel.addClickListener(new ClickListener() {			
			@Override
			public void buttonClick(ClickEvent event) {
		        Notification.show("Cancelled", Type.TRAY_NOTIFICATION);
		        getUI().transactionForm.setVisible(false);
			}
		});
    	
    	openUser.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				getUI().transactionForm.setVisible(false);		
		        getUI().userForm.setVisible(true);			   
			}
		});
    	
    	openIssuer.addClickListener(new ClickListener() {
			
			@Override
			public void buttonClick(ClickEvent event) {
				getUI().transactionForm.setVisible(false);		
		        getUI().issuerForm.setVisible(true);			   
			}
		});
        configureComponents();
        buildLayout();
    }

    private void configureComponents() {
        save.setStyleName(ValoTheme.BUTTON_PRIMARY);
        save.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        setVisible(false);
    }

    private void buildLayout() {
        setSizeUndefined();
        setMargin(true);

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        actions.setSpacing(true);
        
        HorizontalLayout moreActions = new HorizontalLayout(openUser, openIssuer);
        moreActions.setSpacing(true);
              
		addComponents(actions, transactionId, transactionTime, issuer, location, amount, status, notes, moreActions);		
    }

    void edit(Transaction transaction) {
        this.transaction = transaction;
        if(transaction != null) {
            formFieldBindings = BeanFieldGroup.bindFieldsBuffered(transaction, this);
            status.focus();
        }          

        setVisible(transaction != null);
    }

    @Override
    public BlacklistUI getUI() {
        return (BlacklistUI) super.getUI();
    }

}
