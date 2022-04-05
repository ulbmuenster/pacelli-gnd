/*
 * This file is part of authorities-management.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authorities-management is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.authorities.soap;

import de.wwu.ulb.authorities.marc.SruAction;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Iterator;

@ApplicationScoped
public class SoapTools {

    private final Logger LOG = Logger.getLogger(SoapTools.class);

    private final String SOAPENV_URI = "http://schemas.xmlsoap.org/soap/envelope/";

    private final String DIAG_URI = "http://www.loc.gov/zing/srw/diagnostic/";

    private final String SRW_URI = "http://www.loc.gov/zing/srw/";

    private final String UCP_URI = "http://www.loc.gov/zing/srw/update/"; //"info:lc/xmlns/update-v1";

    private final String MARC_URI = "http://www.loc.gov/MARC21/slim";

    private final String RECORD_SCHEMA = "MARC21-xml";

    public Document generateSoapEnvelope(String authenticationToken, SruAction sruAction,
                                                Element record, String identifier) {
        Element soapenv = new Element("soap:Envelope", SOAPENV_URI);
        Element body = new Element("soap:Body", SOAPENV_URI);
        soapenv.appendChild(body);
        Element updateRequest = new Element("ucp:updateRequest", UCP_URI);
        updateRequest.addNamespaceDeclaration("srw", SRW_URI);
        updateRequest.addNamespaceDeclaration("diag", DIAG_URI);
        body.appendChild(updateRequest);
        Element version = new Element("srw:version", SRW_URI);
        version.appendChild("1.0");
        updateRequest.appendChild(version);
        Element action = new Element("ucp:action", UCP_URI);
        if (identifier == null) {
            sruAction = SruAction.CREATE;
        }
        action.appendChild(sruAction.getIdentifier());
        updateRequest.appendChild(action);
        if (sruAction.equals(SruAction.REPLACE)) {
            Element recordIdentifier = new Element("ucp:recordIdentifier", UCP_URI);
            recordIdentifier.appendChild("gnd:gnd" + identifier);
            updateRequest.appendChild(recordIdentifier);
        }
        Element srwRecord = new Element("srw:record", SRW_URI);
        updateRequest.appendChild(srwRecord);
        Element recordPacking = new Element("srw:recordPacking", SRW_URI);
        recordPacking.appendChild("xml");
        srwRecord.appendChild(recordPacking);
        Element recordSchema = new Element("srw:recordSchema", SRW_URI);
        recordSchema.appendChild(RECORD_SCHEMA);
        srwRecord.appendChild(recordSchema);
        Element recordData = new Element("srw:recordData", SRW_URI);
        recordData.appendChild(record);
        srwRecord.appendChild(recordData);
        Element authenticationTokenElement = new Element("authenticationToken");
        authenticationTokenElement.appendChild(authenticationToken);
        Element extraRequestData = new Element("srw:extraRequestData", SRW_URI);
        extraRequestData.appendChild(authenticationTokenElement);
        updateRequest.appendChild(extraRequestData);
        return new Document(soapenv);
    }

    public String getOperationStatus(Document document) {
        XPathContext ucpContext = new XPathContext("ucp", UCP_URI);
        Nodes nodes = document.query("//ucp:operationStatus", ucpContext);
        return nodes.size() > 0 ? nodes.get(0).getValue() : "fail";
    }

    public String getDiagMessage(Document document) {
        StringBuilder diagnose = new StringBuilder();
        XPathContext diagContext = new XPathContext("diag", DIAG_URI);
        Nodes nodes = document.query("//diag:diagnostic", diagContext);
        Iterator<Node> nodeIterator = nodes.iterator();
        String details = null;
        String message = null;
        while (nodeIterator.hasNext()) {
            LOG.info("found Diagnosis element");
            Element diagnostic = (Element) nodeIterator.next();
            Elements detailsElements = diagnostic.getChildElements();
            for (Iterator<Element> iterator = detailsElements.iterator(); iterator.hasNext(); ) {
                Element element = iterator.next();
                if (element.getLocalName().equals("details")) {
                    details = element.getValue();
                }
                if (element.getLocalName().equals("message")) {
                    message = element.getValue();
                }
            }
            LOG.info(details);
            if (details.equals("Cataloguing/validation error.")) {
                diagnose.append(message);
                break;
            }
        }
        return diagnose.toString();
    }

    public Element getMarcXmlRoot(Document document, boolean useMarcNamespace) {
        Iterator<Node> nodesIterator= getMarcXmlRoots(document, useMarcNamespace);
        if (nodesIterator.hasNext()) {
            return (Element) nodesIterator.next();
        }
        return null;
    }

    public Iterator<Node> getMarcXmlRoots(Document document, boolean useMarcNamespace) {
        XPathContext marcContext = useMarcNamespace ? new XPathContext("marcxml", MARC_URI) : null;
        String marcPrefix = useMarcNamespace ? "marcxml:" : "";
        Nodes nodes = document.query("//" + marcPrefix + "record", marcContext);
        return nodes.iterator();
    }

    public int getNumberOfResults(Document document) {
        XPathContext srwContext = new XPathContext("srw", SRW_URI);
        Nodes nodes = document.query("//srw:numberOfRecords", srwContext);
        Iterator<Node> nodesIterator = nodes.iterator();
        if (nodesIterator.hasNext()) {
            Element element = (Element) nodesIterator.next();
            String numberOfRecords = element.getValue();
            return Integer.parseInt(numberOfRecords);
        }
        return 0;
    }

    public Document buildDocument(HttpURLConnection connection) {
        InputStream inputStream = null;
        try {
            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    LOG.info("fetch InputStream");
                    inputStream = connection.getInputStream();
                    LOG.info("InputStream was fetched");
                    Builder builder = new Builder();
                    LOG.info("create Document");
                    Document xml = builder.build(inputStream);
                    LOG.info("Document was built");
                    return xml;
                default:
                    LOG.info("Responsecode: " + connection.getResponseCode());
                    LOG.info("Response: " + connection.getResponseMessage());
                    connection.getErrorStream()
                            .close();
            }
        } catch (IOException | ParsingException e) {
            LOG.fatal(e.getClass().toString());
            LOG.fatal(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.fatal("Finally closing InputStream lead to problem.");
                    LOG.fatal(e.getMessage());
                }
            }
        }
        return null;
    }
}
