/*
 * This file is part of import-authorities.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * import-pacelli is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.importdb;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AuthoritiesDatabase {

    static final Logger LOG = Logger.getLogger(AuthoritiesDatabase.class.getName());

    Builder builder = new Builder();

    final String insertData = "INSERT INTO authorities " +
            "(\n" +
            "   authoritiesid,\n" +
            "   gndid,\n" +
            "   viafid,\n" +
            "   dateofbirth,\n" +
            "   dateofdeath,\n" +
            "   meta,\n" +
            "   firstname,\n" +
            "   lastname,\n" +
            "   details,\n" +
            "   rawxml,\n" +
            "   filename\n" +
            ") VALUES\n" +
            "   (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    final String createTable = "CREATE TABLE IF NOT EXISTS authorities\n" +
            "(\n" +
            "    authoritiesid integer NOT NULL,\n" +
            "    gndid character varying(50),\n" +
            "    viafid character varying(50),\n" +
            "    dateofbirth character varying(50),\n" +
            "    dateofdeath character varying(50),\n" +
            "    meta character varying[],\n" +
            "    firstname character varying(200),\n" +
            "    lastname character varying(100),\n" +
            "    details text,\n" +
            "    rawxml xml NOT NULL,\n" +
            "    filename character varying(255),\n" +
            "    marc21 xml,\n" +
            "    gndupdated character varying(10),\n" +
            "    removedgndid character varying(50),\n" +
            "    pngndid character varying(50),\n" +
            "    gndlevel character varying(4),\n" +
            "    notes character varying(500),\n" +
            "    CONSTRAINT authorities_pk PRIMARY KEY (authoritiesid)\n" +
            ");\n";

    @Inject
    DataSource dataSource;

    public void createTable() {
        Connection connection = null;
        Statement statement = null;
        try {
            if (connection == null || connection.isClosed()) {
                connection = dataSource.getConnection();
                statement = connection.createStatement();
                statement.execute(createTable);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
        } finally {
            closeAll(statement, connection);
        }
    }

    public void importData(Path bioFile) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            Document bio = builder.build(bioFile.toFile());
            Element bioRoot = bio.getRootElement();
            String pacelliId = bioRoot.getFirstChildElement("idno")
                    .getValue();
            if (!bioFile.toString().contains(pacelliId)) {
                LOG.error("Falsche ID in: " + bioFile.toString());
            }
            String gndId = null;
            if (bioRoot.getFirstChildElement("gnd") != null) {
                gndId = bioRoot.getFirstChildElement("gnd")
                        .getValue();
            }
            String viafId = null;
            if (bioRoot.getFirstChildElement("viaf") != null) {
                viafId = bioRoot.getFirstChildElement("viaf")
                        .getValue();
            }
            String firstName = null;
            if (bioRoot.getFirstChildElement("name").getFirstChildElement("firstname") != null) {
                firstName = bioRoot.getFirstChildElement("name")
                        .getFirstChildElement("firstname")
                        .getValue();
            }
            String lastName = null;
            if (bioRoot.getFirstChildElement("name").getFirstChildElement("lastname") != null) {
                lastName = bioRoot.getFirstChildElement("name")
                        .getFirstChildElement("lastname")
                        .getValue();
            }
            String dateOfBirth = null;
            String dateOfDeath = null;
            if (bioRoot.getFirstChildElement("dates") != null) {
                if (bioRoot.getFirstChildElement("dates")
                        .getFirstChildElement("from") != null) {
                    dateOfBirth = bioRoot.getFirstChildElement("dates")
                            .getFirstChildElement("from")
                            .getValue();
                }
                if (bioRoot.getFirstChildElement("dates")
                        .getFirstChildElement("to") != null) {
                    dateOfDeath = bioRoot.getFirstChildElement("dates")
                            .getFirstChildElement("to")
                            .getValue();
                }
            }
            List<String> meta = new ArrayList<>();
            Elements metaElements = bioRoot.getChildElements("meta");
            for (int i = 0; i < metaElements.size(); i++) {
                meta.add(metaElements.get(i).getValue());
            }
            String details = null;
            if (bioRoot.getFirstChildElement("details") != null) {
                details = bioRoot.getFirstChildElement("details")
                        .getValue()
                        .replaceAll("<br/>", "")
                        .replaceAll("<glz/>", "");
            }
            try {
                if (connection == null || connection.isClosed()) {
                    connection = dataSource.getConnection();
                    statement = connection.prepareStatement(insertData);
                    statement.setInt(1, Integer.parseInt(pacelliId));
                    if (gndId == null) {
                        statement.setNull(2, Types.VARCHAR);
                    } else {
                        statement.setString(2, gndId);
                    }
                    if (viafId == null) {
                        statement.setNull(3, Types.VARCHAR);
                    } else {
                        statement.setString(3, viafId);
                    }
                    if (firstName == null) {
                        statement.setNull(7, Types.VARCHAR);
                    } else {
                        statement.setString(7, firstName);
                    }
                    if (lastName == null) {
                        statement.setNull(8, Types.VARCHAR);
                    } else {
                        statement.setString(8, lastName);
                    }
                    if (dateOfBirth == null) {
                        statement.setNull(4, Types.VARCHAR);
                    } else {
                        statement.setString(4, dateOfBirth);
                    }
                    if (dateOfDeath == null) {
                        statement.setNull(5, Types.VARCHAR);
                    } else {
                        statement.setString(5, dateOfDeath);
                    }
                    statement.setObject(6, meta.toArray(new String[meta.size()]));
                    if (details == null) {
                        statement.setNull(9, Types.CLOB);
                    } else {
                        statement.setString(9, details);
                    }
                    SQLXML bioXml = connection.createSQLXML();
                    bioXml.setString(bio.toXML());
                    statement.setSQLXML(10, bioXml);
                    statement.setString(11, bioFile.getFileName().toString());
                    statement.execute();
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage());
                LOG.error("Datei: " + bioFile.getFileName().toString());
            } finally {
                closeAll(statement, connection);
            }
        } catch (ParsingException | IOException e) {
            LOG.error(e.getMessage());
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
