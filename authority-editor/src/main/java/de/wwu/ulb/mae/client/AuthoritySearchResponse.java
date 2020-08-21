/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.client;

import de.wwu.ulb.authorities.marc.MarcData;

import java.util.ArrayList;
import java.util.List;

public class AuthoritySearchResponse {

    private List<MarcData> marcDatas = new ArrayList<>();

    private int numberOfResults;

    public AuthoritySearchResponse() {
    }

    public AuthoritySearchResponse(List<MarcData> marcDatas, int numberOfResults) {
        this.marcDatas = marcDatas;
        this.numberOfResults = numberOfResults;
    }

    public List<MarcData> getMarcDatas() {
        return marcDatas;
    }

    public void setMarcDatas(List<MarcData> marcDatas) {
        this.marcDatas = marcDatas;
    }

    public int getNumberOfResults() {
        return numberOfResults;
    }

    public void setNumberOfResults(int numberOfResults) {
        this.numberOfResults = numberOfResults;
    }
}
