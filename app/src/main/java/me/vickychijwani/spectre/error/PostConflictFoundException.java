package me.vickychijwani.spectre.error;

public class PostConflictFoundException extends RuntimeException {

    public PostConflictFoundException() {
        super("POST CONFLICT FOUND: see logs for details");
    }

}
