/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.model;

import de.wwu.ulb.mae.Database;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LazyDatabaseEntryModel extends LazyDataModel<DatabaseEntry> {

    private Database database;

    private boolean edited;

    private List<DatabaseEntry> databaseEntries;

    public LazyDatabaseEntryModel(Database database, boolean edited) {
        this.database = database;
        this.edited = edited;
    }

    @Override
    public DatabaseEntry getRowData(String rowKey) {
        Integer id = Integer.decode(rowKey);
        for (DatabaseEntry databaseEntry : databaseEntries) {
            if (databaseEntry.getId().equals(id)) {
                return databaseEntry;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(DatabaseEntry databaseEntry) {
        return databaseEntry.getId();
    }

    @Override
    public List<DatabaseEntry> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                                    Map<String, FilterMeta> filters) {
        Map<String, String> filterMap = createFilterMap(filters);
        String sortField = null;
        String sortOrder = null;
        if (sortBy != null && !sortBy.isEmpty()) {
            SortMeta sortMeta = sortBy.values()
                    .iterator()
                    .next();
            sortField = sortMeta.getSortField();
            sortOrder = sortMeta.getSortOrder()
                    .equals(SortOrder.ASCENDING) ? "ASC" : "DESC";
        }
        databaseEntries = database.findDatabaseEntries(pageSize, first,
                sortField, sortOrder, edited, filterMap);
        setRowCount(database.findDatabaseSize(edited));
        return databaseEntries;
    }

    public Map<String, String> createFilterMap(Map<String, FilterMeta> filters) {
        Map<String, String> filterMap = new HashMap<>();
        for (String key : filters.keySet()) {
            filterMap.put(filters.get(key).getColumnKey(), (String) filters.get(key).getFilterValue());
        }
        return filterMap;
    }
}
