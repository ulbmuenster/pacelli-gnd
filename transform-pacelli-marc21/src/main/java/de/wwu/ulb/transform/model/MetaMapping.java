/*
 * This file is part of transform-pacelli-marc21 (tpm).
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform.model;

public class MetaMapping {

    private String mapping;

    private String gndId;

    private String de101id;

    private String de588id;

    public MetaMapping() {
    }

    public MetaMapping(String mapping, String gndId, String de101id, String de588id) {
        this.mapping = mapping;
        this.gndId = gndId;
        this.de101id = de101id;
        this.de588id = de588id;
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getGndId() {
        return gndId;
    }

    public void setGndId(String gndId) {
        this.gndId = gndId;
    }

    public String getDe101id() {
        return de101id;
    }

    public void setDe101id(String de101id) {
        this.de101id = de101id;
    }

    public String getDe588id() {
        return de588id;
    }

    public void setDe588id(String de588id) {
        this.de588id = de588id;
    }
}
