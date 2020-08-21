/*
 * This file is part of prepare-meta-mapping.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * prepare-meta-mapping is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mapping.model;

import java.util.ArrayList;
import java.util.List;

public class Mapping {

    private String meta;

    private String authorityData;

    private String gndId;

    private String[] ids;

    private String type;

    private List<String> synonyms;

    public Mapping() {
    }

    public Mapping(String meta, String authorityData, String gndId, String[] ids,
                   String type, List<String> synonyms) {
        this.meta = meta;
        this.authorityData = authorityData;
        this.gndId = gndId;
        this.ids = ids;
        this.type = type;
        this.synonyms = synonyms;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public String getAuthorityData() {
        return authorityData;
    }

    public void setAuthorityData(String authorityData) {
        this.authorityData = authorityData;
    }

    public String getGndId() {
        return gndId;
    }

    public void setGndId(String gndId) {
        this.gndId = gndId;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getSynonyms() {
        if (synonyms == null) {
            synonyms = new ArrayList<>();
        }
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }
}
