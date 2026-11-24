package com.portfolio.library;

import java.time.LocalDate;
/** Loan for a fictional library member, including completed-loan records. */
public record Loan(String id, String bookId, String borrower, LocalDate borrowedOn, LocalDate dueOn, LocalDate returnedOn) {
    public Loan {
        if (id == null || id.isBlank() || bookId == null || bookId.isBlank() || borrower == null || borrower.isBlank())
            throw new IllegalArgumentException("Loan ID, book ID and member are required");
        if (borrowedOn == null || dueOn == null || dueOn.isBefore(borrowedOn)) throw new IllegalArgumentException("Invalid loan dates");
    }
    public boolean active() { return returnedOn == null; }
    public boolean overdue(LocalDate today) { return active() && dueOn.isBefore(today); }
    public Loan returned(LocalDate date) {return new Loan(id,bookId,borrower,borrowedOn,dueOn,date);}
}
