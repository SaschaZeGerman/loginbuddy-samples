/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.demoserver.provider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LoginbuddyProviderUserinfo extends LoginbuddyProviderCommon {

    private static final Logger LOGGER = Logger.getLogger(LoginbuddyProviderUserinfo.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Whatever happens, we'll return JSON
        response.setContentType("application/json");
        JSONObject errorResponse = new JSONObject();
        JSONObject fakeUserinfoResponse = new JSONObject();

        // do we have an error?
        boolean error = false;

        // not by spec. required here, but if the token_type is dpop, let's check for the parameter and the header anyways ...
        if ("dpop".equalsIgnoreCase(tokenType)) {
            if(checkForDpopProtected(request, response)) return;
            if(checkForDpopNonceProtected(request, response)) return;
        }

        // TODO: Handle access_token validation including RFC specific error responses. Well, this is all fake, so ... do not worry about it now
        String access_token = request.getParameter(Constants.ACCESS_TOKEN.getKey());
        if (access_token == null || access_token.trim().length() == 0) {
            // not found, let's check the authorization header
            String authHeader = request.getHeader(Constants.AUTHORIZATION.getKey());
            if (authHeader != null && Stream.of(authHeader.split(" ")).anyMatch(tokenType::equalsIgnoreCase)) {
                access_token = authHeader.split(" ")[1];
            } else {
                LOGGER.warning("the access_token is missing");
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "the access_token is missing");
                error = true;
            }
        }

        SessionContext sessionContext = null;
        if (!error) {
            // Let's see if we know this access_token
            sessionContext = (SessionContext) LoginbuddyCache.CACHE.get(access_token.split("[.]")[2]);
            if (sessionContext == null || !access_token.equals(sessionContext.get(Constants.ACCESS_TOKEN.getKey()))) {
                LOGGER.warning("the access_token is invalid");
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "the access_token is invalid");
                error = true;
            }
        }

        if(!error && "dpop".equalsIgnoreCase(tokenType)) {
            String dpopJkt = sessionContext.getString("dpop_jkt");
            if(!compareJkt(request, dpopJkt)) {
                LOGGER.warning("JKT values do not match");
                errorResponse.put("error_description", "The jkt does not match the calculated one of the dpop-proof");
                errorResponse.put("error", "invalid_request");
                error = true;
            }
            if(!compareJktCnf(request, dpopJkt)) {
                LOGGER.warning("JKT values do not match");
                errorResponse.put("error_description", "The cnf-jkt does not match the calculated one of the dpop-proof");
                errorResponse.put("error", "invalid_request");
                error = true;
            }
        }

        if (!error) {
            // Check if the access_token has not expired yet
            long expiration = sessionContext.getLong("access_token_expiration");
            if (nowInSeconds() > expiration) {
                LOGGER.warning("the given access_token has expired");
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "the given access_token has expired");
                error = true;
        }}

        String scope = "";
        if (!error) {
            // Check for at least scope 'openid'
            scope = sessionContext.getString(Constants.SCOPE.getKey());
            if (Stream.of(scope.split(" ")).noneMatch("openid"::equals)) {
                LOGGER.warning("The given access_token has not been granted to access this API");
                errorResponse.put("error", "invalid_request");
                errorResponse.put("error_description", "The given access_token has not been granted to access this API");
                error = true;
            }
        }

        if (!error) {

            // Let's build the response message depending on scope values other than 'openid'

            String clientId = sessionContext.getString(Constants.CLIENT_ID.getKey());
            String email = sessionContext.getString("email");
            String sub = getSub(clientId, email, false);

            fakeUserinfoResponse = new JSONObject();
            fakeUserinfoResponse.put("sub", sub);

            // add 'email' if it was requested in scope
            if (Stream.of(scope.split(" ")).anyMatch("email"::equals)) {
                fakeUserinfoResponse.put("email", email);
                fakeUserinfoResponse.put("email_verified", true);
            }

            // add 'profile' if it was requested in scope
            if (Stream.of(scope.split(" ")).anyMatch("profile"::equals)) {
                fakeUserinfoResponse.put("name", "Login Buddy");
                fakeUserinfoResponse.put("given_name", "Login");
                fakeUserinfoResponse.put("family_name", "Buddy");
                fakeUserinfoResponse.put("preferred_username", email);
            }
        }

        if(error) {
            response.setStatus(401);
            response.getWriter().println(errorResponse.toJSONString());
        }
        response.setStatus(200);
        response.getWriter().println(fakeUserinfoResponse.toJSONString());
    }
}