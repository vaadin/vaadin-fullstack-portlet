package org.vaadin.springportlet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.vaadin.springportlet.backend.Book;

import com.vaadin.data.Validator;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.event.ShortcutAction;
import com.vaadin.ui.Field;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

@org.springframework.stereotype.Component
@Scope(proxyMode = ScopedProxyMode.NO, value = "prototype")
public class BookForm extends BookFormDesign {

    @Autowired
    LibraryPortletUserService service;

    private Window popup;

    private Book entity;

    private BiConsumer<BookForm, Book> saveHandler;

    private BeanFieldGroup<Book> fieldGroup;

    private BiConsumer<BookForm, Book> deleteHandler;

    @PostConstruct
    private void createContent() {
        borrowedBy.addItems(service.getCompanyUserEmails());
        saveButton.setClickShortcut(ShortcutAction.KeyCode.ENTER, null);
        saveButton.addClickListener(event -> saveIfValid());
        deleteButton
                .addClickListener(event -> deleteHandler.accept(this, entity));
    }

    public Window openInModalPopup() {
        popup = new Window("Edit entry", this);
        popup.setModal(true);
        popup.setWidth("70%");
        UI.getCurrent().addWindow(popup);
        name.focus();
        return popup;
    }

    public void setEntity(Book book) {
        entity = book;
        fieldGroup = BeanFieldGroup.<Book> bindFieldsUnbuffered(entity, this);
        fieldGroup.setItemDataSource(entity);
        validationError.setVisible(false);
        deleteButton.setEnabled(entity.isPersistent());
    }

    private void saveIfValid() {
        List<String> validationErrors = new ArrayList<>();
        for (Field<?> field : fieldGroup.getFields()) {
            try {
                field.validate();
            } catch (Validator.InvalidValueException e) {
                validationErrors.add(field.getCaption());
            }
        }
        if (validationErrors.isEmpty()) {
            validationError.setVisible(false);
            saveHandler.accept(this, entity);
        } else {
            String errorFields = validationErrors.stream()
                    .collect(Collectors.joining(", "));
            validationError
                    .setValue("Please fix errors in the following fields: "
                            + errorFields);
            validationError.setVisible(true);
        }
    }

    public void setSaveHandler(BiConsumer<BookForm, Book> saveHandler) {
        this.saveHandler = saveHandler;
    }

    public void setDeleteHandler(BiConsumer<BookForm, Book> deleteHandler) {
        this.deleteHandler = deleteHandler;
    }

    public Window getPopup() {
        return popup;
    }

}
