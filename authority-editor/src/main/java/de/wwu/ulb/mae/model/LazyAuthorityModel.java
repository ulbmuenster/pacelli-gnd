/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.model;

import de.wwu.ulb.authorities.marc.MarcData;
import de.wwu.ulb.authorities.search.SearchIndex;
import de.wwu.ulb.mae.client.AuthoritySearchResponse;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LazyAuthorityModel extends LazyDataModel<MarcData> {

    private WebClient webClient;

    private String authoritiesSearchServiceQuery;

    private List<MarcData> authorityEntries;

    private String firstName;

    private String lastName;

    public LazyAuthorityModel(
            WebClient webClient, String authoritiesSearchServiceQuery, String firstName, String lastName) {
        this.webClient = webClient;
        this.authoritiesSearchServiceQuery = authoritiesSearchServiceQuery;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public MarcData getRowData(String rowKey) {
        for (MarcData marcData : authorityEntries) {
            if (rowKey.equals(marcData.getControlFields().get("001"))) {
                return marcData;
            }
        }
        return null;
    }

    @Override
    public String getRowKey(MarcData marcData) {
        return marcData.getControlFields()
                .get("001");
    }

    @Override
    public List<MarcData> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                               Map<String, FilterMeta> filters) {
        List<String> queries = buildQuery(firstName, lastName);
        Map<String, MarcData> marcMap = new HashMap<>();
        for (String query : queries) {
            AuthoritySearchResponse authoritySearchResponse = searchAuthority(
                    query, SearchIndex.Person, first, pageSize);
            for (MarcData marcData : authoritySearchResponse.getMarcDatas()) {
                marcMap.put(marcData.getControlFields().get("001"), marcData);
            }
        }
        setRowCount(marcMap.size());
        authorityEntries = new ArrayList<>(marcMap.values());
        return authorityEntries;
    }

    public int count(Map<String, FilterMeta> filterBy) {
        return 0;
    }

    private List<String> buildQuery(String firstName, String lastName) {
        String prefix = "";
        if (lastName == null) {
            lastName = "";
        }
        if (firstName == null) {
            firstName = "";
        }
        if (lastName.startsWith("d'")) {
            prefix = " " + lastName.substring(0, 1);
            lastName = lastName.substring(2).trim();
        } else if (lastName.startsWith("di") ||
                lastName.startsWith("de") ||
                lastName.startsWith("O'") ||
                lastName.startsWith("t'")) {
            prefix = " " + lastName.substring(0, 2);
            lastName = lastName.substring(2).trim();
        } else if (lastName.startsWith("van") ||
                lastName.startsWith("von") ||
                lastName.startsWith("ter")) {
            prefix = " " + lastName.substring(0, 3);
            lastName = lastName.substring(3).trim();
        }
        lastName = lastName.replaceFirst("OFMConv", "")
                .replaceFirst("OFMObs", "")
                .replaceFirst("OPraem", "")
                .replaceFirst("OFMCap", "")
                .replaceFirst("OCist", "")
                .replaceFirst("OSSCA", "")
                .replaceFirst("OFMObs", "")
                .replaceFirst("OCarm", "")
                .replaceFirst("OPräm", "")
                .replaceFirst("CPPS", "")
                .replaceFirst("CSsR", "")
                .replaceFirst("CSSp", "")
                .replaceFirst("SSND", "")
                .replaceFirst("SImC", "")
                .replaceFirst("CPPS", "")
                .replaceFirst("CSSR", "")
                .replaceFirst("MAFr", "")
                .replaceFirst("MAfr", "")
                .replaceFirst("OFMC", "")
                .replaceFirst("SMCB", "")
                .replaceFirst("CRSP", "")
                .replaceFirst("OSBM", "")
                .replaceFirst("FCJM", "")
                .replaceFirst("OSFS", "")
                .replaceFirst("SdC", "")
                .replaceFirst("OFM", "")
                .replaceFirst("OSB", "")
                .replaceFirst("SVD", "")
                .replaceFirst("OCD", "")
                .replaceFirst("OSU", "")
                .replaceFirst("PSS", "")
                .replaceFirst("OMI", "")
                .replaceFirst("FdC", "")
                .replaceFirst("SDB", "")
                .replaceFirst("SDS", "")
                .replaceFirst("SCJ", "")
                .replaceFirst("MIC", "")
                .replaceFirst("OMC", "")
                .replaceFirst("OSA", "")
                .replaceFirst("OSB", "")
                .replaceFirst("SAC", "")
                .replaceFirst("FMM", "")
                .replaceFirst("OSH", "")
                .replaceFirst("CSI", "")
                .replaceFirst("MSC", "")
                .replaceFirst("SMA", "")
                .replaceFirst("OSF", "")
                .replaceFirst("MEP", "")
                .replaceFirst("SJ", "")
                .replaceFirst("OP", "")
                .replaceFirst("CP", "")
                .replaceFirst("OH", "")
                .replaceFirst("MI", "")
                .replaceFirst("CO", "")
                .replaceFirst("AA", "")
                .replaceFirst("CM", "");
        String alternativeFirstName = "";
        if (firstName.contains(" (Taufname: ")) {
            int index = firstName.indexOf(")", firstName.indexOf(" (Taufname: ") + " (Taufname: ".length());
            if (index == -1) {
                index = firstName.indexOf("]", firstName.indexOf(" (Taufname: ") + " (Taufname: ".length());
            }
            alternativeFirstName = firstName.substring(
                    firstName.indexOf(" (Taufname: ") + " (Taufname: ".length(),
                    index);
        }
        lastName = removeBrackets(lastName);
        if (lastName.contains(",")) {
            lastName = lastName.substring(0, lastName.indexOf(","))
                    .trim();
        }
        firstName = removeBrackets(firstName);
        if (firstName.contains(",")) {
            firstName = firstName.substring(0, firstName.indexOf(","))
                    .trim();
        }
        String[] lastNames = lastName.split("/");
        String[] firstNames = firstName.split("/");
        List<String> queries = new ArrayList<>();
        for (String lastNameSplitted : lastNames) {
            for (String firstnameSplitted : firstNames) {
                if (lastNameSplitted.isEmpty()) {
                    queries.add(firstnameSplitted);
                } else if (firstnameSplitted.isEmpty()) {
                    queries.add(lastNameSplitted);
                } else {
                    queries.add(lastNameSplitted + ", " + firstnameSplitted + prefix);
                }
            }
            if (!alternativeFirstName.isEmpty()) {
                queries.add(lastNameSplitted + ", " + alternativeFirstName);
            }
        }
        return queries;
    }

    private String removeBrackets(String in) {
        if (in.isEmpty()) {
            return in;
        }
        int firstIndex = in.indexOf("(");
        int lastIndex = -1;
        if (firstIndex >= 0) {
            lastIndex = in.indexOf(")");
        } else {
            firstIndex = in.indexOf("[");
            if (firstIndex >= 0) {
                lastIndex = in.indexOf("]");
            }
        }
        if (firstIndex == -1) {
            return in;
        }
        StringBuilder out = new StringBuilder();
        if (firstIndex > 0) {
            out.append(in.substring(0, firstIndex));
        }
        if (lastIndex > 0 && lastIndex < in.length()) {
            out.append(in.substring(lastIndex + 1));
        }
        return out.toString();
    }

    private AuthoritySearchResponse searchAuthority(String query, SearchIndex index, int first, int pageSize) {
        Uni<AuthoritySearchResponse> result = webClient.get(authoritiesSearchServiceQuery +
                        "?query=" + query + "&index=" + index +
                        "&startPosition=" + first + "&maxResults=" + pageSize)
                .send()
                .onItem()
                .transform(response -> {
                    if (response.statusCode() == 200) {
                        return response.bodyAsJson(AuthoritySearchResponse.class);
                    } else {
                        return new AuthoritySearchResponse();
                    }
                });
        AuthoritySearchResponse authoritySearchResponse = result.await()
                .indefinitely();
        return authoritySearchResponse;
    }

}
