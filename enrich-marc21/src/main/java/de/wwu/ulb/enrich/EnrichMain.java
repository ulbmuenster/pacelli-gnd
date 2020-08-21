/*
 * This file is part of enrich-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * enrich-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.enrich;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.inject.Inject;

@QuarkusMain
public class EnrichMain implements QuarkusApplication {

    @Inject
    EnrichResource enrichResource;

    @Override
    public int run(String... args) throws Exception {
        enrichResource.startEnrichment();
        return 0;
    }
}
