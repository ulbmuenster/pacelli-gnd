/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.marc;

public class SubField {

    private String value;

    private boolean changed;

    public SubField() {
    }

    public SubField(String value, boolean changed) {
        this.value = value;
        this.changed = changed;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SubField) {
            return this.value.
                    equals(((SubField) other).getValue());
        }
        return false;
    }
}
