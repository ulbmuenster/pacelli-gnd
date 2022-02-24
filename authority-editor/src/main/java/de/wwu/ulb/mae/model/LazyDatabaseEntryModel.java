/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.model;

import de.wwu.ulb.mae.Database;
import org.jboss.logging.Logger;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LazyDatabaseEntryModel extends LazyDataModel<DatabaseEntry> {

    private static final Logger LOG = Logger.getLogger(LazyDatabaseEntryModel.class.getName());

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
    public String getRowKey(DatabaseEntry databaseEntry) {
        return databaseEntry.getId().toString();
    }

    @Override
    public List<DatabaseEntry> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                                    Map<String, FilterMeta> filters) {
        LOG.info("Entered first load implementation");
        Map<String, String> filterMap = createFilterMap(filters);
        String sortField = null;
        String sortOrder = null;
        if (sortBy != null && !sortBy.isEmpty()) {
            SortMeta sortMeta = sortBy.values()
                    .iterator()
                    .next();
            sortField = sortMeta.getField();
            sortOrder = sortMeta.getOrder()
                    .equals(SortOrder.ASCENDING) ? "ASC" : "DESC";
        }
        databaseEntries = database.findDatabaseEntries(pageSize, first,
                sortField, sortOrder, edited, filterMap);
        setRowCount(database.findDatabaseSize(edited));
        return databaseEntries;
    }

    public int count(Map<String, FilterMeta> filterBy) {
        return 0;
    }

    public List<DatabaseEntry> load(int first, int pageSize, String sortField, SortOrder sortOrder,
                                    Map<String,FilterMeta> filters) {
        LOG.info("Entered alternative load implementation");
        Map<String, String> filterMap = createFilterMap(filters);
        String sortOrderString = null;
        if (sortOrder != null) {
            sortOrderString = sortOrder.equals(SortOrder.ASCENDING) ? "ASC" : "DESC";
        }
        LOG.info("start searching...");
        databaseEntries = database.findDatabaseEntries(pageSize, first,
                sortField, sortOrderString, edited, filterMap);
        setRowCount(database.findDatabaseSize(edited));
        return databaseEntries;
    }

    public Map<String, String> createFilterMap(Map<String, FilterMeta> filters) {
        LOG.info("Entered createFilterMap");
        Map<String, String> filterMap = new HashMap<>();
        if (filters != null) {
            for (String key : filters.keySet()) {
                if (filters.get(key).getFilterValue() != null) {
                    filterMap.put(filters.get(key).getField(), (String) filters.get(key).getFilterValue());
                }
            }
        }
        return filterMap;
    }
}
