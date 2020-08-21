/*
 * This file is part of import-authorities.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * import-pacelli is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.importdb;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.inject.Inject;

@QuarkusMain
public class ImportMain implements QuarkusApplication {

    @Inject
    ImportResource importResource;

    @Override
    public int run(String... args) throws Exception {
        final Options options = new Options();
        options.addOption(Option.builder("b")
                        .longOpt("basePath")
                        .hasArg(true)
                        .numberOfArgs(1)
                        .desc("Directory of files to import")
                        .build())
                .addOption(Option.builder("f")
                        .longOpt("fileNamePattern")
                        .hasArg(true)
                        .numberOfArgs(1)
                        .desc("Pattern of file names to import")
                        .build());
        CommandLineParser commandLineParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLine commandLine = null;

        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (ParseException ex) {
            helpFormatter.printHelp("basePath", options);
            return 1;
        }
        String basePath = commandLine.getOptionValue("basePath");
        String fileNamePattern = commandLine.getOptionValue("fileNamePattern");

        boolean result = importResource.startImport(basePath, fileNamePattern);
        return result ? 0 : 1;
    }
}
