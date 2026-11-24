package com.portfolio.library;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Modern JavaFX desktop UI backed by the separately tested Library domain. */
public class LibraryApp extends Application {
    private final Path storage=Paths.get(System.getProperty("user.home"),".library-desk-portfolio");
    private Library library;
    private final TableView<Book> bookTable=new TableView<>();
    private final TableView<Loan> loanTable=new TableView<>();
    private final Label totalLabel=new Label(), availableLabel=new Label(), loansLabel=new Label(), overdueLabel=new Label();
    private final TextField search=new TextField();

    @Override public void start(Stage stage) throws Exception {
        library=CsvStore.load(storage);
        if(library.books().isEmpty()) {
            library.addBook(new Book("BK-101","Clean Code","Robert Martin","Software",true));
            library.addBook(new Book("BK-102","Algorithms","Robert Sedgewick","Computer Science",true));
            library.addBook(new Book("BK-103","The Pragmatic Programmer","Andrew Hunt","Software",true));
            library.addBook(new Book("BK-104","Designing Data-Intensive Applications","Martin Kleppmann","Data",true));
            CsvStore.save(library,storage);
        }
        VBox root=new VBox(22); root.getStyleClass().add("root"); root.setPadding(new Insets(28));
        Label kicker=new Label("LIBRARY DESK / MANAGEMENT CONSOLE"); kicker.getStyleClass().add("eyebrow");
        Label heading=new Label("Library Management"); heading.getStyleClass().add("heading");
        Label sub=new Label("Catalogue, lending and return management — local demo data only.");sub.getStyleClass().add("subtitle");
        VBox header=new VBox(4,kicker,heading,sub);
        HBox metrics=new HBox(15,card("Books",totalLabel),card("Available",availableLabel),card("On loan",loansLabel),card("Overdue",overdueLabel));
        configureBooks();configureLoans();
        search.setPromptText("Search by title, author, category or book ID...");search.setPrefWidth(420);
        search.textProperty().addListener((o,a,b)->refresh());
        Button add=button("+ Add book",this::addBook); Button delete=button("Remove book",this::removeBook);
        Button borrow=button("Borrow selected",this::borrowBook);Button ret=button("Return selected loan",this::returnBook);
        HBox bookActions=new HBox(12,search,add,delete,borrow);bookActions.setAlignment(Pos.CENTER_LEFT);
        Label booksTitle=new Label("Catalogue");booksTitle.getStyleClass().add("section-heading");
        Label loansTitle=new Label("Loan history");loansTitle.getStyleClass().add("section-heading");
        HBox loanActions=new HBox(12,loansTitle,ret);loanActions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(bookTable,Priority.ALWAYS);VBox.setVgrow(loanTable,Priority.ALWAYS);
        root.getChildren().addAll(header,metrics,booksTitle,bookActions,bookTable,loanActions,loanTable);
        Scene scene=new Scene(root,1140,810);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/library.css")).toExternalForm());
        stage.setTitle("LibraryDesk | Portfolio JavaFX Application");stage.setScene(scene);stage.show();refresh();
    }
    private VBox card(String name,Label value) {
        Label caption=new Label(name);caption.getStyleClass().add("metric-caption");value.getStyleClass().add("metric-value");
        VBox panel=new VBox(7,caption,value);panel.getStyleClass().add("metric-card");panel.setPrefWidth(245);return panel;
    }
    private Button button(String text,Runnable action){Button b=new Button(text);b.getStyleClass().add("action-button");b.setOnAction(e->action.run());return b;}
    private void configureBooks(){
        bookTable.getColumns().addAll(column("ID",Book::id,105),column("Title",Book::title,300),column("Author",Book::author,220),column("Category",Book::category,175),column("Availability",b->b.available()?"Available":"On loan",120));
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);bookTable.setPrefHeight(245);
        bookTable.setPlaceholder(new Label("No books found. Try another search or add a book."));
    }
    private void configureLoans(){
        loanTable.getColumns().addAll(loanColumn("Book ID",Loan::bookId,120),loanColumn("Member",Loan::borrower,220),loanColumn("Borrowed",l->l.borrowedOn().toString(),135),loanColumn("Due",l->l.dueOn().toString(),135),loanColumn("Status",l->l.active()?(l.overdue(LocalDate.now())?"Overdue":"On loan"):"Returned",150));
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);loanTable.setPrefHeight(210);
        loanTable.setPlaceholder(new Label("Borrow a book to see its loan history."));
    }
    private TableColumn<Book,String> column(String title,java.util.function.Function<Book,String> fn,int size){TableColumn<Book,String> c=new TableColumn<>(title);c.setCellValueFactory(v->new SimpleStringProperty(fn.apply(v.getValue())));c.setPrefWidth(size);return c;}
    private TableColumn<Loan,String> loanColumn(String title,java.util.function.Function<Loan,String> fn,int size){TableColumn<Loan,String> c=new TableColumn<>(title);c.setCellValueFactory(v->new SimpleStringProperty(fn.apply(v.getValue())));c.setPrefWidth(size);return c;}
    private void refresh(){
        bookTable.setItems(FXCollections.observableArrayList(library.search(search.getText())));
        loanTable.setItems(FXCollections.observableArrayList(library.loans().reversed()));
        totalLabel.setText(String.valueOf(library.books().size()));availableLabel.setText(String.valueOf(library.availableCount()));
        loansLabel.setText(String.valueOf(library.activeLoanCount()));overdueLabel.setText(String.valueOf(library.overdueCount(LocalDate.now())));
    }
    private void save(){try{CsvStore.save(library,storage);refresh();}catch(IOException e){alert("Save failed",e.getMessage());}}
    private void addBook(){
        Dialog<Book> d=new Dialog<>();d.setTitle("Add a book");d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,new ButtonType("Add book",ButtonBar.ButtonData.OK_DONE));
        TextField id=new TextField(),title=new TextField(),author=new TextField(),category=new TextField("General");
        GridPane form=new GridPane();form.setHgap(12);form.setVgap(12);form.setPadding(new Insets(12));
        String[] labels={"Book ID","Title","Author","Category"};TextField[] fields={id,title,author,category};
        for(int i=0;i<fields.length;i++){form.add(new Label(labels[i]),0,i);form.add(fields[i],1,i);} d.getDialogPane().setContent(form);
        d.setResultConverter(b->b.getButtonData()==ButtonBar.ButtonData.OK_DONE?new Book(id.getText(),title.getText(),author.getText(),category.getText(),true):null);
        try {d.showAndWait().ifPresent(book->{try{library.addBook(book);save();}catch(IllegalArgumentException e){alert("Cannot add book",e.getMessage());}});}
        catch (IllegalArgumentException e){alert("Missing details",e.getMessage());}
    }
    private void removeBook(){Book b=bookTable.getSelectionModel().getSelectedItem();if(b==null){alert("Select a book","Choose a book first.");return;}
        if(!confirm("Remove book?","Delete \""+b.title()+"\"? Books with loan history cannot be deleted."))return;
        try{library.deleteBook(b.id());save();}catch(IllegalArgumentException e){alert("Cannot delete",e.getMessage());}}
    private void borrowBook(){Book b=bookTable.getSelectionModel().getSelectedItem();if(b==null){alert("Select a book","Choose a book first.");return;}
        TextInputDialog dialog=new TextInputDialog();dialog.setTitle("Borrow book");dialog.setHeaderText("Borrow: "+b.title());dialog.setContentText("Fictional member name:");
        dialog.showAndWait().ifPresent(name->{try{library.borrow(b.id(),name,LocalDate.now(),14);save();}catch(IllegalArgumentException e){alert("Borrow failed",e.getMessage());}});
    }
    private void returnBook(){Loan l=loanTable.getSelectionModel().getSelectedItem();if(l==null){alert("Select a loan","Choose a loan to return.");return;}
        if(!confirm("Return book?","Mark the selected book as returned?"))return;
        try{library.returnLoan(l.id(),LocalDate.now());save();}catch(IllegalArgumentException e){alert("Return failed",e.getMessage());}}
    private void alert(String title,String message){Alert a=new Alert(Alert.AlertType.INFORMATION,message,ButtonType.OK);a.setTitle(title);a.setHeaderText(title);a.showAndWait();}
    private boolean confirm(String title,String message){Alert a=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.YES,ButtonType.NO);a.setTitle(title);a.setHeaderText(title);return a.showAndWait().orElse(ButtonType.NO)==ButtonType.YES;}
    public static void main(String[] args){launch(args);}
}
