package net.loginbuddy.democlient;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Sanetizer;
import net.loginbuddy.tools.client.SidecarClient;
import net.loginbuddy.tools.common.exception.LoginbuddyToolsException;
import net.loginbuddy.tools.common.model.LoginbuddyResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SideCar extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ParameterValidatorResult clientProviderResult = ParameterValidator
                .getSingleValue(request.getParameterValues("provider_sidecar"));
        String provider = Sanetizer.sanetize(clientProviderResult.getValue(), 64);

        Map<String, Object> sessionValues = new HashMap<>();
        sessionValues.put(Constants.CLIENT_PROVIDER.getKey(), provider);

        String clientNonce = UUID.randomUUID().toString();
        sessionValues.put(Constants.NONCE.getKey(), clientNonce);

        String clientState = UUID.randomUUID().toString();
        sessionValues.put(Constants.CLIENT_STATE.getKey(), clientState);

        LoginbuddyCache.CACHE.put("statevalue", sessionValues);

        SidecarClient authRequest = SidecarClient.createAuthRequest(provider)
                .setState("statevalue")
                .build();

        try {
            response.sendRedirect(authRequest.getAuthorizationUrl());
        } catch (LoginbuddyToolsException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*
            As for POST, first validation should be implemented here too.
            The url query component will either contain 'code' and 'state' or 'error' and 'error_description'.
            Our code does not need to worry about that, Loginbuddy will handle both cases.
         */
        String query = request.getQueryString();
        try {
            LoginbuddyResponse loginbuddyResponse = SidecarClient.getAuthResponse(query);
            MsgResponse msgResp = new MsgResponse("application/json", loginbuddyResponse.toString(), 200);
            Map<String, Object> sessionValues = new HashMap<>();
            sessionValues.put("msgResponse", msgResp);
            LoginbuddyCache.CACHE.put("statevalue", sessionValues);
            response.sendRedirect(String.format("democlientCallback.jsp?state=%s", "statevalue"));
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(400, String.format("Something went wrong: %s", e.getMessage()));
        }
    }
}