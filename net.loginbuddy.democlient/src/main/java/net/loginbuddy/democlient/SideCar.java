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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SideCar extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Find incoming parameters. In this case the selected provider
        ParameterValidatorResult clientProviderResult = ParameterValidator.getSingleValue(request.getParameterValues("provider_sidecar"));
        String provider = Sanetizer.sanetize(clientProviderResult.getValue(), 64);

        // Create a map as session object and associate it with the state
        Map<String, Object> sessionValues = new HashMap<>();
        sessionValues.put(Constants.CLIENT_PROVIDER.getKey(), provider);

        String clientNonce = UUID.randomUUID().toString();
        sessionValues.put(Constants.CLIENT_NONCE.getKey(), clientNonce);

        String clientState = UUID.randomUUID().toString();
        sessionValues.put(Constants.CLIENT_STATE.getKey(), clientState);

        // Use Loginbuddys Cache
        LoginbuddyCache.CACHE.put(clientState, sessionValues);

        // Use the SDK to request the authorizationUrl for the given provider.
        // - nonce and state are optional (state is recommended). Both are for the client only, Loginbuddy manages its own values
        // - more values can be set before calling 'build()'
        SidecarClient authRequest = SidecarClient.createAuthRequest(provider)
                .setNonce(clientNonce)
                .setState(clientState)
                .build();

        try {
            // redirect the user to the selected provider
            response.sendRedirect(authRequest.getAuthorizationUrl());
        } catch (LoginbuddyToolsException e) {
            response.sendError(e.getHttpStatus(), e.getErrorDescription());
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // The provider redirects back and includes 'code' and 'state' or an error
        // In either case, the query component can be passed to Loginbuddy as is
        String query = request.getQueryString();
        try {

            // Loginbuddy exchanges the code for an access_token, validates the response and returns it here.
            LoginbuddyResponse loginbuddyResponse = SidecarClient.createAuthResponse(query).build().getAuthResponse();

            // Find our previously session object and do some simple validations
            Map<String, Object> sessionValues = (Map) LoginbuddyCache.CACHE.get(loginbuddyResponse.getState());

            // expected state?
            if (!sessionValues.get(Constants.CLIENT_STATE.getKey()).equals(loginbuddyResponse.getState())) {
                throw new RuntimeException("Unexpected state");
            }
            // expected nonce?
            if (!sessionValues.get(Constants.CLIENT_NONCE.getKey()).equals(loginbuddyResponse.getLoginbuddyDetails().getNonce())) {
                throw new RuntimeException("Unexpected nonce");
            }
            // expected provider?
            if (!sessionValues.get(Constants.CLIENT_PROVIDER.getKey()).equals(loginbuddyResponse.getProviderDetails().getProvider())) {
                throw new RuntimeException("Unexpected provider");
            }

            // Handle the response so that this client can display it to the user
            MsgResponse msgResp = new MsgResponse("application/json", loginbuddyResponse.toString(), loginbuddyResponse.getStatus());
            sessionValues.put("msgResponse", msgResp);
            LoginbuddyCache.CACHE.put(loginbuddyResponse.getState(), sessionValues);
            response.sendRedirect(String.format("democlientCallback.jsp?state=%s", loginbuddyResponse.getState()));

        } catch (LoginbuddyToolsException e) {
            response.sendError(e.getHttpStatus(), e.getErrorDescription());
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(400, e.getMessage());
        }
    }
}