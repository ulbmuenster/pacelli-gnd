/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
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
import de.wwu.ulb.mae.client.AuthoritySruSearchClient;
import de.wwu.ulb.mae.client.AuthoritySruUpdateClient;
import de.wwu.ulb.mae.model.DatabaseEntry;
import de.wwu.ulb.mae.model.LazyAuthorityModel;
import de.wwu.ulb.mae.model.LazyDatabaseEntryModel;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
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
    @RestClient
    AuthoritySruSearchClient authoritySruSearchClient;

    @Inject
    @RestClient
    AuthoritySruUpdateClient authoritySruUpdateClient;

    @Inject
    Database database;

    private Builder builder;

    private Jsonb jsonb;

    private LazyDataModel<DatabaseEntry> lazyModel;

    private LazyDataModel<DatabaseEntry> lazyEditedModel;

    private LazyDataModel<MarcData> lazyAuthorityModel;

    private DatabaseEntry selectedDatabaseEntry;

    private DatabaseEntry selectedEditedDatabaseEntry;

    private MarcData selectedMarcData;

    private MarcData selectedAuthority;

    private MarcData authorityMarcData;

    private String directGndId;

    @Inject
    SecurityIdentity securityIdentity;

    @PostConstruct
    public void init() {
        lazyModel = new LazyDatabaseEntryModel(database, false);
        lazyEditedModel = new LazyDatabaseEntryModel(database, true);
        jsonb = JsonbBuilder.create();
        builder = new Builder();
        UserInfo userInfo = (UserInfo) securityIdentity.getAttribute("userinfo");
        LOG.info(userInfo.getString("sub"));
        LOG.info(userInfo.getString("email"));
        LOG.info(userInfo.getString("username"));
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
                    authoritySruSearchClient, marcEntry.getFirstName(), marcEntry.getLastName());
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
            LOG.debug("Psrserausnahme!");
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
                    authoritySruSearchClient, marcEntry.getFirstName(), marcEntry.getLastName());
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
        AuthoritySearchResponse authoritySearchResponse = authoritySruSearchClient.searchAuthority(
                selected.getGndId(), SearchIndex.Identifier, 0, 1);
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
        AuthoritySearchResponse authoritySearchResponse = authoritySruSearchClient.searchAuthority(
                directGndId, SearchIndex.Identifier, 0, 1);
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
                                    if (dataField.getSubFields().containsKey("a")) {
                                        String gndLevel = dataField.getSubFields()
                                                .get("a")
                                                .get(0)
                                                .getValue();
                                        int level = Integer.parseInt(gndLevel.substring(3));
                                        authorityMarcData.getDataFields()
                                                .get("042")
                                                .forEach(authority -> {
                                                    if (authority.getSubFields().containsKey("a")) {
                                                        String authorityGndLevel = authority.getSubFields()
                                                                .get("a")
                                                                .get(0)
                                                                .getValue();
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
                                                    }
                                                });
                                    }
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
        Response response = null;
        try {
            String token = TokenUtils.generateTokenString(pathToPrivateKey,
                    "gressho",
                    "gressho@uni-muenster.de",
                    "/privateKey.pem");
            switch (action) {
                case "update":
                    response = authoritySruUpdateClient.updateAuthority(
                            "Bearer " + token,
                            authorityMarcData);
                    break;
                case "create":
                    System.out.println(jsonb.toJson(authorityMarcData));
                    response = authoritySruUpdateClient.createAuthority(
                            "Bearer " + token,
                            authorityMarcData);
                    break;
            }

            //Achtung: Im SRU der DNB ist ein Bug: das geänderte marcxml-Dokument wird komplett
            //ohne Namespace ausgeliefert, so dass das Parsen nicht funktioniert!

            Integer id = null;
            LOG.debug("Responsestatus: " + response.getStatus());
            if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                EntityTag eTag = response.getEntityTag();
                String diagnose = eTag.getValue();
                setRenderNotification(true);
                setNotification(diagnose);
                pushContext.send("updateNotification");
                LOG.error(diagnose);
            }
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                MarcData marcData = response.readEntity(MarcData.class);
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
