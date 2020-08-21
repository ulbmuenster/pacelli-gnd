/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

import java.util.List;
import java.util.Map;
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
            controlFields = new TreeMap<>();
        }
        return controlFields;
    }

    public void setControlFields(Map<String, String> controlFields) {
        this.controlFields = new TreeMap<>();
        this.controlFields.putAll(controlFields);
    }

    public TreeMap<String, List<DataField>> getDataFields() {
        if (dataFields == null) {
            dataFields = new TreeMap<>();
        }
        return dataFields;
    }

    public void setDataFields(Map<String, List<DataField>> dataFields) {
        this.dataFields = new TreeMap<>();
        this.dataFields.putAll(dataFields);
    }

}
