# Loginbuddy - Samples

**WIP!**

This repository holds samples that use Loginbuddy to support OpenID Connect based logins for applications. Loginbuddy supports different deployment models and these 
samples show off how they may be used.

These samples leverage a few Docker based images:

- **saschazegerman/loginbuddy:latest**
  - this is Loginbuddy itself, deployed as standalone service in front of an IDP
  - this is the container an application developers would use to support OIDC flows in his application
- **saschazegerman/loginbuddy-oidcdr:latest**
  - this is Loginbuddys container that handles dynamic registrations
  - it works in conjunction with Loginbuddy standalone and sidecar images
- **saschazegerman/loginbuddy-democlient:latest**
  - simple applications, a Java based web application and a JavaScript based Single Page App (SPA)
- **saschazegerman/loginbuddy-demoserver:latest**
  - a simple OpenID Connect server to simulate a real IDP
  - this server does some request validations, but it does not verify passwords or usernames

## Run the samples

The instructions are made for Docker on a MacBook and may need to be adjusted for Windows users.

- Add this line to your hosts file: **127.0.0.1 local.loginbuddy.net demoserver.loginbuddy.net democlient.loginbuddy.net**
    - for MacBooks this would be done at: `/etc/hosts`
    - for Windows this would be done at: `C:\Windows\System32\drivers\etc\hosts`

...