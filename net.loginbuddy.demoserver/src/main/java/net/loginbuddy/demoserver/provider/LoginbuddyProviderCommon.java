package net.loginbuddy.demoserver.provider;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

public abstract class LoginbuddyProviderCommon extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginbuddyProviderCommon.class.getName());

    protected String location_demoserver;
    protected String scheme;

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
}
