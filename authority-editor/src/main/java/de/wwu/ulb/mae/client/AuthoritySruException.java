/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.client;

import javax.ws.rs.WebApplicationException;

public class AuthoritySruException extends WebApplicationException {

    public AuthoritySruException() {
        super();
    }

    public AuthoritySruException(String message) {
        super(message);
    }
}
