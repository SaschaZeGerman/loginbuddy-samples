package net.loginbuddy.democlient;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

import java.util.logging.Logger;

public abstract class LoginbuddyDemoclientCommon extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyDemoclientCommon.class));

    protected String location_democlient;
    protected String location_loginbuddy;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        location_democlient = System.getenv("DEMOCLIENT_LOCATION");
        if(location_democlient.startsWith("http://")) {
            LOGGER.info("Loginbuddy Democlient is using http");
        }
        location_loginbuddy = System.getenv("DEMOCLIENT_LOCATION_LOGINBUDDY");
    }

}
