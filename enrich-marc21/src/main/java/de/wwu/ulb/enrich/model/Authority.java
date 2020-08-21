/*
 * This file is part of enrich-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * enrich-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.enrich.model;

public class Authority {

    private Integer authoritiesId;

    private String gndId;

    public Authority() {
    }

    public Authority(Integer authoritiesId, String gndId) {
        this.authoritiesId = authoritiesId;
        this.gndId = gndId;
    }

    public Integer getAuthoritiesId() {
        return authoritiesId;
    }

    public void setAuthoritiesId(Integer authoritiesId) {
        this.authoritiesId = authoritiesId;
    }

    public String getGndId() {
        return gndId;
    }

    public void setGndId(String gndId) {
        this.gndId = gndId;
    }
}
