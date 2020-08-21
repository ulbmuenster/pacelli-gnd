/*
 * This file is part of transform-pacelli-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform.marc;

import de.wwu.ulb.transform.model.DateType;
import de.wwu.ulb.transform.model.MetaMapping;
import de.wwu.ulb.transform.model.AuthoritiesData;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParsingException;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Named
@ApplicationScoped
public class Marc21Builder {

    final String templateFile = "/marc21Template.xml";

    static final Logger logger = Logger.getLogger(Marc21Builder.class.getName());

    Builder builder = new Builder();

    final String marcNamespace = "http://www.loc.gov/MARC21/slim";

    final String gndPrefix = "http://d-nb.info/gnd/";

    final String viafPrefix = "http://viaf.org/viaf/";

    String template;

    @PostConstruct
    public void readTemplate() {
        BufferedReader bufferedReader = null;
        URL templateURL = getClass().getResource(templateFile);
        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader((InputStream) templateURL.getContent()));
            StringBuilder templateBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                templateBuilder.append(line);
            }
            template = templateBuilder.toString();
        } catch (IOException e) {
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    public Document buildMarc21(AuthoritiesData authoritiesData) {
        Document marc21 = null;
        try {
            marc21 = builder.build(template, null);
            Element recordElement = marc21.getRootElement();
            if (authoritiesData.getGndId() != null) {
                String gndId = gndPrefix + authoritiesData.getGndId();
                Element dataField024 = buildIdentifierElement(gndId);
                int position = findNextPosition(24, recordElement);
                recordElement.insertChild(dataField024, position);
                Element dataField035 = buildSecondIdentifierElement(authoritiesData.getGndId());
                position = findNextPosition(35, recordElement);
                recordElement.insertChild(dataField035, position);
            }
            if (authoritiesData.getViafId() != null) {
                String viafId = viafPrefix + authoritiesData.getViafId();
                Element dataField024 = buildIdentifierElement(viafId);
                int position = findNextPosition(24, recordElement);
                recordElement.insertChild(dataField024, position);
            }
            DateType from = analyseDate(authoritiesData.getDateOfBirth());
            DateType to = analyseDate(authoritiesData.getDateOfDeath());
            String lifetimeType = "https://d-nb.info/standards/elementset/gnd#dateOfBirthAndDeath";
            if (from.getDate().isEmpty()) {
                lifetimeType = "https://d-nb.info/standards/elementset/gnd#dateOfDeath";
            }
            if (to.getDate().isEmpty()) {
                lifetimeType = "https://d-nb.info/standards/elementset/gnd#dateOfBirth";
            }
            if (from.isExact() || to.isExact()) {
                Element dataField548 = buildDateElement(
                        from.getExactDate() != null ? from.getExactDate() : from.getDate(),
                        to.getExactDate() != null ? to.getExactDate() : to.getDate(),
                        "datx", lifetimeType);
                int position = findNextPosition(548, recordElement);
                recordElement.insertChild(dataField548, position);
            }
            Element dataField548 = buildDateElement(from.getDate(), to.getDate(),
                    "datl", lifetimeType);
            int position = findNextPosition(548, recordElement);
            recordElement.insertChild(dataField548, position);
            Element dataField100 = buildNameElement(authoritiesData.getLastName(), authoritiesData.getFirstName(),
                    from.getDate(), to.getDate());
            position = findNextPosition(100, recordElement);
            recordElement.insertChild(dataField100, position);
            boolean first = true;
            for (MetaMapping metaMapping : authoritiesData.getMetaMappings()) {
                Element dataField550 = buildOccupationElement(metaMapping, first);
                position = findNextPosition(550, recordElement);
                recordElement.insertChild(dataField550, position);
                first = false;
            }
            if (authoritiesData.getDetails() != null) {
                Element dataField670 = buildSourceElement(authoritiesData.getAuthoritiesId());
                position = findNextPosition(670, recordElement);
                recordElement.insertChild(dataField670, position);
                Element dataField678 = buildBiographicalElement(
                        authoritiesData.getAuthoritiesId(), authoritiesData.getDetails());
                position = findNextPosition(678, recordElement);
                recordElement.insertChild(dataField678, position);
            }
        } catch (ParsingException | IOException e) {
            logger.error(e.getMessage());
        }
        return marc21;
    }

    private DateType analyseDate(String date) {
        DateType dateType = new DateType();
        if (date.equals("n.e.")) {
            dateType.setDate("");
        } else if (date.length() >= 10) {
            dateType.setDate(date.substring(0, 4));
            dateType.setExactDate(date.substring(8, 10) + "." + date.substring(5, 7) +
                    "." + date.substring(0, 4));
        } else if (date.length() == 4) {
            dateType.setDate(date);
        } else {
            dateType.setDate("");
        }
        return dateType;
    }

    private int findNextPosition(int tag, Element parentElement) {
        int position = parentElement.getChildCount();
        for (int i = 0; i < parentElement.getChildCount(); i++) {
            Node node = parentElement.getChild(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                String childTag = element.getAttributeValue("tag");
                if (childTag != null && Integer.parseInt(childTag) > tag) {
                    position = i;
                    break;
                }
            }
        }
        return position;
    }

    private Element buildSourceElement(Integer id) {
        Element dataField670 = new Element("datafield", marcNamespace);
        dataField670.addAttribute(new Attribute("tag", "670"));
        dataField670.addAttribute(new Attribute("ind1", " "));
        dataField670.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "a"));
        subField.appendChild("Homepage");
        dataField670.appendChild(subField);
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = today.format(formatter);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "b"));
        subField.appendChild("Stand: " + date);
        dataField670.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "u"));
        subField.appendChild("http://www.pacelli-edition.de/Biographie/" + id.toString());
        dataField670.appendChild(subField);
        return dataField670;
    }

    private Element buildBiographicalElement(Integer id, String details) {
        Element dataField678 = new Element("datafield", marcNamespace);
        dataField678.addAttribute(new Attribute("tag", "678"));
        dataField678.addAttribute(new Attribute("ind1", " "));
        dataField678.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "b"));
        subField.appendChild(details);
        dataField678.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "u"));
        subField.appendChild("http://www.pacelli-edition.de/Biographie/" + id.toString());
        dataField678.appendChild(subField);
        return dataField678;
    }

    private Element buildDateElement(String from, String to, String typeOfDate, String lifetimeType) {
        Element dataField548 = new Element("datafield", marcNamespace);
        dataField548.addAttribute(new Attribute("tag", "548"));
        dataField548.addAttribute(new Attribute("ind1", " "));
        dataField548.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "a"));
        StringBuilder codeABuilder = new StringBuilder();
        if (typeOfDate.equals("datx")) {
            if (from.length() == 0 || from.length() == 10) {
                codeABuilder.append(from)
                        .append("-");
            } else {
                codeABuilder.append("XX.XX.")
                        .append(from)
                        .append("-");
            }
            if (to.length() == 0 || to.length() == 10) {
                codeABuilder.append(to);
            } else {
                codeABuilder.append("XX.XX.")
                        .append(to);
            }
        } else {
            codeABuilder.append(from)
                    .append("-")
                    .append(to);
        }
        subField.appendChild(codeABuilder.toString());
        dataField548.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "4"));
        subField.appendChild(typeOfDate);
        dataField548.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "4"));
        subField.appendChild(lifetimeType);
        dataField548.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "w"));
        subField.appendChild("r");
        dataField548.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "i"));
        if (typeOfDate.equals("datx")) {
            subField.appendChild("Exakte Lebensdaten");
        } else {
            subField.appendChild("Lebensdaten");
        }
        dataField548.appendChild(subField);
        return dataField548;
    }

    private Element buildNameElement(String lastName, String firstName, String from, String to) {
        Element dataField100 = new Element("datafield", marcNamespace);
        dataField100.addAttribute(new Attribute("tag", "100"));
        dataField100.addAttribute(new Attribute("ind1",
                firstName != null && lastName != null ? "1" : "0"));
        dataField100.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "a"));
        if (lastName != null && firstName != null) {
            subField.appendChild(lastName + ", " + firstName);
        } else if (firstName != null) {
            subField.appendChild(firstName);
        } else {
            subField.appendChild(lastName);
        }
        dataField100.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "d"));
        subField.appendChild(from + "-" + to);
        dataField100.appendChild(subField);
        return dataField100;
    }

    private Element buildOccupationElement(MetaMapping metaMapping, boolean first) {
        Element dataField550 = new Element("datafield", marcNamespace);
        dataField550.addAttribute(new Attribute("tag", "550"));
        dataField550.addAttribute(new Attribute("ind1", " "));
        dataField550.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "0"));
        subField.appendChild(metaMapping.getDe101id());
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "0"));
        subField.appendChild(metaMapping.getDe588id());
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "0"));
        subField.appendChild(metaMapping.getGndId());
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "a"));
        subField.appendChild(metaMapping.getMapping());
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "4"));
        subField.appendChild(first ? "berc" : "beru");
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "4"));
        subField.appendChild("https://d-nb.info/standards/elementset/gnd#professionOrOccupation");
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "w"));
        subField.appendChild("r");
        dataField550.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "i"));
        subField.appendChild(first ? "Charakteristischer Beruf" : "Beruf");
        dataField550.appendChild(subField);
        return dataField550;
    }

    private Element buildIdentifierElement(String id) {
        Element dataField024 = new Element("datafield", marcNamespace);
        dataField024.addAttribute(new Attribute("tag", "024"));
        dataField024.addAttribute(new Attribute("ind1", "7"));
        dataField024.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "a"));
        subField.appendChild(id);
        dataField024.appendChild(subField);
        subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "2"));
        subField.appendChild("uri");
        dataField024.appendChild(subField);
        return dataField024;
    }

    private Element buildSecondIdentifierElement(String id) {
        Element dataField035 = new Element("datafield", marcNamespace);
        dataField035.addAttribute(new Attribute("tag", "035"));
        dataField035.addAttribute(new Attribute("ind1", " "));
        dataField035.addAttribute(new Attribute("ind2", " "));
        Element subField = new Element("subfield", marcNamespace);
        subField.addAttribute(new Attribute("code", "a"));
        subField.appendChild("(DE-588)" + id);
        dataField035.appendChild(subField);
        return dataField035;
    }
}
