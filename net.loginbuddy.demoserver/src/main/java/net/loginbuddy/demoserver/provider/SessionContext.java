package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.cache.LoginbuddyContext;
import net.loginbuddy.common.config.Constants;

import java.io.Serializable;
import java.util.Date;

public class SessionContext extends LoginbuddyContext implements Serializable {

  public SessionContext() {
    super();
  }

  public void sessionInit(String clientId, String scope, String response_type, String code_challenge,
      String code_challenge_method, String redirectUri, String nonce, String state, String authorizationDetails) {
    put(Constants.CLIENT_ID.getKey(), clientId);
    put(Constants.SCOPE.getKey(), scope);
    put(Constants.RESPONSE_TYPE.getKey(), response_type);
    put(Constants.CODE_CHALLENGE.getKey(), code_challenge);
    put(Constants.CODE_CHALLENGE_METHOD.getKey(), code_challenge_method);
    put(Constants.REDIRECT_URI.getKey(), redirectUri);
    put(Constants.NONCE.getKey(), nonce);
    put(Constants.STATE.getKey(), state);
    put(Constants.SESSION.getKey(), getId());
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_LOGIN.getKey());
    put(Constants.AUTHORIZATION_DETAILS.getKey(), authorizationDetails);
  }

  public void sessionLoginProvided(String usernameLabel, String username) {
    put(usernameLabel, username);
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_AUTHENTICATE.getKey());
  }

  public void sessionAuthenticated() {
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_GRANT.getKey());
  }

  public void sessionGranted() {
    put("grant", String.valueOf(new Date()
        .getTime())); // TODO: remember when this grant was given! If we had a 'grant' table, it would go in there
  }

  public long sessionToken(String access_token, String refresh_token, String id_token) {
    long now = LoginbuddyProviderCommon.nowInSeconds();
    put("access_token", access_token);
    put("refresh_token", refresh_token);
    put("id_token", id_token);
    put("access_token_expiration", now + 600L);
    put("refresh_token_expiration", now + 3600L);
    put("refresh_token_lifetime", 3600L);
    return 600; // accessToken lifetime
  }
}
