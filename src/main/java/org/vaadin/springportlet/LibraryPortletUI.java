package org.vaadin.springportlet;

import java.text.DateFormat;
import java.util.Locale;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.spring.annotation.VaadinPortletUI;
import org.vaadin.springportlet.backend.Book;

import com.liferay.portal.model.User;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.converter.StringToDateConverter;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinPortletSession;
import com.vaadin.server.VaadinPortletSession.PortletListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Theme("mytheme")
@SuppressWarnings({ "serial", "deprecation" })
@Widgetset("org.vaadin.springportlet.AppWidgetSet")
@VaadinPortletUI
public class LibraryPortletUI extends UI {

    private static final String PROPERTY_EDIT = "edit";
    private static final String PROPERTY_LOAN = "loan";
    private static final String PROPERTY_PUBLISH_DATE = "publishDate";
    private static final String PROPERTY_NAME = "name";

    private final static String MESSAGE_MIDAIR_COLLISION = "Someone else has modified the content. Please try again.";
    private static final float LOAN_BUTTON_WIDTH = 90.0f;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private LibraryPortletUserService service;

    private Table bookListing;
    private BeanItemContainer<Book> bookContainer;

    static Logger log = Logger.getLogger(LibraryPortletUI.class);

    static class MyWarningNotification extends Notification {

        MyWarningNotification(String caption) {
            super(caption, Notification.Type.WARNING_MESSAGE);
            setIcon(FontAwesome.WARNING);
        }

        public static void show(String caption) {
            new MyWarningNotification(caption).show(Page.getCurrent());
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        ((VaadinPortletSession) VaadinPortletSession.getCurrent())
                .addPortletListener(new RenderRequestListener() {

                    @Override
                    public void handleRenderRequest(RenderRequest request,
                            RenderResponse response, UI uI) {
                        listBooks();
                    }
                });

        bookListing = new Table();
        bookContainer = new BeanItemContainer<>(Book.class);
        bookListing.setContainerDataSource(bookContainer);
        bookListing.setHeight("250px");
        bookListing.setWidth("100%");
        bookListing.addStyleName(ValoTheme.TABLE_BORDERLESS);
        bookListing.addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        bookListing.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        bookListing.addGeneratedColumn(PROPERTY_LOAN,
                (table, itemId, columnId) -> {
                    Book entity = (Book) itemId;
                    User borrower = service.getBorrower(entity);
                    if (borrower == null) {
                        if (service.isAllowedToBorrow()) {
                            Button borrow = new Button("Borrow",
                                    e -> onBorrow(entity));
                            borrow.setWidth(LOAN_BUTTON_WIDTH, Unit.PIXELS);
                            borrow.addStyleName(ValoTheme.BUTTON_PRIMARY);
                            borrow.addStyleName(ValoTheme.BUTTON_SMALL);
                            return borrow;
                        } else {
                            return "Login to loan";
                        }
                    } else if (service.isBorrowedByMe(entity)) {
                        Button mark = new Button("Return",
                                e -> onMarkAsReturned(entity));
                        mark.setWidth(LOAN_BUTTON_WIDTH, Unit.PIXELS);
                        mark.addStyleName(ValoTheme.BUTTON_DANGER);
                        mark.addStyleName(ValoTheme.BUTTON_SMALL);
                        return mark;
                    } else {
                        return "Borrowed by " + borrower.getFirstName() + " "
                                + borrower.getLastName();
                    }
                });

        if (service.isAdmin()) {
            bookListing.addGeneratedColumn(PROPERTY_EDIT,
                    (table, itemId, columnId) -> {
                        Button edit = new Button(null,
                                e -> onRowEdit((Book) itemId));
                        edit.setIcon(FontAwesome.PENCIL);
                        edit.addStyleName(ValoTheme.BUTTON_SMALL);
                        return edit;
                    });
            bookListing.setVisibleColumns(PROPERTY_NAME, PROPERTY_PUBLISH_DATE,
                    PROPERTY_LOAN, PROPERTY_EDIT);
            bookListing.setColumnHeaders("Name", "Publish Date", "", "");
            bookListing.setColumnWidth(PROPERTY_EDIT, 60);
        } else {
            bookListing.setVisibleColumns(PROPERTY_NAME, PROPERTY_PUBLISH_DATE,
                    PROPERTY_LOAN);
            bookListing.setColumnHeaders("Name", "Publish Date", "");
        }

        bookListing.setConverter(PROPERTY_PUBLISH_DATE,
                new StringToDateConverter() {
                    protected DateFormat getFormat(Locale locale) {
                        return DateFormat.getDateInstance(DateFormat.SHORT);
                    };
                });

        listBooks();

        final VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.addComponent(bookListing);
        setContent(layout);

        if (service.isAdmin()) {
            Button add = new Button("Add new Book", this::onAddClick);
            add.setIcon(FontAwesome.PLUS);
            layout.addComponent(add);
        }
    }

    private void listBooks() {
        bookContainer.removeAllItems();
        bookContainer.addAll(service.findAll());
    }

    private void onMarkAsReturned(Book book) {
        try {
            service.releaseBook(book);
        } catch (ObjectOptimisticLockingFailureException e) {
            MyWarningNotification.show(MESSAGE_MIDAIR_COLLISION);
        } catch (Exception e) {
            log.error("Failed to release book ", e);
        }
        listBooks();
    }

    private void onBorrow(Book book) {
        try {
            service.borrowBook(book);
        } catch (ObjectOptimisticLockingFailureException e) {
            MyWarningNotification.show(MESSAGE_MIDAIR_COLLISION);
        } catch (Exception e) {
            log.error("Failed to borrow book ", e);
        }
        listBooks();
    }

    private void onRowEdit(Book entity) {
        BookForm bookForm = applicationContext.getBean(BookForm.class);
        bookForm.setEntity(entity);
        bookForm.setSaveHandler(this::onBookFormSave);
        bookForm.setDeleteHandler(this::onBookDelete);
        bookForm.openInModalPopup();
    }

    private void onAddClick(Button.ClickEvent event) {
        BookForm bookForm = applicationContext.getBean(BookForm.class);
        bookForm.setEntity(new Book());
        bookForm.setSaveHandler(this::onBookFormSave);
        bookForm.openInModalPopup();
    }

    private void onBookFormSave(BookForm bookForm, Book book) {
        try {
            service.save(book);
        } catch (ObjectOptimisticLockingFailureException e) {
            MyWarningNotification.show(MESSAGE_MIDAIR_COLLISION);
        } catch (Exception e) {
            log.error("Failed to save book form", e);
        }
        listBooks();
        bookForm.getPopup().close();
    }

    private void onBookDelete(BookForm bookForm, Book book) {
        try {
            service.delete(book);
        } catch (Exception e) {
            log.error("Failed to delete book", e);
        }
        listBooks();
        bookForm.getPopup().close();
    }

    private final PortletListener myPortletListener = new RenderRequestListener() {

        @Override
        public void handleRenderRequest(RenderRequest request,
                RenderResponse response, UI uI) {
            listBooks();
        }
    };
}
