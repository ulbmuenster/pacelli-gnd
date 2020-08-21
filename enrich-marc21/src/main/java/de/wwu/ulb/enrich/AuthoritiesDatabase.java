/*
 * This file is part of enrich-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * enrich-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.enrich;

import de.wwu.ulb.enrich.model.Authority;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AuthoritiesDatabase {

    static final Logger LOG = Logger.getLogger(AuthoritiesDatabase.class.getName());

    @Inject
    DataSource dataSource;

    private final String selectAuthorities = "SELECT authoritiesid, gndid FROM authorities " +
            "WHERE gndid IS NOT NULL AND gndlevel IS NULL";

    private final String updateAuthorities = "UPDATE authorities SET gndid = ?, pngndid = ?, gndlevel = ? " +
            "WHERE authoritiesid = ?";

    private final String removegndid = "UPDATE authorities SET gndid = NULL, removedgndid = ? " +
            "WHERE authoritiesid = ?";

    public List<Authority> findAll() {
        List<Authority> authorities = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(selectAuthorities);
                ResultSet resultSet =  statement.executeQuery();
                while (resultSet.next()) {
                    Authority pacelliAuthority = new Authority();
                    pacelliAuthority.setAuthoritiesId(resultSet.getInt(1));
                    pacelliAuthority.setGndId(resultSet.getString(2));
                    authorities.add(pacelliAuthority);
                }
            }
        } catch (SQLException e) {
            LOG.fatal(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
        return authorities;
    }

    public void updateAuthority(Integer authoritiesId, String gndId, String gndLevel, String pnGndId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(updateAuthorities);
                if (gndId.isEmpty()) {
                    statement.setNull(1, Types.VARCHAR);
                } else {
                    statement.setString(1, gndId);
                }
                if (pnGndId.isEmpty()) {
                    statement.setNull(2, Types.VARCHAR);
                } else {
                    statement.setString(2, pnGndId);
                }
                if (gndLevel.isEmpty()) {
                    statement.setNull(3, Types.VARCHAR);
                } else {
                    statement.setString(3, gndLevel);
                }
                statement.setInt(4, authoritiesId);
                statement.execute();
            }
        } catch (SQLException e) {
            LOG.fatal(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
    }

    public void removeGndId(Integer authoritiesId, String gndId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.prepareStatement(removegndid);
                statement.setString(1, gndId);
                statement.setInt(2, authoritiesId);
                statement.execute();
            }
        } catch (SQLException e) {
            LOG.fatal(e.getMessage());
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
