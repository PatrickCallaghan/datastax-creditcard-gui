package com.datastax.creditcard.gui;

import com.datastax.creditcard.model.Issuer;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public class IssuerForm extends FormLayout {

	Button back = new Button("back");
    Button blacklist = new Button("blackList");
       
    TextField id = new TextField("Issuer Id");
    TextField name = new TextField("Name");
    TextField location = new TextField("Location");
    
    Issuer issuer;

    // Easily bind forms to beans and manage validation and buffering
    BeanFieldGroup<Issuer> formFieldBindings;

    public IssuerForm() {
    	
    	back.addClickListener(new ClickListener() {			
			@Override
			public void buttonClick(ClickEvent event) {
				getUI().transactionForm.setVisible(true);
				setVisible(false);
			}
		});
    	blacklist.addClickListener(new ClickListener() {
    	    public void buttonClick(ClickEvent event) {
    	        BlacklistWindow sub = new BlacklistWindow(issuer);
    	        
    	        // Add it to the root component
    	        getUI().addWindow(sub);
    	    }
    	});

    	
        configureComponents();
        buildLayout();
    }

    private void configureComponents() {
        /* Highlight primary actions.
         *
         * With Vaadin built-in styles you can highlight the primary save button
         * and give it a keyboard shortcut for a better UX.
         */
        back.setStyleName(ValoTheme.BUTTON_PRIMARY);
        back.setClickShortcut(ShortcutAction.KeyCode.ENTER);
        setVisible(false);
    }

    private void buildLayout() {
        setSizeUndefined();
        setMargin(true);

        HorizontalLayout actions = new HorizontalLayout(back, blacklist);
        actions.setSpacing(true);
        
		addComponents(actions, id, name, location);		
    }

    void view(Issuer issuer) {
        this.issuer = issuer;
        if(issuer != null) {
            formFieldBindings = BeanFieldGroup.bindFieldsBuffered(issuer, this);
            
            id.setReadOnly(true);
            name.setReadOnly(true);
            location.setReadOnly(true); 
            back.focus();
        }          

        setVisible(false);
    }

    @Override
    public BlacklistUI getUI() {
        return (BlacklistUI) super.getUI();
    }
}
