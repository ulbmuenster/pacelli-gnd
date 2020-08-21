/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

import nu.xom.Attribute;
import nu.xom.Element;

import javax.enterprise.context.ApplicationScoped;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MarcDataConverter {

    private final String MARC_NAMESPACE_URI = "http://www.loc.gov/MARC21/slim";

    public Element generateMarcXmlAuthority(MarcData marcData) {
        Element record = new Element("marcxml:record", MARC_NAMESPACE_URI);
        record.addAttribute(new Attribute("type", "Authority"));
        Element leader = new Element("marcxml:leader", MARC_NAMESPACE_URI);
        leader.appendChild(marcData.getLeader());
        record.appendChild(leader);
        for (String key : marcData.getControlFields().keySet()) {
            Element controlField = new Element("marcxml:controlfield", MARC_NAMESPACE_URI);
            controlField.addAttribute(new Attribute("tag", key));
            controlField.appendChild(marcData.getControlFields().get(key));
            record.appendChild(controlField);
        }
        for (String key : marcData.getDataFields().keySet()) {
            for (DataField value : marcData.getDataFields().get(key)) {
                Element dataField = new Element("marcxml:datafield", MARC_NAMESPACE_URI);
                dataField.addAttribute(new Attribute("tag", key));
                dataField.addAttribute(new Attribute("ind1", value.getFirstIndicator()));
                dataField.addAttribute(new Attribute("ind2", value.getSecondIndicator()));
                for (String code : value.getSubFields().keySet()) {
                    for (SubField subFieldValue : value.getSubFields().get(code)) {
                        Element subField = new Element("marcxml:subfield", MARC_NAMESPACE_URI);
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

    public MarcData getMarcData(Element recordElement, boolean useMarcNamespace) {
        String namespaceURI = useMarcNamespace ? MARC_NAMESPACE_URI : "";
        MarcData marcData = new MarcData(recordElement.getFirstChildElement("leader", namespaceURI).getValue(),
                recordElement.getAttributeValue("type"));
        for (Element controlFieldElement :
                recordElement.getChildElements("controlfield", namespaceURI)) {
            marcData.getControlFields().put(controlFieldElement.getAttributeValue(
                    "tag"), controlFieldElement.getValue());
        }
        for (Element dataFieldElement :
                recordElement.getChildElements("datafield", namespaceURI)) {
            String tag = dataFieldElement.getAttributeValue("tag");
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
            for (Element subFieldElement : dataFieldElement.getChildElements("subfield", namespaceURI)) {
                if (!subFields.containsKey(subFieldElement.getAttributeValue("code"))) {
                    List<SubField> values = new ArrayList<>();
                    subFields.put(subFieldElement.getAttributeValue("code"), values);
                }
                String value = subFieldElement.getValue();
                subFields.get(subFieldElement.getAttributeValue("code"))
                        .add(new SubField(value));
            }
            dataField.setSubFields(subFields);
        }
        return marcData;
    }
}
