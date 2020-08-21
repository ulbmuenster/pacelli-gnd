/*
 * This file is part of transform-pacelli-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform.model;

public class DateType {

    private String date;

    private String exactDate;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getExactDate() {
        return exactDate;
    }

    public void setExactDate(String exactDate) {
        this.exactDate = exactDate;
    }

    public boolean isExact() {
        return exactDate != null;
    }
}
