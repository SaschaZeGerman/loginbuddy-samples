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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginbuddyProviderPauthorize extends LoginbuddyProviderCommon {

  private static final Logger LOGGER = Logger.getLogger(LoginbuddyProviderPauthorize.class.getName());

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {

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
          state);
      LoginbuddyCache.CACHE.put(sessionContext.getId(), sessionContext, 60L);
      String requestUri = String.format("urn:loginbuddy-samples:%s",  sessionContext.getId());
      LoginbuddyCache.CACHE.put(requestUri, loginHint, 60L);

      response.setStatus(201);
      response.setContentType("application/json");
      response.getWriter().printf("{\"request_uri\":\"%s\", \"expires_in\":300}", requestUri);
    } catch (Exception e) {
      LOGGER.warning("The pushed authorization request was invalid");
      response.setStatus(404);
      response.setContentType("application/json; charset=UTF-8");
      response.getWriter().println(
          "\"error\":\"invalid_Request\", \"error_description\": \"the pushed authorization request was invalid!\"");
    }
  }
}