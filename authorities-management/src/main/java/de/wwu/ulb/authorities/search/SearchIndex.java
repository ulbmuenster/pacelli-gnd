/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.search;

public enum SearchIndex {

    Geographical("dnb.gnd.sru.index.geographical", "dnb.gnd.sru.query.geographical"),

    Event("dnb.gnd.sru.index.event", "dnb.gnd.sru.query.event"),

    CorporateCore("dnb.gnd.sru.index.corporatecore", "dnb.gnd.sru.query.corporatecore"),

    Name("dnb.gnd.sru.index.name", "dnb.gnd.sru.query.name"),

    Person("dnb.gnd.sru.index.person", "dnb.gnd.sru.query.person"),

    Keyword("dnb.gnd.sru.index.keyword", "dnb.gnd.sru.query.keyword"),

    Work("dnb.gnd.sru.index.work", "dnb.gnd.sru.query.work"),

    Subject("dnb.gnd.sru.index.subject", "dnb.gnd.sru.query.subject"),

    Identifier("dnb.gnd.sru.index.identifier", "dnb.gnd.sru.query.identifier");

    private String indexConfig;

    private String queryConfig;

    SearchIndex(String indexConfig, String queryConfig) {
        this.indexConfig = indexConfig;
        this.queryConfig = queryConfig;
    }

    public String getIndexConfig() {
        return indexConfig;
    }

    public String getQueryConfig() {
        return queryConfig;
    }
}
