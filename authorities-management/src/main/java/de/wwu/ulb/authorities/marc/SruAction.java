/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

public enum SruAction {

    CREATE("info:srw/action/1/create"),

    REPLACE("info:srw/action/1/replace");

    private String identifier;

    SruAction(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
