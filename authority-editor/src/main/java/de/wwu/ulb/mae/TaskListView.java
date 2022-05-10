/*
 * This file is part of authority-editor.
 * Copyright (C) 2020, 2021 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.mae;

import de.wwu.ulb.authorities.marc.DataField;
import de.wwu.ulb.authorities.marc.MarcData;
import de.wwu.ulb.authorities.marc.MarcDataConverter;
import de.wwu.ulb.authorities.marc.SubField;
import de.wwu.ulb.authorities.search.SearchIndex;
import de.wwu.ulb.jwt.TokenUtils;
import de.wwu.ulb.mae.client.AuthoritySearchResponse;
import de.wwu.ulb.mae.model.DatabaseEntry;
import de.wwu.ulb.mae.model.LazyAuthorityModel;
import de.wwu.ulb.mae.model.LazyDatabaseEntryModel;
//import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.primefaces.event.CellEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyDataModel;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class TaskListView implements Serializable {

    private static final Logger LOG = Logger.getLogger(TaskListView.class.getName());

    @Inject
    Vertx vertx;

    WebClient searchClient;

    WebClient updateClient;

    boolean renderNotification;

    String notification;

    @Inject
    @Push(channel = "marc-authority-editor")
    PushContext pushContext;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities.pk")
    String pathToPrivateKey;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.users")
    String[] allowedUsers;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.mae.oidc", defaultValue = "SATOSA")
    String serverType;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-search-service.ssl", defaultValue = "true")
    boolean authoritiesSearchServiceSsl;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-search-service.host")
    String authoritiesSearchServiceHost;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-search-service.port")
    Integer authoritiesSearchServicePort;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-search-service.query")
    String authoritiesSearchServiceQuery;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-update-service.ssl", defaultValue = "true")
    boolean authoritiesUpdateServiceSsl;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-update-service.host")
    String authoritiesUpdateServiceHost;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-update-service.port")
    Integer authoritiesUpdateServicePort;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-update-service.create")
    String authoritiesUpdateServiceCreate;

    @Inject
    @ConfigProperty(name = "de.wwu.ulb.authorities-update-service.update")
    String authoritiesUpdateServiceUpdate;

    @Inject
    Database database;

    Builder builder;

    Jsonb jsonb;

    LazyDataModel<DatabaseEntry> lazyModel;

    LazyDataModel<DatabaseEntry> lazyEditedModel;

    LazyDataModel<MarcData> lazyAuthorityModel;

    DatabaseEntry selectedDatabaseEntry;

    DatabaseEntry selectedEditedDatabaseEntry;

    MarcData selectedMarcData;

    MarcData selectedAuthority;

    MarcData authorityMarcData;

    String directGndId;

//    UserInfo userInfo;

    String userName;

    @Inject
    SecurityIdentity securityIdentity;

    @PostConstruct
    public void init() {
        this.searchClient = WebClient.create(vertx,
                new WebClientOptions().setDefaultHost(authoritiesSearchServiceHost)
                        .setDefaultPort(authoritiesSearchServicePort)
                        .setSsl(authoritiesSearchServiceSsl)
                        .setTrustAll(true));
        this.updateClient = WebClient.create(vertx,
                new WebClientOptions().setDefaultHost(authoritiesUpdateServiceHost)
                        .setDefaultPort(authoritiesUpdateServicePort)
                        .setSsl(authoritiesUpdateServiceSsl)
                        .setTrustAll(true));
        lazyModel = new LazyDatabaseEntryModel(database, false);
        lazyEditedModel = new LazyDatabaseEntryModel(database, true);
        jsonb = JsonbBuilder.create();
        builder = new Builder();
        userName = securityIdentity.getPrincipal().getName();
        LOG.debug("Username: " + userName);
        /*
        userInfo = (UserInfo) securityIdentity.getAttribute("userinfo");
        if (serverType.equals("dex")) {
            JsonObject federatedClaims = userInfo.getObject("federated_claims");
            LOG.info(federatedClaims.getString("user_id"));
            userName = federatedClaims.getString("user_id");
        } else {
            userName = userInfo.getString("username");
            LOG.info(userInfo.getString("username"));
        }
         */
    }

    public boolean isRenderNotification() {
        return renderNotification;
    }

    public void setRenderNotification(boolean renderNotification) {
        this.renderNotification = renderNotification;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public LazyDataModel<DatabaseEntry> getLazyModel() {
        return lazyModel;
    }

    public LazyDataModel<DatabaseEntry> getLazyEditedModel() {
        return lazyEditedModel;
    }

    public LazyDataModel<MarcData> getLazyAuthorityModel() {
        return lazyAuthorityModel;
    }

    public boolean isEditor() {
        if (List.of(allowedUsers).contains(userName)) {
            return true;
        }
        return false;
    }

    public void onRowSelect(SelectEvent selectEvent) throws IOException {
        DatabaseEntry selected = (DatabaseEntry) selectEvent.getObject();
        MarcQueryResult marcEntry = database.findMarcEntry(selected.getId());
        MarcData newMarcData = null;
        try {
            Document marcDocument = builder.build(marcEntry.getXml(), null);
            newMarcData = MarcDataConverter.getMarcData(
                    marcDocument.getRootElement());
        } catch (ParsingException | IOException e) {
            LOG.error(e.getMessage());
            return;
        }
        setRenderNotification(false);
        if (selected.getGndId() != null) {
            onRowSelectWithGndId(selected, newMarcData);
        } else {
            selectedMarcData = newMarcData;
            lazyAuthorityModel = new LazyAuthorityModel(
                    searchClient, authoritiesSearchServiceQuery, marcEntry.getFirstName(), marcEntry.getLastName());
            FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .redirect("/xhtml/searchAuthority.xhtml");
        }
    }

    public void setEditedDatabaseEntry(DatabaseEntry databaseEntry) throws IOException {
        LOG.debug("ID: " + databaseEntry.getId());
        MarcQueryResult marcEntry = database.findMarcEntry(databaseEntry.getId());
        MarcData newMarcData = null;
        try {
            Document marcDocument = builder.build(marcEntry.getXml(), null);
            newMarcData = MarcDataConverter.getMarcData(
                    marcDocument.getRootElement());
            if (newMarcData.getPacelliSource().isEmpty()) {
                newMarcData.addPacelliSource(databaseEntry.getId());
            }
        } catch (ParsingException | IOException e) {
            LOG.debug("Parserausnahme!");
            LOG.error(e.getMessage());
            return;
        }
        setRenderNotification(false);
        if (databaseEntry.getGndId() != null) {
            LOG.debug("GND-ID: " + databaseEntry.getGndId());
            onRowSelectWithGndId(databaseEntry, newMarcData);
        } else {
            LOG.debug("Starte Suche");
            selectedMarcData = newMarcData;
            LOG.debug(selectedMarcData);
            lazyAuthorityModel = new LazyAuthorityModel(
                    searchClient, authoritiesSearchServiceQuery, marcEntry.getFirstName(), marcEntry.getLastName());
            FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .redirect("/xhtml/searchAuthority.xhtml");
        }
    }

    public void onNotesEdited(CellEditEvent event) {
        database.updateNotes(Integer.valueOf(event.getRowKey()),
                (String) event.getNewValue());
    }

    public void onEditedRowSelect(SelectEvent selectEvent) throws IOException {
        DatabaseEntry selected = (DatabaseEntry) selectEvent.getObject();
        MarcQueryResult marcEntry = database.findMarcEntry(selected.getId());
        MarcData newMarcData = null;
        try {
            Document marcDocument = builder.build(marcEntry.getXml(), null);
            newMarcData = MarcDataConverter.getMarcData(
                    marcDocument.getRootElement());
            authorityMarcData = newMarcData;
        } catch (ParsingException | IOException e) {
            LOG.error(e.getMessage());
            return;
        }
        setRenderNotification(false);
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/maeoutput.xhtml");
    }

    public void onAuthorityRowSelect(SelectEvent selectEvent) throws IOException {
        authorityMarcData = (MarcData) selectEvent.getObject();
        mixAuthorityMarcData(selectedMarcData);
        setRenderNotification(false);
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/mae.xhtml");
    }

    private void onRowSelectWithGndId(DatabaseEntry selected, MarcData newMarcData)
            throws IOException {
        authorityMarcData = null;
        Uni<AuthoritySearchResponse> result = searchClient.get(authoritiesSearchServiceQuery +
                        "?query=" + selected.getGndId() + "&index=" + SearchIndex.Identifier +
                        "&startPosition=" + 0 + "&maxResults=" + 1)
                .send()
                .onItem()
                .transform(response -> {
                    if (response.statusCode() == 200) {
                        return response.bodyAsJson(AuthoritySearchResponse.class);
                    } else {
                        return new AuthoritySearchResponse();
                    }
                });
        AuthoritySearchResponse authoritySearchResponse = result.await()
                .indefinitely();
        LOG.debug("Suche durchgeführt");
        List<MarcData> marcDatas = authoritySearchResponse.getMarcDatas();

        if (marcDatas.size() == 1) {
            LOG.debug("1 Treffer für GND-ID " + selected.getGndId());
            authorityMarcData = marcDatas.get(0);
        }
        if (authorityMarcData == null) {
            authorityMarcData = newMarcData;
        } else {
            LOG.debug("Starte Vermischung");
            mixAuthorityMarcData(newMarcData);
        }
        setRenderNotification(false);
        LOG.debug("Starte redirect");
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/mae.xhtml");
    }

    public void searchGndId() throws IOException {
        authorityMarcData = null;
        Uni<AuthoritySearchResponse> result = searchClient.get(authoritiesSearchServiceQuery +
                        "?query=" + directGndId + "&index=" + SearchIndex.Identifier +
                        "&startPosition=" + 0 + "&maxResults=" + 1)
                .send()
                .onItem()
                .transform(response -> {
                    if (response.statusCode() == 200) {
                        return response.bodyAsJson(AuthoritySearchResponse.class);
                    } else {
                        return new AuthoritySearchResponse();
                    }
                });
        AuthoritySearchResponse authoritySearchResponse = result.await()
                .indefinitely();
        List<MarcData> marcDatas = authoritySearchResponse.getMarcDatas();
        if (marcDatas.size() == 1) {
            authorityMarcData = marcDatas.get(0);
        }
        if (authorityMarcData == null) {
            authorityMarcData = selectedMarcData;
        } else {
            mixAuthorityMarcData(selectedMarcData);
        }
        setRenderNotification(false);
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/mae.xhtml");
    }

    private void mixAuthorityMarcData(MarcData newMarcData) {
        Map<String, List<DataField>> possiblyBetterList = new HashMap<>();
        LOG.debug("Suche nach neuen Daten: " + newMarcData.getDataFields().size());
        LOG.debug(newMarcData.getDataFields().keySet());
        for (String tag : newMarcData.getDataFields().keySet()) {
            if (!authorityMarcData.getDataFields().containsKey(tag)) {
                LOG.debug("Kein Tag: " + tag);
                authorityMarcData.getDataFields()
                        .put(tag, newMarcData.getDataFields().get(tag));
            } else {
                newMarcData.getDataFields()
                        .get(tag)
                        .forEach(dataField -> {
                            LOG.debug("Tag: " + tag);
                            if (tag.equals("548")) {
                                LOG.debug(authorityMarcData.getDataFields());
                                LOG.debug(authorityMarcData.getDataFields().get(tag));
                            }
                            if (authorityMarcData.getDataFields().get(tag) != null &&
                                    !authorityMarcData.getDataFields().get(tag).contains(dataField)) {
                                LOG.debug("Bislang kein datafield für: " + tag);
                                authorityMarcData.getDataFields()
                                        .get(tag)
                                        .add(dataField);
                                LOG.debug("datafield für: " + tag + " hinzugefügt");
                            } else {
                                LOG.debug("datafield für: " + tag + " evtl. besser");
                                //Possibly, the data is better than the old one, so check!
                                if (tag.equals("040") ||
                                        tag.equals("042") ||
                                        tag.equals("075") ||
                                        tag.equals("100") ||
                                        tag.equals("548") ||
                                        tag.equals("550")) {
                                    if (!possiblyBetterList.containsKey("tag")) {
                                        LOG.debug("Bislang kein Tag: " + tag);
                                        possiblyBetterList.put(tag, new ArrayList<>());
                                    }
                                    possiblyBetterList.get(tag)
                                            .add(dataField);
                                    LOG.debug("Tag: " + tag + " hinzugefügt...");
                                }
                            }
                            LOG.debug("Verarbeitung Tag: " + tag + " abgeschlossen");
                        });
            }
        }
        //Now check, if new data is better
        LOG.debug("Suche nach besseren Daten: " + possiblyBetterList.size());
        possiblyBetterList.keySet()
                .forEach(tag -> {
                    possiblyBetterList.get(tag)
                            .forEach(dataField -> {
                                LOG.debug("Bearbeite tag " + tag);
                                if (tag.equals("040")) {
                                    List<DataField> dataFields = authorityMarcData.getDataFields()
                                            .get("040");
                                    while (dataFields.size() > 1) {
                                        dataFields.remove(1);
                                    }
                                } else if (tag.equals("042")) {
                                    /*
                                    if (dataField.getSubFields().containsKey("a")) {
                                        String gndLevel = dataField.getSubFields()
                                                .get("a")
                                                .get(0)
                                                .getValue();
                                        int level = Integer.parseInt(gndLevel.substring(3));
                                        authorityMarcData.getDataFields()
                                                .get("042")
                                                .stream()
                                                .filter(authority -> authority.getSubFields().containsKey("a"))
                                                .forEach(authority -> {
                                                    String authorityGndLevel = authority.getSubFields()
                                                            .get("a")
                                                            .get(0)
                                                            .getValue();
                                                    LOG.debug("$a: " + authorityGndLevel);
                                                    int authorityLevel = Integer.parseInt(
                                                            authorityGndLevel.substring(3));
                                                    if (level < authorityLevel) {
                                                        authority.getSubFields()
                                                                .get("a")
                                                                .remove(0);
                                                        authority.getSubFields()
                                                                .get("a")
                                                                .add(0, new SubField(gndLevel, true));
                                                    }
                                                });
                                    }
                                     */
                                } else if (tag.equals("075")) {
                                    int index = authorityMarcData.getDataFields()
                                            .get(tag)
                                            .indexOf(dataField);
                                    DataField authority = authorityMarcData.getDataFields()
                                            .get(tag)
                                            .get(index);
                                    if (dataField.getSubFields().get("b").get(0).equals("p") &&
                                            authority.getSubFields().get("b").get(0).equals("n")) {
                                        authority.getSubFields()
                                                .get("b")
                                                .remove(0);
                                        authority.getSubFields()
                                                .get("b")
                                                .add(0, new SubField("p", true));
                                    }
                                } else if (tag.equals("100")) {
                                    if (dataField.getSubFields().containsKey("d")) {
                                        authorityMarcData.getDataFields()
                                                .get("100")
                                                .forEach(authority -> {
                                                    if (!authority.getSubFields().containsKey("d")) {
                                                        authority.getSubFields()
                                                                .put("d", dataField.getSubFields().get("d"));
                                                    }
                                                });
                                        if (authorityMarcData.getDataFields().containsKey("400")) {
                                            authorityMarcData.getDataFields()
                                                    .get("400")
                                                    .forEach(authority -> {
                                                        if (!authority.getSubFields().containsKey("d")) {
                                                            authority.getSubFields()
                                                                    .put("d", dataField.getSubFields().get("d"));
                                                        }
                                                    });
                                        }
                                    }
                                } else if (tag.equals("550")) {
                                    if (dataField.isDoubledType()) {
                                        for (SubField code4 : dataField.getSubFields().get("4")) {
                                            if (code4.getValue().equals("berc")) {
                                                code4.setValue("beru");
                                            }
                                        }
                                        for (SubField codeI : dataField.getSubFields().get("i")) {
                                            if (codeI.getValue().endsWith("Beruf")) {
                                                codeI.setValue("Beruf");
                                            }
                                        }
                                        authorityMarcData.getDataFields()
                                                .get("550")
                                                .add(dataField);
                                    }
                                }
                            });
                });
        LOG.debug("Vermischung abgeschlossen");
    }

    public void unselect() {
        selectedDatabaseEntry = null;
    }

    public void delete(String key, int index) {
        authorityMarcData.getDataFields()
                .get(key)
                .remove(index);
    }

    public void removeGndId() throws IOException {
        MarcQueryResult marcQueryResult = database.findMarcEntry(selectedDatabaseEntry.getId());
        String xml = marcQueryResult.getXml();
        try {
            Document marcDocument = builder.build(xml, null);
            MarcData marcData = MarcDataConverter.getMarcData(
                    marcDocument.getRootElement());
            marcData.getControlFields()
                    .remove("001");
            marcData.getControlFields()
                    .remove("003");
            marcData.getControlFields()
                    .remove("005");
            marcData.getDataFields()
                    .remove("024");
            if (marcData.getDataFields().containsKey("035")) {
                marcData.getDataFields()
                        .remove("035");
            }
            if (marcData.getDataFields().containsKey("913")) {
                marcData.getDataFields()
                        .remove("913");
            }
            Element record = MarcDataConverter.generateMarcXmlAuthority(marcData);
            marcDocument = new Document(record);
            database.removeGndId(selectedDatabaseEntry.getId(), marcDocument.toXML(), selectedDatabaseEntry.getGndId());
        } catch (ParsingException | IOException e) {
            LOG.error(e.getMessage());
        }
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/index.xhtml");
    }

    public void create() throws IOException {
        authorityMarcData = selectedMarcData;
        setRenderNotification(false);
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/mae.xhtml");
    }

    public void saveInGnd() throws IOException {
        String action = "create";
        if (authorityMarcData.getGndId() != null &&
                !authorityMarcData.getGndId().isEmpty()) {
            action = "update";
        }
        try {
            String token = TokenUtils.generateTokenString(pathToPrivateKey,
                    "gressho",
                    "gressho@uni-muenster.de",
                    "/privateKey.pem");
            String actionUrl = switch (action) {
                case "update" -> authoritiesUpdateServiceUpdate;
                case "create" -> authoritiesUpdateServiceCreate;
                default -> authoritiesUpdateServiceUpdate;
            };
            Uni<JsonObject> result = updateClient.post(actionUrl)
                    .bearerTokenAuthentication(token)
                    .sendJson(authorityMarcData)
                    .onItem()
                    .transform(bufferHttpResponse -> {
                        if (bufferHttpResponse.statusCode() == 200) {
                            return bufferHttpResponse.bodyAsJsonObject();
                        } else {
                            MultiMap headers = bufferHttpResponse.headers();
                            headers.names()
                                    .forEach(header -> {
                                        LOG.info(header + ": " + headers.get(header));
                                    });
                            return new JsonObject()
                                    .put("code", bufferHttpResponse.statusCode())
                                    .put("message", bufferHttpResponse.getHeader("etag"));
                        }
                    });
            JsonObject resultObject = result.await()
                    .indefinitely();
            //Achtung: Im SRU der DNB ist ein Bug: das geänderte marcxml-Dokument wird komplett
            //ohne Namespace ausgeliefert, so dass das Parsen nicht funktioniert!

            Integer id = null;
            LOG.debug("Response: " + resultObject.toString());
            if (resultObject.containsKey("code")) {
                setRenderNotification(true);
                setNotification(resultObject.getString("message"));
                pushContext.send("updateNotification");
                LOG.error(resultObject.getString("message"));
            } else {
                MarcData marcData = resultObject.mapTo(MarcData.class);
                String firstName = null;
                String lastName = null;
                if (marcData.getDataFields().containsKey("100") &&
                        !marcData.getDataFields().get("100").isEmpty()) {
                    DataField field100 = marcData.getDataFields()
                            .get("100")
                            .get(0);
                    if (field100.getSubFields().containsKey("a") &&
                            !field100.getSubFields().get("a").isEmpty()) {
                        String fullName = field100.getSubFields()
                                .get("a")
                                .get(0)
                                .getValue();
                        if (fullName.contains(",")) {
                            String[] splitted = fullName.split(",");
                            lastName = splitted[0];
                            firstName = splitted[1];
                        } else {
                            lastName = fullName;
                        }
                    }
                }
                if (marcData.getDataFields().containsKey("670") &&
                        !marcData.getDataFields().get("670").isEmpty()) {
                    for (DataField field670 : marcData.getDataFields().get("670")) {
                        if (field670.getSubFields().containsKey("u") &&
                                !field670.getSubFields().get("u").isEmpty()) {
                            for (SubField subFieldU : field670.getSubFields().get("u")) {
                                if (subFieldU.getValue().startsWith("http://www.pacelli-edition.de/Biographie/")) {
                                    id = Integer.parseInt(subFieldU.getValue()
                                            .substring("http://www.pacelli-edition.de/Biographie/".length()));
                                    break;
                                }
                            }
                        }
                        if (id != null) {
                            break;
                        }
                    }
                }
                String gndId = marcData.getControlFields().get("001");
                database.updateMarcEntry(id, firstName, lastName,
                        gndId, MarcDataConverter.generateMarcXmlAuthority(marcData).toXML(),
                        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
        } finally {
            selectedDatabaseEntry = null;
            selectedMarcData = null;
        }
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/xhtml/index.xhtml");
    }

    public DatabaseEntry getSelectedDatabaseEntry() {
        return selectedDatabaseEntry;
    }

    public void setSelectedDatabaseEntry(DatabaseEntry selectedDatabaseEntry) {
        this.selectedDatabaseEntry = selectedDatabaseEntry;
    }

    public DatabaseEntry getSelectedEditedDatabaseEntry() {
        return selectedEditedDatabaseEntry;
    }

    public void setSelectedEditedDatabaseEntry(DatabaseEntry selectedEditedDatabaseEntry) {
        this.selectedEditedDatabaseEntry = selectedEditedDatabaseEntry;
    }

    public MarcData getSelectedAuthority() {
        return selectedAuthority;
    }

    public void setSelectedAuthority(MarcData selectedAuthority) {
        this.selectedAuthority = selectedAuthority;
    }

    public MarcData getAuthorityMarcData() {
        return authorityMarcData;
    }

    public MarcData getSelectedMarcData() {
        return selectedMarcData;
    }

    public String getDirectGndId() {
        return directGndId;
    }

    public void setDirectGndId(String directGndId) {
        this.directGndId = directGndId;
    }
}
