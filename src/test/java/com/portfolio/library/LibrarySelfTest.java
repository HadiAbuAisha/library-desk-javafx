package com.portfolio.library;

import java.nio.file.*;
import java.time.LocalDate;

/** Runs with vanilla Java 21, without the JavaFX UI dependencies. */
public final class LibrarySelfTest {
    public static void main(String[] args) throws Exception {
        Library db=new Library();
        db.addBook(new Book("B-1","Algorithms, Explained","A \"Demo\" Author","Technical",true));
        db.addBook(new Book("B-2","Database Design","Demo Author","Technical",true));
        assert db.search("algorithms").size()==1;
        var loan=db.borrow("B-1","Fictional Student",LocalDate.of(2026,11,1),14);
        assert db.availableCount()==1;
        assert loan.overdue(LocalDate.of(2026,11,20));
        try {db.borrow("B-1","Other",LocalDate.now(),14);throw new AssertionError("Double loan accepted");}
        catch(IllegalArgumentException ok) {}
        Path temp=Files.createTempDirectory("library-test-");
        CsvStore.save(db,temp);
        Library loaded=CsvStore.load(temp);
        assert loaded.books().size()==2;
        assert loaded.activeLoanCount()==1;
        assert loaded.overdueCount(LocalDate.of(2026,11,20))==1;
        assert loaded.search("Algorithms, Explained").size()==1;
        loaded.returnLoan(loaded.loans().getFirst().id(),LocalDate.of(2026,11,10));
        assert loaded.availableCount()==2;
        CsvStore.save(loaded,temp);
        assert CsvStore.load(temp).activeLoanCount()==0;
        System.out.println("All 11 Library domain / CSV persistence assertions passed.");
    }
}
