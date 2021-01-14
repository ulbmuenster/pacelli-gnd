/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.update;

import de.wwu.ulb.authorities.marc.MarcData;
import de.wwu.ulb.authorities.marc.MarcDataConverter;
import de.wwu.ulb.authorities.soap.SoapTools;
import nu.xom.Document;
import nu.xom.Element;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static de.wwu.ulb.authorities.marc.SruAction.CREATE;
import static de.wwu.ulb.authorities.marc.SruAction.REPLACE;

/**
 * This service offers a REST-API, to simplify accessing a SRU Record update service
 *
 * @author Werner Greßhoff
 */
@RequestScoped
@DeclareRoles({
        "AuthoritiesManagerL1",
        "AuthoritiesManagerL2",
        "AuthoritiesManagerL3",
        "AuthoritiesManagerL4",
        "AuthoritiesManagerL5",
        "AuthoritiesManagerL6",
        "AuthoritiesManagerL7"})
@Path("/record")
@DenyAll
@Traced
@Tag(name = "Authority update service",
        description = "update authority records with SRU service")
public class AuthoritiesUpdateResource {

    private final Logger LOG = Logger.getLogger(AuthoritiesUpdateResource.class);

    @Inject
    Config config;

    @Inject
    @ConfigProperty(name = "dnb.gnd.service.update.url")
    String gndServiceUrl;

    @Inject
    JsonWebToken callerPrincipal;

    @Inject
    SoapTools soapTools;

    @Inject
    MarcDataConverter marcDataConverter;

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({
            "AuthoritiesManagerL1",
            "AuthoritiesManagerL2",
            "AuthoritiesManagerL3",
            "AuthoritiesManagerL4",
            "AuthoritiesManagerL5",
            "AuthoritiesManagerL6",
            "AuthoritiesManagerL7"})
    @Counted(description = "Count - authority record created", name = "countCreateAuthority", absolute = true,
            displayName = "Create authority record")
    @Metered(description = "Analysis - authority record created", name = "meterCreateAuthority", absolute = true)
    @Timed(description = "Runtime - authority record created", name = "timeCreateAuthority", absolute = true)
    @Operation(summary = "Creation of a authority record in GND")
    @APIResponse(responseCode = "200",
            description = "The GND id of the new authority record",
            content = @Content(mediaType = "text/plain",
                    schema = @Schema(implementation = String.class)))
    public Response createAuthority(
            @RequestBody(description = "Marc21 record", required = true) MarcData marcData) {
        LOG.info("createAuthority entered");
        String timeField = marcData.getControlFields().get("008");
        marcData.getControlFields()
                .put("008", LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + timeField.substring(6));
        Element recordElement = marcDataConverter.generateMarcXmlAuthority(marcData);
        Document soapenv = soapTools.generateSoapEnvelope(getToken(), CREATE, recordElement, null);
        Document responseDocument = sendDocument(soapenv.toXML());
        return buildResponse(responseDocument);
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({
            "AuthoritiesManagerL1",
            "AuthoritiesManagerL2",
            "AuthoritiesManagerL3",
            "AuthoritiesManagerL4",
            "AuthoritiesManagerL5",
            "AuthoritiesManagerL6",
            "AuthoritiesManagerL7"})
    @Counted(description = "Count - authority record updated", name = "countUpdateAuthority", absolute = true,
            displayName = "Update an authority record")
    @Metered(description = "Analysis - authority record updated", name = "meterUpdateAuthority", absolute = true)
    @Timed(description = "Runtime - authority record updated", name = "timeUpdateAuthority", absolute = true)
    @Operation(summary = "Update of an authority record in GND")
    @APIResponse(responseCode = "200",
            description = "The GND identifier of updated authority record",
            content = @Content(mediaType = "text/plain",
                    schema = @Schema(implementation = String.class)))
    public Response updateAuthority(
            @RequestBody(description = "Marc21 record", required = true) MarcData marcData) {
        LOG.info("updateAuthority entered");
        String identifier = marcData.getControlFields()
                .get("001");
        Element recordElement = marcDataConverter.generateMarcXmlAuthority(marcData);
        Document soapenv = soapTools.generateSoapEnvelope(getToken(), REPLACE, recordElement, identifier);
        Document responseDocument = sendDocument(soapenv.toXML());
        return buildResponse(responseDocument);
    }

    private Response buildResponse(Document responseDocument) {
        String status = soapTools.getOperationStatus(responseDocument);
        LOG.info(responseDocument.toXML());
        if (status.equals("fail")) {
            String diagnose = soapTools.getDiagMessage(responseDocument);
            LOG.info(diagnose);
            return Response.notModified(diagnose)
                    .build();
        }
        MarcData responseMarc = marcDataConverter.getMarcData(
                soapTools.getMarcXmlRoot(responseDocument, false), false);
        return Response.ok(responseMarc)
                .build();
    }

    private String getToken() {
        Set<String> groupSet = callerPrincipal.getGroups();
        String[] groups = groupSet.toArray(new String[0]);
        if (groups.length > 0) {
            String group = groups[0];
            return config.getValue("dnb.gnd.service.token." + group, String.class);
        }
        return null;
    }

    private Document sendDocument(String output) {
        try {
            output = Normalizer.normalize(output, Normalizer.Form.NFD);
            LOG.debug(output);
            URL serviceUrl = new URL(gndServiceUrl);
            Proxy proxy;
            try {
                Optional<String> proxyPort = config.getOptionalValue("https.proxyPort", String.class);
                Optional<String> proxyHost = config.getOptionalValue("https.proxyHost", String.class);
                int port = Integer.parseInt(proxyPort.orElse("80"));
                proxy = proxyHost.isPresent()
                        ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost.get(), port))
                        : Proxy.NO_PROXY;
            } catch (NoSuchElementException e) {
                proxy = Proxy.NO_PROXY;
            }
            HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection(proxy);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            out.write(output.getBytes("UTF-8"));
            return soapTools.buildDocument(connection);
        } catch (IOException e) {
            LOG.fatal(e.getMessage());
            return null;
        }
    }
}