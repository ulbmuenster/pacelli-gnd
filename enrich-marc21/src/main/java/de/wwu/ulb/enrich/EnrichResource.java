/*
 * This file is part of enrich-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * enrich-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.enrich;

import de.wwu.ulb.enrich.model.Authority;
import org.jboss.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class EnrichResource {

    private static final Logger LOG = Logger.getLogger(EnrichResource.class.getName());

    private final String MARC_NAMESPACE = "http://www.loc.gov/MARC21/slim";

    @Inject
    AuthoritiesDatabase authoritiesDatabase;

    public boolean startEnrichment() {
        List<Authority> authorities = authoritiesDatabase.findAll();
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        Namespace marcNamespace = Namespace.getNamespace(MARC_NAMESPACE);
        authorities.stream()
                .filter(authority -> authority.getGndId().matches("\\d.*"))
                .forEach(authority -> {
                    String marc21Url = "https://d-nb.info/gnd/" + authority.getGndId() +
                            "/about/marcxml";
                    try {
                        URL pacelliUrl = new URL(marc21Url);
                        String content = readFromURL(pacelliUrl);
                        Document pacelliDocument = builder.build(new StringReader(content));
                        AtomicReference<String> gndLevel = new AtomicReference<>("");
                        AtomicReference<String> gndId = new AtomicReference<>(authority.getGndId());
                        AtomicReference<String> pnGndId = new AtomicReference<>("");
                        pacelliDocument.getRootElement()
                                .getChildren("datafield", marcNamespace)
                                .stream()
                                .forEach(datafield -> {
                                    switch (datafield.getAttributeValue("tag")) {
                                        case "042":
                                            Element subfield042 = datafield.getChild(
                                                    "subfield", marcNamespace);
                                            gndLevel.set(subfield042.getValue());
                                            break;
                                        case "075":
                                            datafield.getChildren("subfield", marcNamespace)
                                                    .forEach(subfield075 -> {
                                                        if (subfield075.getAttributeValue("code").equals("b")) {
                                                            if (subfield075.getValue().equals("n")) {
                                                                pnGndId.set(authority.getGndId());
                                                                gndId.set("");
                                                            }
                                                        }
                                                    });
                                            break;
                                    }
                                });
                        authoritiesDatabase.updateAuthority(authority.getAuthoritiesId(), gndId.get(),
                                gndLevel.get(), pnGndId.get());
                    } catch (JDOMException | IOException e) {
                        if (e instanceof FileNotFoundException) {
                            authoritiesDatabase.removeGndId(authority.getAuthoritiesId(),
                                    authority.getGndId());
                        } else {
                            LOG.fatal(e.getMessage());
                        }
                    }

                });
        return true;
    }

    private String readFromURL(URL url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder content = new StringBuilder();

        String line;
        while ((line = in.readLine()) != null) {
            content.append(line);
        }
        in.close();
        return content.toString();
    }
}
