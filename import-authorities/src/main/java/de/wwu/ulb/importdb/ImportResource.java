/*
 * This file is part of import-authorities.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * import-pacelli is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.importdb;

import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@ApplicationScoped
public class ImportResource {

    private static final Logger LOG = Logger.getLogger(ImportResource.class.getName());

    @Inject
    AuthoritiesDatabase authoritiesDatabase;

    public boolean startImport(String basePath, String fileNamePattern) {
        authoritiesDatabase.createTable();
        try {
            Files.walk(Paths.get(basePath))
                    .filter(path -> path.getFileName().toString().matches(fileNamePattern))
                    .forEach(path -> authoritiesDatabase.importData(path));
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return false;
        }
    }

}
