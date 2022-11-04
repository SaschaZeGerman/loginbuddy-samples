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
  - this server does some request validations, but it does not verify passwords or usernames (... for demo only)

## Run the samples

The instructions are made for Docker on a MacBook and may need to be adjusted for Windows users.

- Add this line to your hosts file: **127.0.0.1 local.loginbuddy.net demoserver.loginbuddy.net democlient.loginbuddy.net**
    - for MacBooks this would be done at: `/etc/hosts`
    - for Windows this would be done at: `C:\Windows\System32\drivers\etc\hosts`

### Custom Configuration Loader

This testsuite is using a custom loader for loading clients and providers. The custom loader has been implemented in **src** directory.

The class implementing the loader is configured here:
- file: `./docker-test/loginbuddy.properties`
- property: `config.loginbuddy.loader.default`

### Prepare

Clone this project:
- `git clone https://github.com/SaschaZeGerman/loginbuddy.git`
- `cd` into `./loginbuddy`

Add this line to your hosts file: `127.0.0.1 local.loginbuddy.net demoserver.loginbuddy.net democlient.loginbuddy.net`
- for MacBooks this would be done at: `/etc/hosts`
- for Windows this would be done at: `C:\Windows\System32\drivers\etc\hosts`

### Run docker

If you want to re-launch Loginbuddy often, create a re-usable keypair for development purposes:
- `make initialize_dev`

**Using https:** Open a terminal and run this command from the root of the repository (./loginbuddy):

- `docker-compose -f docker-compose-demosetup.yml up`
- open a browser at `https://democlient.loginbuddy.net`
  - you will have to confirm SSL warnings. Loginbuddy produces self-signed certificates on the fly
  - if you ran the `make` command from above these warnings will only appear the first time

**Using http:** If your browser does not support self-signed websites, use this:

- `docker-compose -f docker-compose-demosetup-http.yml up`
- open a browser at `http://democlient.loginbuddy.net`
- this allows you to run the demosetup using http only
  - the feature of **OpenID Connect Dynamic Registration** is not available since Loginbuddy would try to register with an **http://** redirect_uri which is not supported!

**Note:** When using **http**, the **SPA** democlient simulates the usage of PKCE and uses a fixed code_verifier since browsers do not support the creation of a sha256 value with **http**!

## The demo flow

On the first screen you choose to use a webapplication or a single page app (SPA). Those are the two provided demo clients.

When following the screens, at some point you can choose the **FAKE** provider. If you have an OpenID provider that supports OpenID Connect Dynamic Registration available, you can choose that instead! (**Note:** The dynamic registration option is not available for the http demosetup).

When everything went well, you will get to a page that contains a window that displays the data that Loginbuddy has received from the OpenID Provider. It also contains data that Loginbuddy has produced.

That is the response your application would get!

If you are not sure what Loginbuddy can do for you, please refer to **Deployment**, **Development** and **Configuration** in this WIKI.

## Add a new OpenID Provider

To do this you first need to clone this project! You also need to sign up at an OpenID Provider such as google.

This is the short version of the **Configuration** document. Here you go:

### Within your OpenID Provider

- Create a developer account at your target OpenID Provider (try google for a start)
- Register an OAuth application using these details:
  - **client type:** *confidential* or *web application*
  - **redirect_uri:** *https://local.loginbuddy.net/callback*
- Note these values that google generates:
  - **client_id**
  - **client_secret**

### Within the Loginbuddy project

You need to update a few files:

- **{loginbuddy-project}/docker-build/add-ons/demosetup/config.json**
  - go to **loginbuddy.clients\[0].providers** which is a JSONArray. Next to **"server_loginbuddy"** add **, "google"**
  - go to **loginbuddy.providers\[2].provider.google**. Configure the **client_id** and **client_secret** that you noted earlier
- **{loginbuddy-project}/docker-build/add-ons/demosetup/permissions.policy**
  - Uncomment these lines:
    -     permission java.net.SocketPermission "accounts.google.com", "connect,resolve";
          permission java.net.SocketPermission "oauth2.googleapis.com", "connect,resolve";
          permission java.net.SocketPermission "openidconnect.googleapis.com", "connect,resolve";
          permission java.net.SocketPermission "www.googleapis.com", "connect,resolve";
- your **hosts** file. On a MacBook this would be at **/etc/hosts**
  - add the below line (if you have not already done it):
  - **127.0.0.1 local.loginbuddy.net democlient.loginbuddy.net demoserver.loginbuddy.net**
- run the following commands at the root of Loginbuddy!
  - `make build_all`  // this builds the Loginbuddy containers
  - `docker-compose -f docker-compose-demosetup.yml up`  // this will launch the demo environment
- Open a browser at **https://democlient.loginbuddy.net**
- Click **Demo Client** // two are available, choose one
- Click **Submit**
- Select **google**
- follow the prompts

In the final screen you will see what data was received and created by Loginbuddy. That document would be available to your application!

When using Loginbuddy, your client application will always get the same response structure, no matter which provider was chosen by your users. This simplifies the development of your client.

For more info, look into the [Configuration](Configuration) document.