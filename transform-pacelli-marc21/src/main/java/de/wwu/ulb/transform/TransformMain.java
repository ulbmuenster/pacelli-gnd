/*
 * This file is part of transform-pacelli-marc21.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.inject.Inject;

@QuarkusMain
public class TransformMain implements QuarkusApplication {

    @Inject
    TransformResource transformResource;

    @Override
    public int run(String... args) throws Exception {
        transformResource.startTransformation();
        return 0;
    }
}
