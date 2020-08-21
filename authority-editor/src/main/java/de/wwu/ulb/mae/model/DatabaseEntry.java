/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.model;

public class DatabaseEntry {

    private Integer id;

    private String firstName;

    private String lastName;

    private String gndId;

    private String pnGndId;

    private String gndLevel;

    private String notes;

    public DatabaseEntry() {
    }

    public DatabaseEntry(Integer id, String firstName, String lastName,
                         String gndId, String gndLevel, String notes) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gndId = gndId;
        this.gndLevel = gndLevel;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGndId() {
        return gndId;
    }

    public void setGndId(String gndId) {
        this.gndId = gndId;
    }

    public String getPnGndId() {
        return pnGndId;
    }

    public void setPnGndId(String pnGndId) {
        this.pnGndId = pnGndId;
    }

    public String getGndLevel() {
        return gndLevel;
    }

    public void setGndLevel(String gndLevel) {
        this.gndLevel = gndLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
