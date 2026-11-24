package com.portfolio.library;

import java.time.LocalDate;
import java.util.*;

/** In-memory domain service. Persistence is handled separately by CsvStore. */
public class Library {
    private final LinkedHashMap<String, Book> books = new LinkedHashMap<>();
    private final List<Loan> loans = new ArrayList<>();

    public Collection<Book> books() { return Collections.unmodifiableCollection(books.values()); }
    public List<Loan> loans() { return Collections.unmodifiableList(loans); }
    public void addBook(Book book) {
        if (books.containsKey(book.id())) throw new IllegalArgumentException("A book with that ID already exists.");
        books.put(book.id(), book);
    }
    public void deleteBook(String id) {
        if (!books.containsKey(id)) throw new IllegalArgumentException("Book not found.");
        if (loans.stream().anyMatch(l -> l.bookId().equals(id)))
            throw new IllegalArgumentException("Cannot delete a book with loan history.");
        books.remove(id);
    }
    public void addImportedLoan(Loan loan) {
        if (!books.containsKey(loan.bookId())) throw new IllegalArgumentException("Loan references missing book");
        if (loans.stream().anyMatch(l -> l.id().equals(loan.id()))) throw new IllegalArgumentException("Duplicate loan ID");
        if (loan.active() && loans.stream().anyMatch(l -> l.bookId().equals(loan.bookId()) && l.active()))
            throw new IllegalArgumentException("Book has multiple active loans");
        loans.add(loan);
        if (loan.active()) books.put(loan.bookId(), books.get(loan.bookId()).withAvailability(false));
    }
    public Loan borrow(String bookId, String borrower, LocalDate today, int days) {
        Book book = books.get(bookId);
        if (book == null || !book.available()) throw new IllegalArgumentException("Book is not available.");
        if (borrower == null || borrower.isBlank()) throw new IllegalArgumentException("Enter a member name.");
        if (days < 1 || days > 90) throw new IllegalArgumentException("Loan duration must be 1–90 days.");
        Loan loan = new Loan(UUID.randomUUID().toString(), bookId, borrower.trim(), today, today.plusDays(days), null);
        loans.add(loan); books.put(bookId, book.withAvailability(false)); return loan;
    }
    public void returnLoan(String id, LocalDate today) {
        for (int i = 0; i < loans.size(); i++) {
            Loan loan = loans.get(i);
            if (!loan.id().equals(id)) continue;
            if (!loan.active()) throw new IllegalArgumentException("Loan was already returned.");
            if (today.isBefore(loan.borrowedOn())) throw new IllegalArgumentException("Return date precedes loan date.");
            loans.set(i, loan.returned(today));
            Book book = books.get(loan.bookId()); books.put(book.id(), book.withAvailability(true)); return;
        }
        throw new IllegalArgumentException("Loan not found.");
    }
    public List<Book> search(String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return books.values().stream().filter(b -> (b.id()+" "+b.title()+" "+b.author()+" "+b.category()).toLowerCase(Locale.ROOT).contains(term)).toList();
    }
    public long availableCount() {return books.values().stream().filter(Book::available).count();}
    public long activeLoanCount() {return loans.stream().filter(Loan::active).count();}
    public long overdueCount(LocalDate today) {return loans.stream().filter(l -> l.overdue(today)).count();}
}
