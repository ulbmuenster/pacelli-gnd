/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.search;

import de.wwu.ulb.authorities.marc.MarcData;
import de.wwu.ulb.authorities.marc.MarcDataConverter;
import de.wwu.ulb.authorities.soap.SoapTools;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.opentracing.Traced;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * This service offers a REST-API, to simplify accessinga SRU search service
 * to run a search in GND with WOE index.
 *
 * @author Werner Greßhoff
 */
@RequestScoped
@Path("/retrieve")
@Traced
@Tag(name = "authority search service",
        description = "search and retrieve authority records with SRU service")
public class AuthoritiesSearchResource {

    private final Logger LOG = Logger.getLogger(AuthoritiesSearchResource.class);

    private final String SEARCH_PARAMETERS = "?version=1.1&operation=searchRetrieve" +
            "&recordSchema=MARC21-xml&startRecord=%d&maximumRecords=%d&accessToken=%s" +
            "&query=%s";

    @Inject
    @ConfigProperty(name = "dnb.gnd.service.search.url")
    String gndServiceUrl;

    @Inject
    @ConfigProperty(name = "dnb.gnd.service.search.token")
    String gndAccessToken;

    @Inject
    SoapTools soapTools;

    @Inject
    MarcDataConverter marcDataConverter;

    @Inject
    Config config;

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(description = "Count - Searches", name = "countSearchAuthority", absolute = true,
            displayName = "Search authority records")
    @Metered(description = "Analysis - Searches", name = "meterSearchAuthority", absolute = true)
    @Timed(description = "Runtime - Searches", name = "timeSearchAuthority", absolute = true)
    @Operation(summary = "Search for authority records in GND")
    @APIResponse(responseCode = "200",
            description = "MARC data of found authority records",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AuthoritySearchResponse.class)))
    public Response searchAuthority(
            @Parameter(description = "Query string") @QueryParam("query") String query,
            @Parameter(description = "Index") @QueryParam("index") SearchIndex searchIndex,
            @Parameter(description = "Starting position of cursor") @QueryParam("startPosition") Integer startPosition,
            @Parameter(description = "max. number of result records") @QueryParam("maxResults") Integer maxResults) {
        LOG.info("Entering searchAuthority");
        String indexName = config.getOptionalValue(searchIndex.getIndexConfig(), String.class)
                .orElse("WOE");
        String indexQuery = config.getOptionalValue(searchIndex.getQueryConfig(), String.class)
                .orElse("");
        if (startPosition == null) {
            startPosition = 0;
        }
        if (maxResults == null) {
            maxResults = 10;
        }
        LOG.info(indexName + "=" + query + " " + indexQuery);
        Document responseDocument = findAuthorities(indexName + "=" + query + " " + indexQuery,
                startPosition, maxResults);
        List<MarcData> marcDatas = new ArrayList<>();
        if (responseDocument != null) {
            for (Iterator<Node> nodeIterator = soapTools.getMarcXmlRoots(responseDocument, true);
                 nodeIterator.hasNext(); ) {
                Element marcData = (Element) nodeIterator.next();
                marcDatas.add(marcDataConverter.getMarcData(marcData, true));
            }
            int numberOfResults = soapTools.getNumberOfResults(responseDocument);
            return Response.ok(new AuthoritySearchResponse(marcDatas, numberOfResults), MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.noContent()
                .build();
    }

    private Document findAuthorities(String query, Integer startPosition, Integer maxResults) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8")
                    .replaceAll("\\+", "%20");
            URL serviceUrl = new URL(gndServiceUrl + String.format(SEARCH_PARAMETERS,
                    startPosition, maxResults, gndAccessToken, encodedQuery));
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
            LOG.info(proxy.toString());
            HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection(proxy);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
            LOG.info("connection created");
            return soapTools.buildDocument(connection);
        } catch (IOException e) {
            LOG.fatal(e.getMessage());
            return null;
        }
    }

}
