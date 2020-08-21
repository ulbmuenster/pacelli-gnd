/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

import nu.xom.Attribute;
import nu.xom.Element;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarcDataConverter {

    private static final String MARC_NAMESPACE = "http://www.loc.gov/MARC21/slim";

    public static Element generateMarcXmlAuthority(MarcData marcData) {
        Element record = new Element("marcxml:record", MARC_NAMESPACE);
        record.addAttribute(new Attribute("type", "Authority"));
        Element leader = new Element("marcxml:leader", MARC_NAMESPACE);
        leader.appendChild(marcData.getLeader());
        record.appendChild(leader);
        for (String key : marcData.getControlFields().keySet()) {
            Element controlField = new Element("marcxml:controlfield", MARC_NAMESPACE);
            controlField.addAttribute(new Attribute("tag", key));
            controlField.appendChild(marcData.getControlFields().get(key));
            record.appendChild(controlField);
        }
        for (String key : marcData.getDataFields().keySet()) {
            for (DataField value : marcData.getDataFields().get(key)) {
                Element dataField = new Element("marcxml:datafield", MARC_NAMESPACE);
                dataField.addAttribute(new Attribute("tag", key));
                dataField.addAttribute(new Attribute("ind1", value.getFirstIndicator()));
                dataField.addAttribute(new Attribute("ind2", value.getSecondIndicator()));
                for (String code : value.getSubFields().keySet()) {
                    for (SubField subFieldValue : value.getSubFields().get(code)) {
                        Element subField = new Element("marcxml:subfield", MARC_NAMESPACE);
                        subField.addAttribute(new Attribute("code", code));
                        subField.appendChild(Normalizer.normalize(subFieldValue.getValue(), Normalizer.Form.NFD));
                        dataField.appendChild(subField);
                    }
                }
                record.appendChild(dataField);
            }
        }
        return record;
    }

    public static MarcData getMarcData(Element recordElement) {
        MarcData marcData = new MarcData(recordElement.getFirstChildElement(
                "leader", MARC_NAMESPACE).getValue(),
                recordElement.getAttributeValue("type"));
        for (int i = 0; i < recordElement.getChildElements(
                "controlfield", MARC_NAMESPACE).size(); i++) {
            Element controlFieldElement = recordElement.getChildElements(
                    "controlfield", MARC_NAMESPACE)
                    .get(i);
            marcData.getControlFields().put(controlFieldElement.getAttributeValue(
                    "tag"), controlFieldElement.getValue());
        }
        for (int i = 0; i < recordElement.getChildElements(
                "datafield", MARC_NAMESPACE).size(); i++) {
            Element dataFieldElement = recordElement.getChildElements(
                    "datafield", MARC_NAMESPACE)
                    .get(i);
            String tag = dataFieldElement.getAttributeValue("tag");
            if (tag.equals("548")) {
                boolean dontAdd = false;
                for (int j = 0; j < dataFieldElement.getChildElements(
                        "subfield", MARC_NAMESPACE).size(); j++) {
                    Element subFieldElement = dataFieldElement.getChildElements(
                            "subfield", MARC_NAMESPACE)
                            .get(j);
                    if (subFieldElement.getAttributeValue("code").equals("a")
                            && subFieldElement.getValue().trim().equals("-")) {
                        dontAdd = true;
                        break;
                    }
                }
                if (dontAdd) {
                    continue;
                }
            }
            DataField dataField = new DataField(
                    tag,
                    dataFieldElement.getAttributeValue("ind1"),
                    dataFieldElement.getAttributeValue("ind2"));
            if (!marcData.getDataFields().containsKey(tag)) {
                List<DataField> dataFields = new ArrayList<>();
                marcData.getDataFields().put(tag, dataFields);
            }
            marcData.getDataFields()
                    .get(tag)
                    .add(dataField);
            Map<String, List<SubField>> subFields = new LinkedHashMap<>();
            for (int j = 0; j < dataFieldElement.getChildElements(
                    "subfield", MARC_NAMESPACE).size(); j++) {
                Element subFieldElement = dataFieldElement.getChildElements(
                        "subfield", MARC_NAMESPACE)
                        .get(j);
                if (tag.equals("100")
                        && subFieldElement.getAttributeValue("code").equals("d")
                        && subFieldElement.getValue().trim().equals("-")) {
                    continue;
                }
                if (!subFields.containsKey(
                        subFieldElement.getAttributeValue("code"))) {
                    List<SubField> values = new ArrayList<>();
                    subFields.put(subFieldElement.getAttributeValue(
                            "code"), values);
                }
                subFields.get(subFieldElement.getAttributeValue("code"))
                        .add(new SubField(subFieldElement.getValue(), true));
            }
            dataField.setSubFields(subFields);
        }
        return marcData;
    }
}
