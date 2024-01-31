package net.loginbuddy.demoserver.provider;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class LoginbuddyProviderCommon extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginbuddyProviderCommon.class.getName());

    protected String location_demoserver;
    protected String scheme;
    protected String tokenType;
    protected boolean dpopNonceRequired;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        location_demoserver = System.getenv("DEMOSERVER_LOCATION");
        if(location_demoserver.startsWith("http://")) {
            scheme = "http";
            LOGGER.warning("Loginbuddy Demoserver is using http!");
        } else {
            scheme = "https";
        }
        tokenType = System.getenv("DEMOSERVER_TOKEN_TYPE");
        tokenType = tokenType == null ? "Bearer" : "dpop".equalsIgnoreCase(tokenType) ? "DPop" : "Bearer";
        dpopNonceRequired = tokenType.equalsIgnoreCase("dpop");
    }

    /**
     * Get the 'sub' value. Either plain or a PPID
     *
     * @param clientId
     * @param email
     * @param ppid
     * @return
     */
    protected String getSub(String clientId, String email, boolean ppid) {
        if (ppid) {
            // Create a fake PPID to be used with 'sub'
            String ppidSub = "fakeProviderSalt".concat(clientId).concat(email);
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                // should never happen
                LOGGER.severe(e.getMessage());
            }
            return new String(Base64.getUrlEncoder().encode(md.digest(ppidSub.getBytes()))).replace("=", "").replace("-", "");
        } else {
            return email;
        }
    }

    protected boolean checkForDpopNonce(HttpServletRequest request, HttpServletResponse response) {
        // dpop-header is a JWT, and has 'nonce' as part of the payload
        boolean result = false;
        try {
            String dpopNonce = (String) ((JSONObject) new JSONParser().parse(new String(org.jose4j.base64url.Base64.decode(request.getHeader("dpop").split("[.]")[1])))).get("nonce");
            if (dpopNonce == null) {
                LOGGER.info("Missing dpop nonce");
                response.setStatus(400);
                response.setContentType("application/json; charset=UTF-8");
                response.addHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
                response.getWriter().println("{\"error\":\"use_dpop_nonce\", \"error_description\": \"Authorization server requires nonce in DPoP proof\"}");
                result = true;
            }
        } catch(Exception e) {
            LOGGER.warning(String.format("DPoP error occurred: %s", e.getMessage()));
        }
        return result;
    }
    protected boolean checkForDpopNonceProtected(HttpServletRequest request, HttpServletResponse response) {
        // dpop-header is a JWT, and has 'nonce' as part of the payload
        boolean result = false;
        try {
            String dpopNonce = (String) ((JSONObject) new JSONParser().parse(new String(org.jose4j.base64url.Base64.decode(request.getHeader("dpop").split("[.]")[1])))).get("nonce");
            if (dpopNonce == null) {
                LOGGER.info("Missing dpop nonce for protected endpoint");
                response.setStatus(401);
                response.setContentType("application/json; charset=UTF-8");
                response.addHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
                response.addHeader("WWW-Authenticate", "DPoP error=\"use_dpop_nonce\", error_description=\"Resource server requires nonce in DPoP proof\"");
                response.getWriter().println("{\"error\":\"use_dpop_nonce\", \"error_description\": \"Authorization server requires nonce in DPoP proof\"}");
                result = true;
            }
        } catch(Exception e) {
            LOGGER.warning(String.format("DPoP error occurred: %s", e.getMessage()));
        }
        return result;
    }

    protected boolean checkForDpop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(request.getHeader("dpop") == null) {
            LOGGER.info("Missing dpop header");
            response.setStatus(400);
            response.setContentType("application/json; charset=UTF-8");
            response.addHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
            response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP header\"}");
            return true;
        }
        return false;
    }
    protected boolean checkForDpopProtected(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(request.getHeader("dpop") == null) {
            LOGGER.info("Missing dpop header for protected endpoint");
            response.setStatus(401);
            response.setContentType("application/json; charset=UTF-8");
            response.addHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
            response.addHeader("WWW-Authenticate", "DPoP error=\"invalid_request\", error_description=\"Missing DPoP proof\", algs=\"ES256\"");
            response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP header\"}");
            return true;
        }
        return false;
    }
    protected boolean checkForDpopJkt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(request.getParameter("dpop_jkt") == null) {
            LOGGER.info("Missing dpop_jkt parameter");
            response.setStatus(400);
            response.setContentType("application/json; charset=UTF-8");
            response.addHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
            response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP dpop_jkt  parameter\"}");
            return true;
        }
        return false;
    }
}
