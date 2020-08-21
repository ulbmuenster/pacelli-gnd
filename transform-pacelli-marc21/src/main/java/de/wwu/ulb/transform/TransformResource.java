/*
 * This file is part of transform-pacelli-marc21 (tpm).
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * transform-pacelli-marc21 is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.transform;

import de.wwu.ulb.transform.marc.Marc21Builder;
import de.wwu.ulb.transform.model.AuthoritiesData;
import nu.xom.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class TransformResource {

    final String unknown = "n.e.";

    @Inject
    AuthoritiesDatabase authoritiesDatabase;

    @Inject
    Marc21Builder marc21Builder;

    public boolean startTransformation() {
        int offset = 0;
        final int limit = 500;
        int sizeOfResultSet = 0;
        do {
            List<AuthoritiesData> authoritiesDataList = authoritiesDatabase.findPacelliMetadata(limit, offset);
            sizeOfResultSet = authoritiesDataList.size();
            offset += limit;
            authoritiesDataList.stream()
                    .filter(authoritiesData ->
                            !authoritiesData.getDateOfBirth().equals(unknown) ||
                                    !authoritiesData.getDateOfDeath().equals(unknown))
                    .filter(authoritiesData ->
                            authoritiesData.getLastName() != null &&
                                    (!authoritiesData.getLastName().equals("N.N.") ||
                                    !authoritiesData.getLastName().equals("N. N.")))
                    .forEach(authoritiesData -> {
                        Document marc21 = marc21Builder.buildMarc21(authoritiesData);
                        authoritiesDatabase.updateMarc21(marc21, authoritiesData.getAuthoritiesId());
                    });
        } while (sizeOfResultSet > 0);
        return true;
    }
}
