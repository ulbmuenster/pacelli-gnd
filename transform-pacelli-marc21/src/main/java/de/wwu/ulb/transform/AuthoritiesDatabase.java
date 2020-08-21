/*
 * This file is part of transform-pacelli-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform;

import de.wwu.ulb.transform.model.MetaMapping;
import de.wwu.ulb.transform.model.AuthoritiesData;
import nu.xom.Document;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AuthoritiesDatabase {

    private static final Logger LOG = Logger.getLogger(AuthoritiesDatabase.class.getName());

    @Inject
    DataSource dataSource;

    private final String selectMax = "SELECT max(authoritiesid) from authorities";

    private final String selectMetaMapping = "SELECT meta, mapping, gndid, de101id, de588id, synonyms " +
            "FROM metamapping WHERE type = 'saz'";

    private final String selectPacelli = "SELECT authoritiesid, gndid, viafid, dateofbirth, " +
            "dateofdeath, meta, firstname, lastname, details FROM authorities ORDER BY authoritiesid " +
            "LIMIT ? OFFSET ?";

    private final String updateMarc21 = "UPDATE authorities SET marc21 = ? WHERE authoritiesid = ?";

    private Map<String, MetaMapping> metaMappings;

    @PostConstruct
    public void readMetaMapping() {
        metaMappings = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectMetaMapping);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String meta = resultSet.getString(1);
                    String mapping = resultSet.getString(2);
                    String gndId = resultSet.getString(3);
                    String de101id = resultSet.getString(4);
                    String de588id = resultSet.getString(5);
                    String[] synonymsArray = (String[]) resultSet.getArray(6)
                            .getArray();
                    MetaMapping metaMapping = new MetaMapping(mapping, gndId, de101id, de588id);
                    metaMappings.put(meta, metaMapping);
                    if (!metaMappings.containsKey(mapping)) {
                        metaMappings.put(mapping, metaMapping);
                    }
                    Arrays.stream(synonymsArray)
                            .filter(synonym -> !metaMappings.containsKey(synonym))
                            .forEach(synonym -> {
                                if (!metaMappings.containsKey(synonym)) {
                                    metaMappings.put(synonym, metaMapping);
                                }
                            });
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
    }

    public List<AuthoritiesData> findPacelliMetadata(int limit, int offset) {
        List<AuthoritiesData> authoritiesDataList = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectPacelli);
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                ResultSet resultSet =  statement.executeQuery();
                while (resultSet.next()) {
                    AuthoritiesData authoritiesData = new AuthoritiesData();
                    authoritiesData.setAuthoritiesId(resultSet.getInt(1));
                    authoritiesData.setGndId(resultSet.getString(2));
                    authoritiesData.setViafId(resultSet.getString(3));
                    authoritiesData.setDateOfBirth(resultSet.getString(4));
                    authoritiesData.setDateOfDeath(resultSet.getString(5));
                    String[] meta = (String[]) resultSet.getArray(6)
                            .getArray();
                    authoritiesData.setMetaMappings(findMetaMappings(meta));
                    authoritiesData.setFirstName(resultSet.getString(7));
                    authoritiesData.setLastName(resultSet.getString(8));
                    authoritiesData.setDetails(resultSet.getString(9));
                    authoritiesDataList.add(authoritiesData);
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return authoritiesDataList;
    }

    public void updateMarc21(Document marc21, int pacelliId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(updateMarc21);
                SQLXML marc21Xml = connection.createSQLXML();
                marc21Xml.setString(marc21.toXML());
                statement.setSQLXML(1, marc21Xml);
                statement.setInt(2, pacelliId);
                statement.execute();
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
    }

    private List<MetaMapping> findMetaMappings(String[] metaArray) {
        List<MetaMapping> metaMappingsList = new ArrayList<>();
        for (String meta : metaArray) {
            Optional<String> optionalMapping =  metaMappings.keySet()
                    .parallelStream()
                    .filter(meta::contains)
                    .findFirst();
            optionalMapping.ifPresent(optional -> {
                metaMappingsList.add(metaMappings.get(optional));
            });
        }
        return metaMappingsList;
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
