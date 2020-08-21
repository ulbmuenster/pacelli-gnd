/*
 * This file is part of authority-editor.
 * Copyright (C) 2020 Universitäts- und Landesbibliothek Münster.
 *
 * authority-editor is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License; see LICENSE file for more details.
 */
package de.wwu.ulb.jwt;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import org.eclipse.microprofile.jwt.Claims;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class TokenUtils {

    private TokenUtils() {
    }

    public static String generateTokenString(String privateKeyFileName, String preferredUserName,
                                             String upn, String kid) throws Exception {
        PrivateKey privateKey = readPrivateKey(privateKeyFileName);

        JwtClaimsBuilder claims = Jwt.claims("/token.json");
        long currentTimeInSecs = currentTimeInSecs();
        long exp = currentTimeInSecs + 300;

        claims.issuedAt(currentTimeInSecs)
                .preferredUserName(preferredUserName)
                .upn(upn)
                .claim(Claims.auth_time.name(), currentTimeInSecs)
                .expiresAt(exp);

        return claims.jws().signatureKeyId(kid)
                .sign(privateKey);
    }

    /**
     * Read a PEM encoded private key
     * @param pemResName - key file name
     * @return PrivateKey
     * @throws Exception on decode failure
     */
    private static PrivateKey readPrivateKey(String pemResName) throws Exception {
        InputStream contentIS = new FileInputStream(pemResName);
        byte[] tmp = new byte[4096];
        int length = contentIS.read(tmp);
        PrivateKey privateKey = decodePrivateKey(new String(tmp, 0, length));
        return privateKey;
    }

    /**
     * Decode a PEM encoded private key string to an RSA PrivateKey
     * @param pemEncoded - PEM string for private key
     * @return PrivateKey
     * @throws Exception on decode failure
     */
    private static PrivateKey decodePrivateKey(String pemEncoded) throws Exception {
        pemEncoded = removeBeginEnd(pemEncoded);
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(pemEncoded);

        // extract the private key

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(keySpec);
        return privKey;
    }

    private static String removeBeginEnd(String pem) {
        pem = pem.replaceAll("-----BEGIN (.*)-----", "");
        pem = pem.replaceAll("-----END (.*)----", "");
        pem = pem.replaceAll("\r\n", "");
        pem = pem.replaceAll("\n", "");
        return pem.trim();
    }

    /**
     * @return the current time in seconds since epoch
     */
    private static int currentTimeInSecs() {
        long currentTimeMS = System.currentTimeMillis();
        int currentTimeSec = (int) (currentTimeMS / 1000);
        return currentTimeSec;
    }

}
