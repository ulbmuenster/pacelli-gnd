/*
 * This file is part of prepare-meta-mapping.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * prepare-meta-mapping is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mapping;

import de.wwu.ulb.mapping.model.Mapping;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AuthoritiesDatabase {

    private static final Logger LOG = Logger.getLogger(AuthoritiesDatabase.class.getName());

    @Inject
    DataSource dataSource;

    final String selectMax = "SELECT max(authoritiesid) from authorities";

    final String selectMeta = "SELECT meta FROM authorities WHERE authoritiesid > ? " +
            "AND authoritiesid <= ? ORDER BY authoritiesid";

    final String insertMapping = "INSERT INTO metamapping (meta, mapping, gndid, type, synonyms, " +
            "de101id, de588id) VALUES (?, ?, ?, ?, ?, ?, ?)";

    final String createMetaMapping = "CREATE TABLE metamapping\n" +
            "(\n" +
            "    meta character varying(500) NOT NULL,\n" +
            "    mapping character varying(500),\n" +
            "    gndid character varying(50),\n" +
            "    type character varying(3),\n" +
            "    synonyms character varying(500)[],\n" +
            "    de101id character varying(50),\n" +
            "    de588id character varying(50),\n" +
            "    CONSTRAINT metamapping_pk PRIMARY KEY (meta)\n" +
            ")\n";

    public boolean createTable() {
        Connection connection = null;
        Statement statement = null;
        boolean result = false;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.createStatement();
                result = statement.execute(createMetaMapping);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return result;
    }

    public int getMaxId() {
        Connection connection = null;
        Statement statement = null;
        int maxId = 0;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(selectMax);
                if (resultSet.next()) {
                    maxId = resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return maxId;
    }

    public List<String> getMeta(int cursor, int steps) {
        List<String> metas = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectMeta);
                statement.setInt(1, cursor);
                statement.setInt(2, cursor + steps);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    Array array = resultSet.getArray(1);
                    String[] metaArray = (String[]) array.getArray();
                    Arrays.stream(metaArray)
                            .forEach(meta -> metas.add(meta));
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return metas;
    }

    public void insertMappingData(Map<String, Mapping> mappings) {
        List<String> metas = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                for (String key : mappings.keySet()) {
                    if (mappings.get(key) != null) {
                        statement = connection.prepareStatement(insertMapping);
                        statement.setString(1, key);
                        statement.setString(2, mappings.get(key).getAuthorityData());
                        statement.setString(3, mappings.get(key).getGndId());
                        statement.setString(4, mappings.get(key).getType());
                        statement.setObject(5, mappings.get(key).getSynonyms()
                                .toArray(new String[mappings.get(key).getSynonyms().size()]));
                        String[] ids = mappings.get(key)
                                .getIds();
                        if (ids[0] != null) {
                            statement.setString(6, ids[0]);
                        } else {
                            statement.setNull(6, Types.VARCHAR);
                        }
                        if (ids[1] != null) {
                            statement.setString(7, ids[1]);
                        } else {
                            statement.setNull(7, Types.VARCHAR);
                        }
                        statement.execute();
                    }
                }
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
