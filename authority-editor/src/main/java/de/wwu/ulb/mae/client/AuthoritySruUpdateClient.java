/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae.client;

import de.wwu.ulb.authorities.marc.MarcData;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.Dependent;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;

@Path("/")
@Dependent
@RegisterRestClient
@RegisterProvider(AuthoritySruException.class)
public interface AuthoritySruUpdateClient extends Serializable {

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAuthority(@HeaderParam("Authorization") String authorization,
                                    MarcData marcData);

    @POST
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAuthority(@HeaderParam("Authorization") String authorization,
                                    MarcData marcData);
}
