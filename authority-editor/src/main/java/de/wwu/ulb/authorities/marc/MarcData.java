/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class MarcData {

    public String leader;

    public String type;

    public TreeMap<String, String> controlFields;

    public TreeMap<String, List<DataField>> dataFields;

    public MarcData() {
        this.leader = "00000nz  a2200000oc 4500";
        this.type = "Authority";
    }

    public MarcData(String leader, String type) {
        this.leader = leader;
        this.type = type;
    }

    public String getLeader() {
        return leader;
    }

    public String getType() {
        return type;
    }

    public TreeMap<String, String> getControlFields() {
        if (controlFields == null) {
            controlFields = new TreeMap<>(new MarcTagComparator());
        }
        return controlFields;
    }

    public void setControlFields(Map<String, String> controlFields) {
        this.controlFields = new TreeMap<>(new MarcTagComparator());
        this.controlFields.putAll(controlFields);
    }

    public TreeMap<String, List<DataField>> getDataFields() {
        if (dataFields == null) {
            dataFields = new TreeMap<>(new MarcTagComparator());
        }
        return dataFields;
    }

    public void setDataFields(Map<String, List<DataField>> dataFields) {
        this.dataFields = new TreeMap<>(new MarcTagComparator());
        this.dataFields.putAll(dataFields);
    }

    public void addPacelliSource(Integer id) {
        List<DataField> sourceFields = dataFields.containsKey("670")
                ? dataFields.get("670")
                : new ArrayList<>();
        DataField sourceField = new DataField("670");
        Map<String, List<SubField>> subFields = new HashMap<>();
        subFields.put("a", List.of(new SubField("Homepage", true)));
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = today.format(formatter);
        subFields.put("b", List.of(new SubField("Stand: " + date, true)));
        subFields.put("u", List.of(
                new SubField("http://www.pacelli-edition.de/Biographie/" + id.toString(), true)));
        sourceField.setSubFields(subFields);
        sourceFields.add(sourceField);
        dataFields.put("670", sourceFields);
    }

    public String getGndId() {
        if (type.equals("Authority")) {
            return controlFields.get("001");
        }
        return "";
    }

    public String getName() {
        if (type.equals("Authority")) {
            StringBuilder names = new StringBuilder();
            List<DataField> nameFields = dataFields.get("100");
            if (nameFields != null && !nameFields.isEmpty()) {
                DataField nameField = nameFields.get(0);
                List<SubField> nameSubField = nameField.getSubFields()
                        .get("a");
                if (nameSubField != null && !nameSubField.isEmpty()) {
                    names.append(nameSubField.get(0).getValue());
                }
            }
            return names.toString();
        }
        return "Daten sind keine Normdaten!";
    }

    public List<String> getDates() {
        List<String> dates = new ArrayList<>();
        if (type.equals("Authority")) {
            List<DataField> dateDataFields = dataFields.get("548");
            if (dateDataFields != null) {
                for (DataField dateDataField : dateDataFields) {
                    StringBuilder dateFieldBuilder = new StringBuilder();
                    List<SubField> typeSubField = dateDataField.getSubFields()
                            .get("i");
                    if (typeSubField != null && !typeSubField.isEmpty()) {
                        dateFieldBuilder.append(typeSubField.get(0).getValue())
                                .append(": ");
                    }
                    List<SubField> dateSubField = dateDataField.getSubFields()
                            .get("a");
                    if (dateSubField != null && !dateSubField.isEmpty()) {
                        dateFieldBuilder.append(dateSubField.get(0).getValue());
                    }
                    dates.add(dateFieldBuilder.toString());
                }
            }
        }
        return dates;
    }

    public String getBio() {
        if (type.equals("Authority")) {
            StringBuilder bio = new StringBuilder();
            List<DataField> bioFields = dataFields.get("678");
            if (bioFields != null && !bioFields.isEmpty()) {
                DataField bioField = bioFields.get(0);
                List<SubField> bioSubField = bioField.getSubFields()
                        .get("b");
                if (bioSubField != null && !bioSubField.isEmpty()) {
                    bio.append(bioSubField.get(0).getValue());
                }
            }
            return bio.toString();
        }
        return "";
    }

    public List<String> getTitles() {
        List<String> titles = new ArrayList<>();
        if (type.equals("Authority")) {
            List<DataField> titleDataFields = dataFields.get("672");
            if (titleDataFields != null) {
                for (DataField titleDataField : titleDataFields) {
                    StringBuilder titleFieldBuilder = new StringBuilder();
                    List<SubField> titleSubField = titleDataField.getSubFields()
                            .get("a");
                    if (titleSubField != null && !titleSubField.isEmpty()) {
                        titleFieldBuilder.append(titleSubField.get(0).getValue());
                    }
                    titles.add(titleFieldBuilder.toString());
                }
            }
        }
        return titles;
    }

    public Optional<String> getPacelliSource() {
        if (type.equals("Authority")) {
            List<DataField> sourceDataFields = dataFields.get("670");
            if (sourceDataFields != null) {
                for (DataField sourceDataField : sourceDataFields) {
                    List<SubField> sourceSubField = sourceDataField.getSubFields()
                            .get("u");
                    if (sourceSubField != null && !sourceSubField.isEmpty()) {
                        return Optional.ofNullable(sourceSubField.get(0)
                                .getValue()
                                .startsWith("http://www.pacelli-edition.de/Biographie")
                                ? sourceSubField.get(0).getValue()
                                : null);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public String toString() {
        return leader;
    }
}
