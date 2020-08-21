/*
 * This file is part of transform-pacelli-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform.model;

import java.util.List;

public class AuthoritiesData {

    private Integer authoritiesId;

    private String gndId;

    private String viafId;

    private String dateOfBirth;

    private String dateOfDeath;

    private List<MetaMapping> metaMappings;

    private String firstName;

    private String lastName;

    private String details;

    public AuthoritiesData() {
    }

    public Integer getAuthoritiesId() {
        return authoritiesId;
    }

    public void setAuthoritiesId(Integer authoritiesId) {
        this.authoritiesId = authoritiesId;
    }

    public String getGndId() {
        return gndId;
    }

    public void setGndId(String gndId) {
        this.gndId = gndId;
    }

    public String getViafId() {
        return viafId;
    }

    public void setViafId(String viafId) {
        this.viafId = viafId;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(String dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public List<MetaMapping> getMetaMappings() {
        return metaMappings;
    }

    public void setMetaMappings(List<MetaMapping> metaMappings) {
        this.metaMappings = metaMappings;
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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String toString() {
        return "Id: " + authoritiesId + "; dateOfBirth: " + dateOfBirth + "; dateOfDeath: " + dateOfDeath;
    }
}
