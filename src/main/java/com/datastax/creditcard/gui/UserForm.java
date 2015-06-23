package com.datastax.creditcard.gui;

import com.datastax.creditcard.model.User;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public class UserForm extends FormLayout {
	
    Button back = new Button("back");
    Button blacklist = new Button("blackList");
       
    TextField userId = new TextField("User Id");
    TextField firstname = new TextField("First");
    TextField lastname = new TextField("Last");
    TextField cityName = new TextField("City");
    TextField stateName = new TextField("State");
    TextField gender = new TextField("Gender");
    TextField creditCardNo = new TextField("Credit Card no");
    
    User user;

    // Easily bind forms to beans and manage validation and buffering
    BeanFieldGroup<User> formFieldBindings;

    public UserForm() {
    	
    	back.addClickListener(new ClickListener() {			
			@Override
			public void buttonClick(ClickEvent event) {
				
				getUI().transactionForm.setVisible(true);		
				setVisible(false);
			}
		});
    	blacklist.addClickListener(new ClickListener() {
    	    public void buttonClick(ClickEvent event) {
    	        BlacklistWindow sub = new BlacklistWindow(user);
    	        
    	        // Add it to the root component
    	        getUI().addWindow(sub);
    	    }
    	});
    	    	
        configureComponents();
        buildLayout();
        setVisible(false);
    }

    private void configureComponents() {

        back.setStyleName(ValoTheme.BUTTON_PRIMARY);
        back.setClickShortcut(ShortcutAction.KeyCode.ENTER);        
    }

    private void buildLayout() {
        setSizeUndefined();
        setMargin(true);

        HorizontalLayout actions = new HorizontalLayout(back, blacklist);
        actions.setSpacing(true);
        
		addComponents(actions, userId, firstname, lastname, cityName, stateName, gender, creditCardNo);		
    }


    public void view(User user) {
        this.user = user;
        
        if(user != null) {
            formFieldBindings = BeanFieldGroup.bindFieldsBuffered(user, this);
            
            userId.setReadOnly(true);
            firstname.setReadOnly(true);
            lastname.setReadOnly(true);
            cityName.setReadOnly(true); 
            stateName.setReadOnly(true);
            gender.setReadOnly(true);
            creditCardNo.setReadOnly(true);
            back.focus();
        }          

        setVisible(false);
    }

    @Override
    public BlacklistUI getUI() {
        return (BlacklistUI) super.getUI();
    }
}
