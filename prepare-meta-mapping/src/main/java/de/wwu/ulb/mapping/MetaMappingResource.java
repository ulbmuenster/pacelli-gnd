/*
 * This file is part of prepare-meta-mapping.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * prepare-meta-mapping is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mapping;

import de.wwu.ulb.mapping.model.Mapping;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MetaMappingResource {

    final int MAX_STEPS = 5000;

    static final Logger logger = Logger.getLogger(MetaMappingResource.class.getName());

    Builder builder = new Builder();

    final String marcNamespace = "http://www.loc.gov/MARC21/slim";

    final String queryIndex = "WOE%3D";

    final String queryEntity = "%20and%20BBG%3DTs*";

    @Inject
    AuthoritiesDatabase authoritiesDatabase;

    public boolean startMapping(String sruService, String accessToken) {
        if (!authoritiesDatabase.createTable()) {
            return false;
        }
        List<String> metas = new ArrayList<>();
        int pacelliMaxId = authoritiesDatabase.getMaxId();
        HttpClient client = HttpClient.newHttpClient();
        Map<String, Mapping> mappings = new HashMap<>();
        for (int cursor = 0; cursor <= pacelliMaxId; cursor += MAX_STEPS) {
            metas.addAll(authoritiesDatabase.getMeta(cursor, cursor + MAX_STEPS));
        }
        metas.stream()
                .filter(meta -> !meta.trim().contains(" ")
                        && !meta.trim().isEmpty()
                        && !mappings.containsKey(meta))
                .forEach(meta -> {
                    String query = queryIndex + URLEncoder.encode(meta, StandardCharsets.UTF_8)
                            + queryEntity;
                    String url = String.format(sruService, query) + "&accessToken="
                            + accessToken;
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();
                    try {
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        Mapping mapping = getMapping(meta, response.body());
                        mappings.put(meta, mapping);
                    } catch (IOException | InterruptedException e) {
                    }
                });
        authoritiesDatabase.insertMappingData(mappings);
        return true;
    }

    private Mapping getMapping(String meta, String xmlResponse) {
        Mapping mapping = new Mapping();
        mapping.setMeta(meta);
        try {
            Document sruDocument = builder.build(xmlResponse, null);
            XPathContext marc21Context = new XPathContext("marc21", marcNamespace);
            Nodes records = sruDocument.query("//marc21:record", marc21Context);
            for (int i = 0; i < records.size(); i++) {
                Element record = (Element) records.get(i);
                Nodes nodes150 = record.query("marc21:datafield[@tag = \"150\"]" +
                                "/marc21:subfield[@code = \"a\" and text() = \"" + meta + "\"]",
                        marc21Context);
                if (nodes150.size() == 1) {
                    Element node150 = (Element) nodes150.get(0);
                    if (((Element) node150.getParent())
                            .getChildElements("subfield", marcNamespace).size() == 1) {
                        mapping.setAuthorityData(node150.getValue());
                        String gndId = findGndId(record);
                        if (gndId != null) {
                            mapping.setGndId(gndId);
                        }
                        mapping.setIds(findIds(record));
                        mapping.getSynonyms()
                                .addAll(findSynonyms(record));
                        mapping.setType(findEntityType(record));
                        return mapping;
                    }
                } else {
                    Nodes nodes450 = record.query("marc21:datafield[@tag = \"450\"]" +
                                    "/marc21:subfield[@code = \"a\" and text() = \"" + meta + "\"]",
                            marc21Context);
                    if (nodes450.size() > 0) {
                        nodes150 = record.query("marc21:datafield[@tag = \"150\"]" +
                                        "/marc21:subfield[@code = \"a\"]",
                                marc21Context);
                        Element node150 = (Element) nodes150.get(0);
                        if (((Element) node150.getParent())
                                .getChildElements("subfield", marcNamespace).size() == 1) {
                            mapping.setAuthorityData(node150.getValue());
                            String gndId = findGndId(record);
                            if (gndId != null) {
                                mapping.setGndId(gndId);
                            }
                            mapping.setIds(findIds(record));
                            mapping.getSynonyms()
                                    .addAll(findSynonyms(record));
                            mapping.setType(findEntityType(record));
                            return mapping;
                        }
                    }
                }
            }
        } catch (ParsingException | IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private String findGndId(Element record) {
        XPathContext marc21Context = new XPathContext("marc21", marcNamespace);
        Nodes nodes024 = record.query("marc21:datafield[@tag = \"024\" " +
                "and @ind1 = \"7\"]", marc21Context);
        for (int i = 0; i < nodes024.size(); i++) {
            Element node024 = (Element) nodes024.get(i);
            Nodes code2 = node024.query("marc21:subfield[@code = \"2\"]",
                    marc21Context);
            if (code2.size() > 0 && code2.get(0).getValue().equals("uri")) {
                Nodes codea = node024.query("marc21:subfield[@code = \"a\"]",
                        marc21Context);
                return codea.get(0).getValue();
            }
        }
        return null;
    }

    private String[] findIds(Element record) {
        String[] ids = new String[2];
        XPathContext marc21Context = new XPathContext("marc21", marcNamespace);
        Nodes nodes035 = record.query("marc21:datafield[@tag = \"035\"]", marc21Context);
        for (int i = 0; i < nodes035.size(); i++) {
            Element node035 = (Element) nodes035.get(i);
            Nodes codea = node035.query("marc21:subfield[@code = \"a\"]",
                    marc21Context);
            if (codea.size() > 0) {
                if (codea.get(0).getValue().startsWith("(DE-101)")) {
                    ids[0] = codea.get(0).getValue();
                }
                if (codea.get(0).getValue().startsWith("(DE-588)")) {
                    ids[1] = codea.get(0).getValue();
                }
            }
        }
        return ids;
    }

    private String findEntityType(Element record) {
        XPathContext marc21Context = new XPathContext("marc21", marcNamespace);
        Nodes nodes075b = record.query("marc21:datafield[@tag = \"075\"]" +
                "/marc21:subfield[@code = \"b\"]", marc21Context);
        for (int i = 0; i < nodes075b.size(); i++) {
            String node075b = nodes075b.get(i)
                    .getValue();
            if (node075b.length() == 3) {
                return node075b;
            }
        }
        return null;
    }

    private List<String> findSynonyms(Element record) {
        List<String> synonyms = new ArrayList<>();
        XPathContext marc21Context = new XPathContext("marc21", marcNamespace);
        Nodes nodes450 = record.query("marc21:datafield[@tag = \"450\"]" +
                        "/marc21:subfield[@code = \"a\"]",
                marc21Context);
        for (int j = 0; j < nodes450.size(); j++) {
            String synonym = nodes450.get(j)
                    .getValue();
            synonyms.add(synonym);
        }
        return synonyms;
    }
}
