package net.loginbuddy.democlient;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Sanetizer;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class Initialize extends LoginbuddyDemoclientCommon {

    private static final Logger LOGGER = Logger.getLogger(Initialize.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // just checking if this unused hidden field had a value which would be suspicious

        ParameterValidatorResult providerAddition = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROVIDER_ADDITION.getKey()));
        if (providerAddition.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            LOGGER.warning(String.format("Invalid request! Unused field had values: '%s'", providerAddition));
            response.sendError(400, "Invalid request, please try again!");
            return;
        }

        // Simple validation. This is just for demo!

        ParameterValidatorResult clientIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
        String clientId = Sanetizer.sanetize(clientIdResult.getValue(), 64);

        ParameterValidatorResult clientProviderResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()));
        String clientProvider = Sanetizer.sanetize(clientProviderResult.getValue(), 64);

        ParameterValidatorResult authorizationDetailsResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.AUTHORIZATION_DETAILS.getKey()), "");
        String authorizationDetails = authorizationDetailsResult.getValue();
        if(!"".equals(authorizationDetails)) {
            try {
                new JSONParser().parse(authorizationDetails);
            } catch (ParseException e) {
                LOGGER.warning(String.format("The given authorization_details are not JSON: %s", e.getMessage()));
                authorizationDetails = "";
            }
        }
        String clientAuthorizationDetails = "".equals(authorizationDetails) ? "" : String.format("&authorization_details=%s", HttpHelper.urlEncode(authorizationDetails));

        ParameterValidatorResult clientObfuscateTokenResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.OBFUSCATE_TOKEN.getKey()));
        boolean clientObfuscateToken = Boolean.parseBoolean(Sanetizer.sanetize(clientObfuscateTokenResult.getValue(), 5));

        // Create a session

        Map<String, Object> sessionValues = new HashMap<>();
        sessionValues.put(Constants.CLIENT_ID.getKey(), clientIdResult.getValue());

        String clientResponseType = "code";
        sessionValues.put(Constants.CLIENT_RESPONSE_TYPE.getKey(), clientResponseType);

        String clientRedirectUri = String.format("%s/callback", location_democlient);
        sessionValues.put(Constants.CLIENT_REDIRECT.getKey(), clientRedirectUri);

        String clientNonce = UUID.randomUUID().toString();
        sessionValues.put(Constants.NONCE.getKey(), clientNonce);

        String clientState = UUID.randomUUID().toString();
        sessionValues.put(Constants.CLIENT_STATE.getKey(), clientState);

        String clientScope = "openid email profile";
        sessionValues.put(Constants.CLIENT_SCOPE.getKey(), clientScope);

        sessionValues.put(Constants.CLIENT_PROVIDER.getKey(), clientProvider);

        LoginbuddyCache.CACHE.put(clientState, sessionValues);

        // Create authorization URL to send the user to Loginbuddy
        response.sendRedirect(String.format("%s/authorize?client_id=%s&response_type=%s&redirect_uri=%s&nonce=%s&state=%s&scope=%s&provider=%s&obfuscate_token=%b%s",
                location_loginbuddy,
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientResponseType, StandardCharsets.UTF_8),
                URLEncoder.encode(clientRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(clientNonce, StandardCharsets.UTF_8),
                URLEncoder.encode(clientState, StandardCharsets.UTF_8),
                URLEncoder.encode(clientScope, StandardCharsets.UTF_8),
                URLEncoder.encode(clientProvider, StandardCharsets.UTF_8),
                clientObfuscateToken,
                clientAuthorizationDetails));
    }
}