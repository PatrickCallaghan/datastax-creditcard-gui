package com.datastax.creditcard.gui;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.creditcard.model.Merchant;
import com.datastax.creditcard.model.User;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;


public class BlacklistWindow extends Window {

	private static Logger logger = LoggerFactory.getLogger(BlacklistWindow.class);
	private Button ok;
	private TextField amountField;
	FormLayout content = new FormLayout();
	
	public BlacklistWindow(){
		super("BlackList Window Amount"); // Set window caption
        center();

        // Some basic content for the window        
        
        amountField = new TextField("Amount");
        amountField.setWidth("300px");
        content.addComponent(amountField);
        content.setMargin(true);
        content.setWidth("400px");
        setContent(content);
        
        // Disable the close button
        setClosable(true);

        // Trivial logic for closing the sub-window
        ok = new Button("Add");        
	}
	
    public BlacklistWindow(final Merchant merchant) {
    	this();
        ok.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				getUI().service.insertBlacklistMerchant(new Date(), merchant.getId(), merchant.getLocation(), new Double(amountField.getValue()));
				close();
				
				String msg = String.format("Created blacklist rules for issuer : '%s' in '%s'", merchant.getId(), merchant.getLocation());
	            Notification.show(msg,Type.HUMANIZED_MESSAGE);
			}
        });
        content.addComponent(ok);
        logger.info("Issuer Blacklist ready");
    }
    
    public BlacklistWindow(final User user) {
    	this();
        ok.addClickListener(new ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				getUI().service.insertBlacklistCard(new Date(), user.getCreditCardNo(), new Double(amountField.getValue()));
				close();
				
	            String msg = String.format("Created blacklist rules for credit card no : '%s'.", user.getCreditCardNo());
	            Notification.show(msg,Type.HUMANIZED_MESSAGE);
			}
        });
        content.addComponent(ok);
        logger.info("User Blacklist ready");
    }

    @Override
    public BlacklistUI getUI() {
        return (BlacklistUI) super.getUI();
    }
    
}