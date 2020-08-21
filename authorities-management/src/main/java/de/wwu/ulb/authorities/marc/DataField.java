/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

import java.util.List;
import java.util.Map;

public class DataField {

    private String tag;

    private String firstIndicator;

    private String secondIndicator;

    private Map<String, List<SubField>> subFields;

    public DataField() {
        this.tag = "001";
        this.firstIndicator = " ";
        this.secondIndicator = " ";
    }

    public DataField(String tag) {
        this.tag = tag;
        this.firstIndicator = " ";
        this.secondIndicator = " ";
    }

    public DataField(String tag, String firstIndicator, String secondIndicator) {
        this.tag = tag;
        this.firstIndicator = firstIndicator;
        this.secondIndicator = secondIndicator;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getFirstIndicator() {
        return firstIndicator;
    }

    public String getSecondIndicator() {
        return secondIndicator;
    }

    public Map<String, List<SubField>> getSubFields() {
        return subFields;
    }

    public void setSubFields(Map<String, List<SubField>> subFields) {
        this.subFields = subFields;
    }
}
