package com.datastax.creditcard.gui;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.creditcard.model.UserRule;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class UserRuleWindow extends Window {

	private static Logger logger = LoggerFactory.getLogger(UserRuleWindow.class);
	private Button ok;
	private TextField amountField;
	
	private TextField ruleNameField;
	private TextField merchantField;
	private TextField notField;
	private TextField nodField;

	private String ruleId = UUID.randomUUID().toString();
	
	FormLayout content = new FormLayout();
		
	public UserRuleWindow(String userId){
		super("User Rule Window"); // Set window caption
        
        // Some basic content for the window        
        center();

        // Some basic content for the window        
        ruleNameField = new TextField("Rule Name");
        ruleNameField.setWidth("200px");
        merchantField = new TextField("Issuer");
        merchantField.setWidth("200px");
        amountField = new TextField("Amount");
        amountField.setWidth("200px");
        notField = new TextField("No of transactions");
        notField.setWidth("200px");
        nodField = new TextField("No of days");
        nodField.setWidth("200px");
        
        content.addComponent(ruleNameField);
        content.addComponent(merchantField);        		
        content.addComponent(amountField);
        content.addComponent(notField);
        content.addComponent(nodField);
        				
        content.setMargin(true);
        content.setWidth("400px");
        setContent(content);
        
        // Disable the close button
        setClosable(true);

        // Trivial logic for closing the sub-window
        ok = new Button("Save");        

        ok.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				
				UserRule userRule = new UserRule();
				userRule.setUserId(userId);				
				userRule.setRuleId(ruleId);
				
				userRule.setRuleName(ruleNameField.getValue());
				userRule.setMerchant(merchantField.getValue());
				userRule.setAmount(new Double(amountField.getValue()));
				userRule.setNoOfDays(Integer.parseInt(nodField.getValue()));
				userRule.setNoOfTransactions(Integer.parseInt(notField.getValue()));
				
				getUI().service.insertUserRule(userRule);
				getUI().populateUserRules();
								
	            String msg = String.format("Saved user rule for user '%s'.", userId);
	            Notification.show(msg,Type.HUMANIZED_MESSAGE);
	            close();
			}
        });
        content.addComponent(ok);
        logger.info("User Rule ready");
	}
    
    public UserRuleWindow(UserRule userRule) {
    	this(userRule.getUserId());
    	
    	this.amountField.setValue("" + userRule.getAmount());
    	this.merchantField.setValue(userRule.getMerchant());
    	this.notField.setValue("" +userRule.getNoOfTransactions());
    	this.nodField.setValue("" +userRule.getNoOfDays() );
    	this.ruleNameField.setValue(userRule.getRuleName());
    	this.ruleId = userRule.getRuleId();
    	
    	ok = new Button("Save");
	}

	@Override
    public UsersUI getUI() {
        return (UsersUI) super.getUI();
    }
    
}