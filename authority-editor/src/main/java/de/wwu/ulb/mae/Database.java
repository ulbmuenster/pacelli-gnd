/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae;

import de.wwu.ulb.mae.model.DatabaseEntry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    @Inject
    DataSource dataSource;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.tablename")
    Provider<String> tableName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.firstnamecolumn")
    Provider<String> firstNameColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.lastnamecolumn")
    Provider<String> lastNameColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.marccolumn")
    Provider<String> marcColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.idcolumn")
    Provider<String> idColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.gndidcolumn")
    Provider<String> gndIdColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.gndupdatedcolumn")
    Provider<String> gndUpdatedColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.removedgndidcolumn")
    Provider<String> removedGndIdColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.pngndidcolumn")
    Provider<String> pnGndIdColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.gndlevelcolumn")
    Provider<String> gndLevelColumnName;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.notescolumn")
    Provider<String> notesColumnName;

    private final String selectAll = "SELECT %s, %s, %s, %s, %s, %s, %s FROM %s " +
            "WHERE %s IS NOT NULL AND %s IS NULL %s ORDER BY %s %s LIMIT ? OFFSET ?";

    private final String selectEdited = "SELECT %s, %s, %s, %s, %s, %s, %s FROM %s " +
            "WHERE %s IS NOT NULL AND %s IS NOT NULL %s ORDER BY %s %s LIMIT ? OFFSET ?";

    private final String selectRowCount = "SELECT count(%s) FROM %s WHERE %s IS NOT NULL " +
            "AND %s IS NULL";

    private final String selectEditedRowCount = "SELECT count(%s) FROM %s WHERE %s IS NOT NULL " +
            "AND %s IS NOT NULL";

    private final String selectMarc = "SELECT %s, %s, %s FROM %s WHERE %s = ?";

    private final String updateMarc = "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?";

    private final String removeGndId = "UPDATE %s SET %s = null, %s = ?, %s = ? WHERE %s = ?";

    private final String updateNotes = "UPDATE %s SET %s = ? WHERE %s = ?";

    public List<DatabaseEntry> findDatabaseEntries(Integer limit, Integer offset,
                                                   String sortField, String sortOrder,
                                                   boolean edited,
                                                   Map<String, String> filter) {
        if (edited) {
            return findInDatabase(limit, offset, sortField, sortOrder, selectEdited, filter);
        }
        return findInDatabase(limit, offset, sortField, sortOrder, selectAll, filter);
    }

    private List<DatabaseEntry> findInDatabase(Integer limit, Integer offset,
                                               String sortField, String sortOrder,
                                               String select,
                                               Map<String, String> filter) {
        LOG.info("Parameters: Limit=" + limit + ", Offset=" + offset + ", SortField=" + sortField +
                ", SortOrder=" + sortOrder + ", Select=" + select);
        if (filter != null) {
            filter.keySet()
                    .forEach(key -> LOG.info("Key=" + key + ", Value=" + filter.get(key)));
        }
        if (sortField == null) {
            sortField = "id";
        }
        String order = null;
        switch (sortField) {
            case "id":
                order = idColumnName.get();
                break;
            case "gndId":
                order = gndIdColumnName.get() + ", " + idColumnName.get();
                break;
            case "lastName":
                order = lastNameColumnName.get() + ", " + idColumnName.get();
                break;
            case "gndLevel":
                order = gndLevelColumnName.get() + ", " + idColumnName.get();
        }
        StringBuilder filterValue = new StringBuilder("");
        for (String key : filter.keySet()) {
            filterValue.append("AND lower(");
            switch (key) {
                case "lastName":
                    filterValue.append(lastNameColumnName.get());
                    break;
                case "firstName":
                    filterValue.append(firstNameColumnName.get());
                    break;
                case "notes":
                    filterValue.append(notesColumnName.get());
                    break;
            }
            filterValue.append(") LIKE '%")
                    .append(filter.get(key).toLowerCase())
                    .append("%' ");
        }
        List<DatabaseEntry> databaseEntries = new ArrayList<>();
        String selectStatement = String.format(select, idColumnName.get(),
                gndIdColumnName.get(), firstNameColumnName.get(), lastNameColumnName.get(),
                pnGndIdColumnName.get(), gndLevelColumnName.get(), notesColumnName.get(),
                tableName.get(), marcColumnName.get(), gndUpdatedColumnName.get(),
                filterValue.toString(), order, sortOrder != null ? sortOrder : "");
        LOG.info(selectStatement);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectStatement);
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    DatabaseEntry databaseEntry = new DatabaseEntry();
                    databaseEntry.setId(resultSet.getInt(1));
                    databaseEntry.setGndId(resultSet.getString(2));
                    databaseEntry.setFirstName(resultSet.getString(3));
                    databaseEntry.setLastName(resultSet.getString(4));
                    databaseEntry.setPnGndId(resultSet.getString(5));
                    databaseEntry.setGndLevel(resultSet.getString(6));
                    databaseEntry.setNotes(resultSet.getString(7));
                    databaseEntries.add(databaseEntry);
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return databaseEntries;
    }

    public Integer findDatabaseSize(boolean edited) {
        String selectStatement = String.format(edited ? selectEditedRowCount : selectRowCount,
                idColumnName.get(), tableName.get(), marcColumnName.get(),
                gndUpdatedColumnName.get());
        Connection connection = null;
        PreparedStatement statement = null;
        Integer rowCount = 0;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectStatement);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    rowCount = resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return rowCount;
    }

    public MarcQueryResult findMarcEntry(Integer id) {
        String selectStatement = String.format(selectMarc, marcColumnName.get(),
                firstNameColumnName.get(), lastNameColumnName.get(),
                tableName.get(), idColumnName.get());
        LOG.debug(selectStatement);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectStatement);
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    SQLXML xml = resultSet.getSQLXML(1);
                    String firstName = resultSet.getString(2);
                    String lastName = resultSet.getString(3);
                    LOG.debug(xml.getString());
                    return new MarcQueryResult(xml.getString(), firstName, lastName);
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return null;
    }

    public boolean updateMarcEntry(Integer id, String firstName, String lastName, String gndId,
                                   String xml, String updateDate) {
        String updateStatement = String.format(updateMarc, tableName.get(),
                firstNameColumnName.get(), lastNameColumnName.get(), marcColumnName.get(),
                gndUpdatedColumnName.get(), gndIdColumnName.get(), idColumnName.get());
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(updateStatement);
                SQLXML marc21Xml = connection.createSQLXML();
                marc21Xml.setString(xml);
                if (firstName != null) {
                    statement.setString(1, firstName);
                } else {
                    statement.setNull(1, Types.VARCHAR);
                }
                if (lastName != null) {
                    statement.setString(2, lastName);
                } else {
                    statement.setNull(1, Types.VARCHAR);
                }
                statement.setSQLXML(3, marc21Xml);
                if (updateDate != null) {
                    statement.setString(4, updateDate);
                } else {
                    statement.setNull(4, Types.VARCHAR);
                }
                statement.setString(5, gndId);
                statement.setInt(6, id);
                int result = statement.executeUpdate();
                return result > 0;
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return false;
    }

    public boolean updateNotes(Integer id, String notes) {
        String updateStatement = String.format(updateNotes, tableName.get(),
                notesColumnName.get(), idColumnName.get());
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(updateStatement);
                statement.setString(1, notes);
                statement.setInt(2, id);
                int result = statement.executeUpdate();
                return result > 0;
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return false;
    }

    public void removeGndId(Integer id, String xml, String gndId) {
        String deleteStatement = String.format(removeGndId, tableName.get(),
                gndIdColumnName.get(), marcColumnName.get(),
                removedGndIdColumnName.get(), idColumnName.get());
        LOG.debug(deleteStatement);
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(deleteStatement);
                SQLXML marc21Xml = connection.createSQLXML();
                marc21Xml.setString(xml);
                statement.setSQLXML(1, marc21Xml);
                statement.setString(2, gndId);
                statement.setInt(3, id);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
    }

    private void closeAll(Statement statement, Connection connection) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (Exception e) {
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
        }
    }
}
