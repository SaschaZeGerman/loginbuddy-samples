# Loginbuddy - Samples

This repository holds samples that use Loginbuddy. Once launched, users can open a browser and will find demo clients, a demo OpenID Connect (OIDC) server and Loginbuddy itself.

Loginbuddy is located in between clients and OIDC providers. Loginbuddy handles all *complicated* parts of OAuth and OIDC flows and follows best practices.

These samples are also usable to connect to new providers, test connections, view responses of those providers, and integrate them into a client.

## Run the samples

To run and build the samples, these tools and technologies are needed:

- java jdk11
- maven
- docker
- docker-compose
- make // this is for your convenience. If not available, its commands can be run manually

### Prepare your environment

- Add this line to your hosts file: **127.0.0.1 local.loginbuddy.net demoserver.loginbuddy.net democlient.loginbuddy.net**
  - for MacBooks this would be done at: `/etc/hosts`
  - for Windows this would be done at: `C:\Windows\System32\drivers\etc\hosts`

Clone the project:

- `git clone https://github.com/SaschaZeGerman/loginbuddy-samples.git`
- `cd ./loginbuddy-samples`

Run these files to setup the dev environment and dev key pairs:

- `sh initialize-dev-environment.sh`  // once
- `sh initialize-dev-tls-keypair.sh`  // whenever you want to update the dev key pair

### Build the samples

This is one command only:

- `make build_all`  // this will use maven to build and compile the project, it will also build docker images

### Launch the samples

Loginbuddy is completely docker based:

- `docker-compose up -d`  // to view the logging output, run it without the switch -d)
- `docker-compose down` // once you want to stop the setup

**Tip**: the samples will pull Loginbuddy (loginbuddy, loginbuddy-oidcdr) from Dockerhub. However, depending on your machine (MacBook Intel/ Apple silicone/ Windows) you may run into problems and have to build Loginbuddy yourself. For that, please follow the instructions at [Loginbuddy on GitHub](https://github.com/SaschaZeGerman/loginbuddy/wiki/Development). It mainly requires `make build_all`, that's it.

After launching the setup, you should find these images (docker ps -aq)

- **saschazegerman/loginbuddy-democlient:latest** (part of this project)
  - simple applications, a Java based web application and a JavaScript based Single Page App (SPA)
- **saschazegerman/loginbuddy-demoserver:latest** (part of this project)
  - a simple OpenID Connect server to simulate a real IDP
  - this server does some request validations, but it does not verify passwords or usernames (... for demo only)
- **saschazegerman/loginbuddy:latest** (pulled from Dockerhub)
  - this is Loginbuddy itself, deployed as standalone service in front of an IDP
  - this is the container an application developers would use to support OIDC flows in his application
- **saschazegerman/loginbuddy-oidcdr:latest** (pulled from Dockerhub)
  - this is Loginbuddys container that handles dynamic registrations
  - it works in conjunction with Loginbuddy standalone and sidecar images

The containers occupy these ports:

- democlient: 80, 443
- demoserver: 8443
- Loginbuddy: 8444

These ports are needed to simulate the three party setup on a single machine.

## Using the samples

Now that your setup is up and running, open a browser:

- `https://democlient.loginbuddy.net`

``` 
You have to confirm SSL/TLS certificate warnings due to self-signed certificates. Because the setup uses different hostnames, you have to confirm multiple times.
Confirming these warnings is only required once since the setup is reusing the generated dev key pair with each launch.
```

Out of the box there are two clients available. A web application and a SPA, both demo slightly different features.

Once selected, the user will find a page that displays two buttons named **FAKE**. Those simulate buttons such as *Sign in with Google*.

Both buttons take users to Loginbuddys demo OIDC server. In a real life scenario, that could be Google or any other OIDC supported authorization server.

After selecting the provider and signing in, a response is returned and displayed.

The response includes all values as returned by the provider, plus three fields:

- details_provider
- details_loginbuddy
- details_normalized

The response is exactly the response as returned from Loginbuddy to the client. That means, if you are developing a client that uses Loginbuddy,
you can use this view to know what you have to handle.

## Add a new OpenID Provider

To add another button to the selection of providers, only a few steps are needed:

1. Register and OAuth/ OIDC client at your target OIDC provider
2. Configure Loginbuddy to include that provider when displaying the list of supported providers
3. Add an image for the button into Loginbuddy

### Configure your OpenID Provider

- Create a developer account at your target OpenID Provider (try Google for a start)
- Register an OAuth application using these details:
  - **client type:** *confidential* or *web application*
  - **redirect_uri:** *https://local.loginbuddy.net:8444/callback*
- Note these values that google generates:
  - **client_id**
  - **client_secret**

### Within this sample project

You need to update a few files:

- **./docker-build/add-ons/loginbuddy/config.json**
  - add a provider configuration for Google
  - copy the example from *./docker-build/add-ons/templates/config_common_providers.json* and fill in *client_id/ client_secret* 
- **./docker-build/add-ons/loginbuddy/permissions.policy**
  - Uncomment these lines:
    -     permission java.net.SocketPermission "accounts.google.com", "connect,resolve";
          permission java.net.SocketPermission "oauth2.googleapis.com", "connect,resolve";
          permission java.net.SocketPermission "openidconnect.googleapis.com", "connect,resolve";
          permission java.net.SocketPermission "www.googleapis.com", "connect,resolve";
- launch the setup as before
- Open a browser at **https://democlient.loginbuddy.net** and select the image of *Sign in with Google*
- follow the prompts

For more info, look into the [Configuration](Configuration) document.

If you are not sure what Loginbuddy can do for you, please refer to **Deployment**, **Development** and **Configuration** in this WIKI.