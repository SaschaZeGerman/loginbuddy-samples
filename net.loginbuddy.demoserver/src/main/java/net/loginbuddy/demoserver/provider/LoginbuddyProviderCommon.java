package net.loginbuddy.demoserver.provider;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
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
        if (location_demoserver.startsWith("http://")) {
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
        try {
            String dpopNonce = (String) ((JSONObject) new JSONParser().parse(new String(Base64.getDecoder().decode(request.getHeader("dpop").split("[.]")[1])))).get("nonce");
            if (dpopNonce == null) {
                LOGGER.warning("Missing dpop nonce");
                response.setStatus(400);
                response.setContentType("application/json; charset=UTF-8");
                String dpopJkt = UUID.randomUUID().toString();
                LOGGER.info(String.format("Generated dpop_jkt: %s", dpopJkt));
                response.setHeader(Constants.DPOP_NONCE_HEADER.getKey(), dpopJkt);
                response.getWriter().println("{\"error\":\"use_dpop_nonce\", \"error_description\": \"Authorization server requires nonce in DPoP proof\"}");
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("DPoP error occurred: %s", e.getMessage()));
            return true;
        }
        return false;
    }

    protected boolean checkForDpopNonceProtected(HttpServletRequest request, HttpServletResponse response) {
        // dpop-header is a JWT, and has 'nonce' as part of the payload
        try {
            String dpopNonce = (String) ((JSONObject) new JSONParser().parse(new String(Base64.getDecoder().decode(request.getHeader("dpop").split("[.]")[1])))).get("nonce");
            if (dpopNonce == null) {
                LOGGER.warning("Missing dpop nonce for protected endpoint");
                response.setStatus(401);
                response.setContentType("application/json; charset=UTF-8");
                response.setHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
                response.setHeader("WWW-Authenticate", "DPoP error=\"use_dpop_nonce\", error_description=\"Resource server requires nonce in DPoP proof\"");
                response.getWriter().println("{\"error\":\"use_dpop_nonce\", \"error_description\": \"Authorization server requires nonce in DPoP proof\"}");
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("DPoP error occurred: %s", e.getMessage()));
            return true;
        }
        return false;
    }

    protected boolean checkForDpop(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getHeader("dpop") == null) {
            LOGGER.warning("Missing dpop header");
            response.setStatus(400);
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
            response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP header\"}");
            return true;
        }
        return false;
    }

    protected boolean checkForDpopProtected(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getHeader("dpop") == null) {
            LOGGER.warning("Missing dpop header for protected endpoint");
            response.setStatus(401);
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
            response.setHeader("WWW-Authenticate", "DPoP error=\"invalid_request\", error_description=\"Missing DPoP proof\", algs=\"ES256\"");
            response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP header\"}");
            return true;
        }
        return false;
    }

    protected boolean checkForDpopJkt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getParameter("dpop_jkt") == null) {
            LOGGER.warning("Missing dpop_jkt parameter");
            response.setStatus(400);
            response.setContentType("application/json; charset=UTF-8");
            response.setHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
            response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP dpop_jkt  parameter\"}");
            return true;
        }
        return false;
    }

    protected boolean checkJktVsJwkJkt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String dpopJkt = request.getParameter("dpop_jkt");
        String dpopJwkJkt = getJwkJkt(request);
        if (dpopJwkJkt.equals(dpopJkt)) {
            return false;
        }
        LOGGER.warning(String.format("jkt do not match. dpop_jkt=%s, dpop_jwk_jkt=%s", dpopJkt, dpopJwkJkt));
        response.setStatus(400);
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader(Constants.DPOP_NONCE_HEADER.getKey(), UUID.randomUUID().toString());
        response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\": \"Authorization server requires DPoP dpop_jkt  parameter\"}");
        return true;
    }

    protected String getJwkJkt(HttpServletRequest request) throws Exception {
        JSONObject jwtHeader = (JSONObject) new JSONParser().parse(new String(org.jose4j.base64url.Base64.decode(request.getHeader("dpop").split("[.]")[0])));
        return JsonWebKey.Factory.newJwk(((JSONObject) jwtHeader.get("jwk")).toJSONString()).calculateBase64urlEncodedThumbprint("SHA-256");
    }

    /**
     * Compare the dpop_jkt that was given during the authorization request and the calculated one based on the JWK found in the dpop-header
     *
     * @param request contains the dpop-header
     * @param dpopJkt
     * @return
     */
    protected boolean compareJkt(HttpServletRequest request, String dpopJkt) {
        try {
            String dpopJwkJkt = getJwkJkt(request);
            if (dpopJwkJkt.equals(dpopJkt)) {
                return true;
            } else {
                LOGGER.warning(String.format("jkt do not match. dpop_jkt=%s, dpop_jwk_jkt=%s", dpopJkt, dpopJwkJkt));
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Error comparing jkt: %s", e.getMessage()));
        }
        return false;
    }

    /**
     * Comparing the cnf/jkt out of the access_token to dpop_jkt given at the authorization_code flow
     *
     * @param request contains the access_token
     * @param dpopJkt
     * @return
     */
    protected boolean compareJktCnf(HttpServletRequest request, String dpopJkt) {
        try {
            JSONObject accessTokenPayload = (JSONObject) new JSONParser().parse(new String(org.jose4j.base64url.Base64.decode(request.getHeader("authorization").split("[ ]")[1].split("[.]")[1])));
            String dpopCnfJkt = (String) ((JSONObject) accessTokenPayload.get("cnf")).get("jkt");
            if (dpopCnfJkt.equals(dpopJkt)) {
                return true;
            } else {
                LOGGER.warning(String.format("jkt do not match. dpop_jkt=%s, cnf_jkt=%s", dpopJkt, dpopCnfJkt));
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Error comparing jkt: %s", e.getMessage()));
        }
        return false;
    }

    protected static long nowInSeconds() {
        return new Date().getTime() / 1000;
    }
}
