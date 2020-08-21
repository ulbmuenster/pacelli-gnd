/*
 * This file is part of prepare-meta-mapping.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * prepare-meta-mapping is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mapping;

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
public class MetaMappingMain implements QuarkusApplication {

    @Inject
    MetaMappingResource metaMappingResource;

    @Override
    public int run(String... args) throws Exception {
        final Options options = new Options();
        options.addOption(Option.builder("s")
                        .longOpt("sruService")
                        .hasArg(true)
                        .numberOfArgs(1)
                        .desc("URI of sru service")
                        .build())
                .addOption(Option.builder("a")
                        .longOpt("accessToken")
                        .hasArg(true)
                        .numberOfArgs(1)
                        .desc("Token for the access of DNB sru service")
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
        String sruService = commandLine.getOptionValue("sruService");
        String fileNamePattern = commandLine.getOptionValue("fileNamePattern");
        boolean result = metaMappingResource.startMapping(sruService, fileNamePattern);

        return result ? 0 : 1;
    }
}
