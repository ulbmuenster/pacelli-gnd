/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.client;

import de.wwu.ulb.authorities.search.SearchIndex;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.Dependent;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;

@Path("/")
@Dependent
@RegisterRestClient
@RegisterProvider(AuthoritySruException.class)
public interface AuthoritySruSearchClient extends Serializable {

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthoritySearchResponse searchAuthority(
            @QueryParam("query") String query,
            @QueryParam("index") SearchIndex searchIndex,
            @QueryParam("startPosition") Integer startPosition,
            @QueryParam("maxResults") Integer maxResults);
}
