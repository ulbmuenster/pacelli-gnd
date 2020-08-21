/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DataField {

    private static final Logger LOG = Logger.getLogger(DataField.class.getName());

    private String tag;

    private String firstIndicator;

    private String secondIndicator;

    private Map<String, List<SubField>> subFields;

    private boolean doubledType;

    public DataField() {
        this.tag = "001";
        this.firstIndicator = " ";
        this.secondIndicator = " ";
        this.doubledType = false;
    }

    public DataField(String tag) {
        this.tag = tag;
        this.firstIndicator = " ";
        this.secondIndicator = " ";
        this.doubledType = false;
    }

    public DataField(String tag, String firstIndicator, String secondIndicator) {
        this.tag = tag;
        this.firstIndicator = firstIndicator;
        this.secondIndicator = secondIndicator;
        this.doubledType = false;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getFirstIndicator() {
        return firstIndicator;
    }

    public String getSecondIndicator() {
        return secondIndicator;
    }

    public Map<String, List<SubField>> getSubFields() {
        return subFields;
    }

    public void setSubFields(Map<String, List<SubField>> subFields) {
        this.subFields = subFields;
    }

    public int getSubFieldsSize() {
        AtomicInteger size = new AtomicInteger(0);
        subFields.values()
                .forEach(value -> {
                    size.addAndGet(value.size());
                });
        return size.get();
    }

    public boolean isDoubledType() {
        return doubledType;
    }

    public void setDoubledType(boolean doubledType) {
        this.doubledType = doubledType;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DataField) {
            DataField dataField = (DataField) object;
            AtomicBoolean equal = new AtomicBoolean(true);
            switch (tag) {
                case "024":
                case "035":
                case "042":
                case "043":
                    subFields.keySet()
                            .forEach(key -> {
                                if (!dataField.getSubFields().containsKey(key)) {
                                    equal.set(false);
                                }
                            });
                    break;
                case "075":
                    subFields.keySet()
                            .stream()
                            .filter(key -> key.equals('2'))
                            .forEach(key -> {
                                if (!dataField.getSubFields().containsKey(key)) {
                                    equal.set(false);
                                    return;
                                }
                                if (dataField.getSubFields().get(key).get(0).equals(subFields.get(key).get(0))) {
                                    return;
                                }
                                equal.set(false);
                            });
                    break;
                case "040":
                case "079":
                    break;
                case "100":
                    subFields.keySet()
                            .stream()
                            .filter(key -> key.equals('a'))
                            .forEach(key -> {
                                if (!dataField.getSubFields().containsKey(key)) {
                                    equal.set(false);
                                    return;
                                }
                            });
                    break;
                case "548":
                    LOG.debug("Vergleiche 548");
                    String thisDate = subFields.get("a")
                            .get(0)
                            .getValue();
                    LOG.debug("thisDate: " + thisDate);
                    if (dataField.getSubFields().get("a") == null
                            || dataField.getSubFields().get("a").isEmpty()) {
                        equal.set(false);
                        break;
                    }
                    String otherDate = dataField.getSubFields()
                            .get("a")
                            .get(0)
                            .getValue();
                    LOG.debug("otherDate: " + otherDate);
                    List<SubField> thisTypes = subFields.get("4");
                    LOG.debug("thisTypes: " + thisTypes.size());
                    List<SubField> otherTypes = dataField.getSubFields()
                            .get("4");
                    LOG.debug("otherTypes: " + otherTypes.size());
                    if (!thisDate.equals(otherDate) ||
                            thisTypes.size() != otherTypes.size() ||
                            !thisTypes.containsAll(otherTypes)) {
                        equal.set(false);
                    }
                    break;
                case "550":
                    List<SubField> code4s = subFields.get("4");
                    List<SubField> otherCode4s = dataField.getSubFields()
                            .get("4");
                    for (SubField code4 : code4s) {
                        if (code4.getValue().equals("berc")) {
                            for (SubField otherCode4 : otherCode4s) {
                                if (otherCode4.getValue().equals("berc")) {
                                    doubledType = true;
                                    dataField.setDoubledType(true);
                                }
                            }
                        }
                    }
                    if (doubledType) {
                        break;
                    }
                    List<SubField> thisIds = subFields.get("0");
                    List<SubField> otherIds = dataField.getSubFields()
                            .get("0");
                    if (thisIds != null && otherIds != null &&
                            (thisIds.size() != otherIds.size() ||
                            !thisIds.containsAll(otherIds))) {
                        equal.set(false);
                    }
                    break;
                default:
                    return false;
            }
            return equal.get();
        }
        return false;
    }
}
