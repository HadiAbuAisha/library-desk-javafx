package com.portfolio.library;

/** Immutable book description (the book ID is a human-readable identifier). */
public record Book(String id, String title, String author, String category, boolean available) {
    public Book {
        if (id == null || id.isBlank() || title == null || title.isBlank() || author == null || author.isBlank())
            throw new IllegalArgumentException("Book ID, title and author are required");
        if (category == null || category.isBlank()) category = "General";
    }
    public Book withAvailability(boolean isAvailable) {return new Book(id,title,author,category,isAvailable);}
}
