package com.reservaya.auth.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = -122749211558826935L;

	public UsernameAlreadyExistsException(String username) {
        super("Username '" + username + "' is already registered");
    }
}
