/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.demoserver.provider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.Pkce;
import org.jose4j.jws.JsonWebSignature;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginbuddyProviderToken extends LoginbuddyProviderCommon {

    private static final Logger LOGGER = Logger.getLogger(LoginbuddyProviderToken.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "POST");
        response.sendError(405, "Method not allowed");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        JSONObject errorResponse = new JSONObject();
        JSONObject fakeProviderResponse = new JSONObject();

        // not by spec. required here, but if the token_type is dpop, let's check for the parameter and the header anyways ...
        if ("dpop".equalsIgnoreCase(tokenType)) {
            if (checkForDpop(request, response)) return;
            if (checkForDpopNonce(request, response)) return;
        }

        // TODO: Handle multiple grant_types. But, since this is all fake, we'll just support 'authorization_code'
        String grant_type = request.getParameter(Constants.GRANT_TYPE.getKey());
        if (!("authorization_code".equalsIgnoreCase(grant_type) || "refresh_token".equalsIgnoreCase(grant_type))) {
            LOGGER.warning("The given grant_type is not supported or the parameter is missing");
            errorResponse.put("error_description", "The given grant_type is not supported or the parameter is missing");
            errorResponse.put("error", "invalid_request");
            response.setStatus(400);
            response.getWriter().write(errorResponse.toJSONString());
            return;
        }

        String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
        if(clientId == null) {
            LOGGER.warning("Missing client_id");
            errorResponse.put("error_description", "missing client_id");
            errorResponse.put("error", "invalid_request");
            response.setStatus(400);
            response.getWriter().write(errorResponse.toJSONString());
            return;
        }

        // TODO: Validate the client_secret. But, since this is all fake, we'll just check if it exists
        if (request.getParameter(Constants.CLIENT_SECRET.getKey()) == null) {
            LOGGER.warning("The client_secret is missing");
            errorResponse.put("error_description", "The client_secret is missing");
            errorResponse.put("error", "invalid_request");
            response.setStatus(400);
            response.getWriter().write(errorResponse.toJSONString());
            return;
        }

        SessionContext sessionContext = null;

        long now = nowInSeconds();
        String atJti = UUID.randomUUID().toString();

        JSONObject accessTokenPayload = new JSONObject();
        accessTokenPayload.put(Constants.ISSUER.getKey(), location_demoserver);
        accessTokenPayload.put("iat", now);
        accessTokenPayload.put("exp", now + 600);
        accessTokenPayload.put("jti", atJti);
        accessTokenPayload.put("aud", location_demoserver);

        JSONObject refreshTokenPayload = new JSONObject();
        refreshTokenPayload.put(Constants.ISSUER.getKey(), location_demoserver);
        refreshTokenPayload.put("iat", now);
        refreshTokenPayload.put("exp", now + 3600);
        refreshTokenPayload.put("jti", UUID.randomUUID().toString());
        refreshTokenPayload.put("aud", location_demoserver);
        refreshTokenPayload.put("at_jti", atJti);

        String givenRefreshTokenJwt = request.getParameter("refresh_token");
        String id_token = null; // only created for authorization_code flow

        if (Constants.REFRESH_TOKEN.getKey().equalsIgnoreCase(grant_type)) {

            if (givenRefreshTokenJwt == null) {
                LOGGER.warning("Missing refresh_token");
                errorResponse.put("error_description", "Missing refresh_token");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }
            JSONObject givenRefreshTokenPayload = null;
            try {
                givenRefreshTokenPayload = (JSONObject) new JSONParser().parse(new String(Base64.getDecoder().decode(givenRefreshTokenJwt.split("[.]")[1])));
            } catch (Exception e) {
                LOGGER.warning(String.format("Invalid refresh_token token format: %s", givenRefreshTokenJwt));
                errorResponse.put("error_description", "Invalid refresh_token token format");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }

            // this is null if the token expiration is in the past
            sessionContext = (SessionContext) LoginbuddyCache.CACHE.get(givenRefreshTokenJwt);
            if(sessionContext == null) {
                LOGGER.warning("The given refresh_token has expired");
                errorResponse.put("error_description", "The given refresh_token has expired");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }

            String clientIdRefreshToken = (String) givenRefreshTokenPayload.get("client_id");
            String scopeRefreshTokenSession = sessionContext.getString("scope");

            if (! (clientId.equalsIgnoreCase(clientIdRefreshToken) && clientId.equalsIgnoreCase(sessionContext.getString(Constants.CLIENT_ID.getKey())))) {
                LOGGER.warning(String.format("The client is invalid for this refresh_token. client_id_request: %s, client_id_refresh_token: %s", clientId, clientIdRefreshToken));
                errorResponse.put("error_description", "The client is invalid for this refresh_token");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }

            String scopeRequest = request.getParameter("scope");
            if (scopeRequest != null && !scopeRequest.equalsIgnoreCase(scopeRefreshTokenSession)) {
                LOGGER.warning(String.format("The requested scope is not valid. scope_request: %s, scope_refresh_token: %s", scopeRequest, scopeRefreshTokenSession));
                errorResponse.put("error_description", "The requested scope is not valid");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }
            accessTokenPayload.put("scope", scopeRefreshTokenSession);
            refreshTokenPayload.put("scope", scopeRefreshTokenSession);

            fakeProviderResponse.put("scope", scopeRefreshTokenSession);

            LoginbuddyCache.CACHE.remove(givenRefreshTokenJwt); // the refresh_token is valid only once

        } else if ("authorization_code".equalsIgnoreCase(grant_type)) {

            String code = request.getParameter(Constants.CODE.getKey());
            if(code == null) {
                LOGGER.warning("Missing authorization_code");
                errorResponse.put("error_description", "Missing authorization_code");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }

            // find the session and fail if it is unknown
            sessionContext = (SessionContext) LoginbuddyCache.CACHE.remove(code);
            if (sessionContext == null) {
                LOGGER.warning("The given authorization_code is invalid or has expired or none was given");
                errorResponse.put("error_description", "The given authorization_code is invalid or has expired or none was given");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }

            // Need to check if the given clientId is the one associated with the given authorization_code
            if (!clientId.equals(sessionContext.get(Constants.CLIENT_ID.getKey()))) {
                LOGGER.warning(String.format("The given client_id is not valid for the given authorization_code: %s", clientId));
                errorResponse.put("error_description", "The given client_id is not valid for the given authorization_code");
                errorResponse.put("error", "invalid_request");
                response.setStatus(400);
                response.getWriter().write(errorResponse.toJSONString());
                return;
            }

            // Validate 'code_verifier' if PKCE was used (which is the default for loginbuddy)
            String code_challenge = sessionContext.getString(Constants.CODE_CHALLENGE.getKey());
            if (code_challenge != null) {
                String code_verifier = request.getParameter(Constants.CODE_VERIFIER.getKey());
                if (code_verifier == null || "".equals(code_verifier.trim()) || request.getParameterValues(Constants.CODE_VERIFIER.getKey()).length > 1) {
                    LOGGER.warning("Missing code_verifier");
                    errorResponse.put("error_description", "Missing code_verifier");
                    errorResponse.put("error", "invalid_request");
                    response.setStatus(400);
                    response.getWriter().write(errorResponse.toJSONString());
                    return;
                } else {
                    if (!Pkce.validate(code_challenge, sessionContext.getString(Constants.CODE_CHALLENGE_METHOD.getKey()), code_verifier)) {
                        LOGGER.warning("The given code_verifier is invalid");
                        errorResponse.put("error_description", "The given code_verifier is invalid");
                        errorResponse.put("error", "invalid_request");
                        response.setStatus(400);
                        response.getWriter().write(errorResponse.toJSONString());
                        return;
                    }
                }
            }

            if ("dpop".equalsIgnoreCase(tokenType)) {
                String dpopJkt = sessionContext.getString("dpop_jkt");
                if (!compareJkt(request, dpopJkt)) {
                    LOGGER.warning("JKT values do not match");
                    errorResponse.put("error_description", "The dpop_jkt does not match the calculated one of the dpop-proof");
                    errorResponse.put("error", "invalid_request");
                    response.setStatus(400);
                    response.getWriter().write(errorResponse.toJSONString());
                    return;
                }
                try {
                    JSONObject cnf = new JSONObject();
                    cnf.put("jkt", getJwkJkt(request));
                    accessTokenPayload.put("cnf", cnf);
                } catch(Exception e) {
                    LOGGER.warning(String.format("Could not add cnf.jkt: %s", e.getMessage()));
                }
            }

            try {
                id_token = Jwt.DEFAULT.createSignedJwtRs256(location_demoserver,
                        sessionContext.getString("client_id"),
                        5,
                        getSub(sessionContext.getString("client_id"), sessionContext.getString("email"), false),
                        sessionContext.getString("nonce"),
                        false).getCompactSerialization();
            } catch (Exception e) {
                LOGGER.warning(String.format("Could not create id_token: %s", e.getMessage()));
            }

            fakeProviderResponse.put("id_token", id_token);
            fakeProviderResponse.put("scope", sessionContext.get(Constants.SCOPE.getKey()));

        } else {
            LOGGER.warning(String.format("The given grant_type is not supported: %s", grant_type));
            errorResponse.put("error_description", "The given grat_type is not supported");
            errorResponse.put("error", "invalid_request");
            response.setStatus(400);
            response.getWriter().write(errorResponse.toJSONString());
            return;
        }

        accessTokenPayload.put("sub", getSub(clientId, sessionContext.getString("email"), false));
        refreshTokenPayload.put("sub", getSub(clientId, sessionContext.getString("email"), false));

        accessTokenPayload.put(Constants.CLIENT_ID.getKey(), clientId);
        refreshTokenPayload.put(Constants.CLIENT_ID.getKey(), clientId);

        accessTokenPayload.put(Constants.TOKEN_TYPE.getKey(), tokenType);
        refreshTokenPayload.put(Constants.TOKEN_TYPE.getKey(), tokenType);

        try {
            JsonWebSignature accessTokenJws = Jwt.DEFAULT.createSignedJwt(accessTokenPayload.toJSONString(), JwsAlgorithm.RS256);
            accessTokenJws.setHeader("typ", "at+jwt");
            String access_token = accessTokenJws.getCompactSerialization();

            JsonWebSignature refreshTokenJws = Jwt.DEFAULT.createSignedJwt(refreshTokenPayload.toJSONString(), JwsAlgorithm.RS256);
            refreshTokenJws.setHeader("typ", "rt+jwt");
            String refresh_token = refreshTokenJws.getCompactSerialization();

            long accessTokenLifetime = sessionContext.sessionToken(access_token, refresh_token, id_token);

            LoginbuddyCache.CACHE.put(access_token.split("[.]")[2], sessionContext, accessTokenLifetime);
            LoginbuddyCache.CACHE.put(refresh_token, sessionContext, sessionContext.get("refresh_token_lifetime", Long.class));

            fakeProviderResponse.put(Constants.ACCESS_TOKEN.getKey(), access_token);
            fakeProviderResponse.put(Constants.REFRESH_TOKEN.getKey(), refresh_token);
            fakeProviderResponse.put(Constants.TOKEN_TYPE.getKey(), tokenType);
            fakeProviderResponse.put(Constants.EXPIRES_IN.getKey(), accessTokenLifetime);

            response.setStatus(200);
            response.getWriter().println(fakeProviderResponse);
        } catch(Exception e) {
            LOGGER.warning(String.format("server_error: %s", e.getMessage()));
            errorResponse.put("error_description", "Something unexpected happened, please try again");
            errorResponse.put("error", "server_error");
            response.setStatus(400);
            response.getWriter().write(errorResponse.toJSONString());
        }
    }
}