/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

/*
 * This class simulates and OAuth 2.0 suthorization endpoint. It is meant for demo purposes
 *
 */
package net.loginbuddy.demoserver.provider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Pkce;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginbuddyProviderAuthorize extends LoginbuddyProviderCommon {

    private static final Logger LOGGER = Logger.getLogger(LoginbuddyProviderAuthorize.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {

            // Handle the PAR request (just check if the given clientId matches the initial one, otherwise ... this is just for demo purposes)
            String requestUri = request.getParameter(Constants.REQUEST_URI.getKey());
            if (requestUri != null) {
                SessionContext sessionContext = (SessionContext) LoginbuddyCache.CACHE.remove(requestUri.replaceAll("urn:loginbuddy-samples:", ""));
                if (sessionContext == null) {
                    throw new IllegalArgumentException("The given redirect_uri is invalid or has expired");
                }
                String clientId = (String) sessionContext.get(Constants.CLIENT_ID.getKey());
                if (clientId != null && clientId.equals(request.getParameter(Constants.CLIENT_ID.getKey()))) {
                    LoginbuddyCache.CACHE.put(sessionContext.getId(), sessionContext);
                    // forward to a fake login page
                    request.getRequestDispatcher(String.format("demoserverUsername.jsp?session=%s%s", sessionContext.getId(), LoginbuddyCache.CACHE.remove(requestUri))).forward(request, response);
                } else {
                    throw new IllegalArgumentException("The given client_id is invalid. It should match the one used with the initial PAR request");
                }
            } else {

                /*
                 * All the parameters below need to be validated. But, since this is just for demo purposes, they are mostly used 'as is'.
                 * In the future this could be turned into a 'real' /authorization endpoint.
                 *
                 */
                String nonce = request.getParameter(Constants.NONCE.getKey());
                if (nonce == null || nonce.trim().length() == 0) {
                    throw new IllegalArgumentException("The nonce is invalid or missing");
                }

                String state = request.getParameter(Constants.STATE.getKey());
                if (state == null || state.trim().length() == 0) {
                    throw new IllegalArgumentException("The state is invalid or missing");
                }

                // Currently only response_type=code is supported. Let's check for that to simulate a more realistic behaviour
                String response_type = request.getParameter(Constants.RESPONSE_TYPE.getKey());
                if (!"code".equals(response_type)) {
                    throw new IllegalArgumentException("The given response_type is not supported");
                }

                // TODO: Validate if this is a valid client_id
                String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
                if (clientId == null || clientId.trim().length() == 0) {
                    throw new IllegalArgumentException("The client_id is missing");
                }

                // TODO: Validate if the SCOPE for this client is valid
                String scope = request.getParameter(Constants.SCOPE.getKey());
                if (scope == null || scope.trim().length() == 0) {
                    throw new IllegalArgumentException("The scope is missing");
                }

                String code_challenge = request.getParameter(Constants.CODE_CHALLENGE.getKey());
                if (!Pkce.verifyChallenge(code_challenge)) {
                    throw new IllegalArgumentException("Invalid code_challenge");
                }

                // We always require S256
                String code_challenge_method = request.getParameter(Constants.CODE_CHALLENGE_METHOD.getKey());
                if (!Pkce.CODE_CHALLENGE_METHOD_S256.equalsIgnoreCase(code_challenge_method)) {
                    throw new IllegalArgumentException("The given code_challenge_method is not supported!");
                }

                // TODO: Validate the redirect_uri to be one registered for the client_id.
                String redirectUri = request.getParameter(Constants.REDIRECT_URI.getKey());
                if (redirectUri == null || redirectUri.trim().length() == 0 || !redirectUri.startsWith(scheme)) {
                    throw new IllegalArgumentException(String.format("The given redirect_uri is not valid. '%s' schema is not supported!", scheme));
                }

                String loginHint = request.getParameter(Constants.LOGIN_HINT.getKey());
                if (loginHint != null && loginHint.trim().length() <= 24 && !loginHint.equalsIgnoreCase("null")) {
                    loginHint = String.format("&login_hint=%s", URLEncoder.encode(loginHint, StandardCharsets.UTF_8));
                } else {
                    loginHint = "";
                }

                String authorizationDetails = request.getParameter(Constants.AUTHORIZATION_DETAILS.getKey());
                if (authorizationDetails == null || authorizationDetails.equalsIgnoreCase("null")) {
                    authorizationDetails = "";
                } else {
                    try {
                        new JSONParser().parse(authorizationDetails);
                    } catch(ParseException e) {
                        LOGGER.warning(String.format("The given authorization_details is not JSON: %s", e.getMessage()));
                        authorizationDetails = "";
                    }
                }


                // Need to remember all these values for the current session
                SessionContext sessionContext = new SessionContext();
                sessionContext.sessionInit(
                        clientId,
                        scope,
                        response_type,
                        code_challenge,
                        code_challenge_method,
                        redirectUri,
                        nonce,
                        state,
                        authorizationDetails);

                // not by spec. required here, but if the token_type is dpop, let's check for the parameter anyways ...
                if ("dpop".equalsIgnoreCase(tokenType)) {
                    if (request.getParameter("dpop_jkt") == null) {
                        throw new IllegalArgumentException("I require DPoP, and with that, dpop_jkt as a query parameter");
                    } else {
                        sessionContext.put("dpop_jkt", request.getParameter("dpop_jkt"));
                        LOGGER.info(String.format("Received dpop_jkt: %s", request.getParameter("dpop_jkt")));
                    }
                }

                LoginbuddyCache.CACHE.put(sessionContext.getId(), sessionContext);

                // forward to a fake login page
                request.getRequestDispatcher(String.format("demoserverUsername.jsp?session=%s%s", sessionContext.getId(), loginHint)).forward(request, response);

            }
        } catch (Exception e) {
            LOGGER.warning(String.format("The authorization request was invalid: %s", e.getMessage()));
            response.setStatus(404);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().println(
                    "\"error\":\"invalid_Request\", \"error_description\": \"the authorization request was invalid due to missing or invalid parameters!\"");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // TODO: since this is fake, we do not really validate the incoming parameters ... just assuming they are available ... exactly once ... otherwise, add validation!

        String path = request.getRequestURL().toString();
        String sessionId = request.getParameter("session");
        sessionId = "null".equals(sessionId) ? null : sessionId; // for some reason we get "null"
        String action = request.getParameter("action");

        SessionContext sessionContext = null;
        if (sessionId != null && sessionId.trim().length() > 0) {
            sessionContext = (SessionContext) LoginbuddyCache.CACHE.remove(sessionId);
            if (sessionContext == null) {
                LOGGER.warning("Unknown or expired session!");
                response.sendError(400, "Unknown or expired session!");
                return;
            } else {
                // let's check if the given session is the one associated with the sessionValues
                String sessionState = sessionContext.getId();
                if (!sessionId.equals(sessionState)) {
                    LOGGER.warning("Sessions at FakeProvider are mixed up!");
                    response.sendError(400, "Sessions at FakeProvider are mixed up!");
                    return;
                }
                // let's check if the given action is the one we expect (unless it is 'cancel')
                String actionExpected = sessionContext.getString(Constants.ACTION_EXPECTED.getKey());
                if (!actionExpected.equals(action) && !"cancel".equalsIgnoreCase(action)) {
                    LOGGER.warning(
                            "The current action was not expected! Given: '" + action + "', expected: '" + actionExpected + "'");
                    response.sendError(400, "The current action was not expected!");
                    return;
                }
            }
        } else {
            LOGGER.warning("Invalid or missing session parameter!");
            response.sendError(400, "Invalid or missing session parameter!");
            return;
        }

        try {
            // Handle the provided 'email address'. In a real life scenario it would have to be validated
            if (path.endsWith("/login")) {
                String email = request.getParameter("email");
                if ("login".equalsIgnoreCase(action)) {
                    if (email != null && email.trim().length() > 0) {
                        // add the email to the current session, but also check if it is the expected one
                        sessionContext.sessionLoginProvided("email", email);
                        LoginbuddyCache.CACHE.put(sessionId, sessionContext);
                        request.getRequestDispatcher("demoserverAuthenticate.jsp?session=" + sessionId).forward(request, response);
                    } else {
                        // TODO: else { ... return an error and request the email-address or allow to cancel ... }
//            sessionValues.put("failedLoginAttempt", new Date().getTime());
//            LoginbuddyCache.CACHE.put(sessionValues.getId(), sessionValues);
                    }
                } else if ("cancel".equalsIgnoreCase(action)) {

                    String clientRedirectUri = sessionContext.getString(Constants.REDIRECT_URI.getKey());
                    String clientState = sessionContext.getString(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&error=login_cancelled&error_description=the+resource_owner+cancelled+the+login+process";
                    response.sendRedirect(clientRedirectUri);
                }
            } else if (path.endsWith("/authenticate")) {
                String password = request.getParameter("password");
                if ("authenticate".equalsIgnoreCase(action)) {
                    if (password != null && password.trim().length() > 0) {
                        sessionContext.sessionAuthenticated();
                        LoginbuddyCache.CACHE.put(sessionId, sessionContext);
                        request.getRequestDispatcher("demoserverConsent.jsp?session=" + sessionId).forward(request, response);
                    } else {
                        // TODO: else { ... return an error and request the password or allow to cancel ... }

                    }
                } else if ("cancel".equalsIgnoreCase(action)) {

                    String clientRedirectUri = sessionContext.getString(Constants.REDIRECT_URI.getKey());
                    String clientState = sessionContext.getString(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&error=login_cancelled&error_description=the+resource_owner+cancelled+the+authentication+process";
                    response.sendRedirect(clientRedirectUri);
                }
            } else if (path.endsWith("/consent")) {
                if ("grant".equals(action)) {

                    // now it is time to issue an authorization code
                    String code = UUID.randomUUID().toString();

                    sessionContext.sessionGranted();
                    LoginbuddyCache.CACHE.put(code, sessionContext);

                    String clientRedirectUri = sessionContext.getString(Constants.REDIRECT_URI.getKey());
                    String clientState = sessionContext.getString(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&code=" + URLEncoder.encode(code, "UTF-8");

                    response.sendRedirect(clientRedirectUri);
                } // TODO: else { ... return an error and request the consent or allow to cancel ... }
                else if ("cancel".equalsIgnoreCase(action)) {

                    String clientRedirectUri = sessionContext.get(Constants.REDIRECT_URI.getKey(), String.class);
                    String clientState = sessionContext.get(Constants.STATE.getKey(), String.class);

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&error=access_denied&error_description=the+resource_owner+denied_access";
                    response.sendRedirect(clientRedirectUri);
                }
            } else {
                LOGGER.warning("Unknown API was called!");
                response.sendError(400, "Unknown API was called!");
            }
        } catch (ServletException e) {
            LOGGER.warning(String.format("Something in the FakeProvider went badly wrong! %s", e.getMessage()));
            response.sendError(500, "Something in the FakeProvider went badly wrong!");
        }
    }
}