package com.portfolio.library;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Simple local persistence; CSV quoted fields and atomic replacement. */
public final class CsvStore {
    private CsvStore() {}
    private static String quoted(String value) {return "\"" + value.replace("\"", "\"\"") + "\"";}
    private static String row(String... fields) {return String.join(",", Arrays.stream(fields).map(CsvStore::quoted).toList());}

    /** RFC-4180-like parser for one CSV line, with doubled quote escaping. */
    public static List<String> parseLine(String text) {
        List<String> cols = new ArrayList<>(); StringBuilder token = new StringBuilder(); boolean inQuotes = false;
        for (int i=0;i<text.length();i++) {
            char ch=text.charAt(i);
            if (ch=='\"') {if (inQuotes && i+1<text.length() && text.charAt(i+1)=='\"') {token.append('"'); i++;} else inQuotes=!inQuotes;}
            else if (ch==',' && !inQuotes) {cols.add(token.toString()); token.setLength(0);}
            else token.append(ch);
        }
        if (inQuotes) throw new IllegalArgumentException("Malformed CSV field");
        cols.add(token.toString()); return cols;
    }
    public static Library load(Path directory) throws IOException {
        Library library=new Library();
        Path bookFile=directory.resolve("books.csv");
        if (Files.exists(bookFile)) {
            List<String> rows=Files.readAllLines(bookFile,StandardCharsets.UTF_8);
            for (int i=1;i<rows.size();i++) if (!rows.get(i).isBlank()) {
                List<String> parts=parseLine(rows.get(i));
                if (parts.size()!=5) throw new IOException("Invalid books CSV row " + (i+1));
                library.addBook(new Book(parts.get(0),parts.get(1),parts.get(2),parts.get(3),Boolean.parseBoolean(parts.get(4))));
            }
        }
        Path loanFile=directory.resolve("loans.csv");
        if (Files.exists(loanFile)) {
            List<String> rows=Files.readAllLines(loanFile,StandardCharsets.UTF_8);
            for (int i=1;i<rows.size();i++) if (!rows.get(i).isBlank()) {
                List<String> p=parseLine(rows.get(i));
                if (p.size()!=6) throw new IOException("Invalid loans CSV row " + (i+1));
                library.addImportedLoan(new Loan(p.get(0),p.get(1),p.get(2),LocalDate.parse(p.get(3)),LocalDate.parse(p.get(4)),p.get(5).isBlank()?null:LocalDate.parse(p.get(5))));
            }
        }
        return library;
    }
    public static void save(Library library, Path directory) throws IOException {
        Files.createDirectories(directory);
        List<String> books=new ArrayList<>(List.of("id,title,author,category,available"));
        for (Book b:library.books()) books.add(row(b.id(),b.title(),b.author(),b.category(),String.valueOf(b.available())));
        writeAtomic(directory.resolve("books.csv"),books);
        List<String> loans=new ArrayList<>(List.of("id,book_id,borrower,borrowed_on,due_on,returned_on"));
        for (Loan l:library.loans()) loans.add(row(l.id(),l.bookId(),l.borrower(),l.borrowedOn().toString(),l.dueOn().toString(),l.returnedOn()==null?"":l.returnedOn().toString()));
        writeAtomic(directory.resolve("loans.csv"),loans);
    }
    private static void writeAtomic(Path dest, List<String> lines) throws IOException {
        Path tmp=Files.createTempFile(dest.getParent(),"library-",".tmp");
        try {
            Files.write(tmp,lines,StandardCharsets.UTF_8);
            try {Files.move(tmp,dest,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch (AtomicMoveNotSupportedException e) {Files.move(tmp,dest,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(tmp);}
    }
}
