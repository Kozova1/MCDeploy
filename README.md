# MCDeploy
MCDeploy is a new and easy way to deploy minecraft servers.

It enables creating servers declaratively, using a config file written in TOML.  
MCDeploy provides the following subcommands:
- `new DIRECTORY` - Creates a new server using the default template in DIRECTORY.
- `help` - Displays a short help screen.
- `version` - Displays the current version.
- `deploy` - Deploys the server according to the configuration file in the current directory. If the server already exists, it will error out.
- `update` - Does the same as `deploy`, except it allows overwriting existing servers, thus enabling updating a server. BE CAREFUL WITH THIS!
- `describe CONFIG_PATH...` - Describes a list of config values. For more info, see [`src/main/resources/Help.txt`](src/main/resources/Help.txt).
- `write-systemd-service` - Writes a systemd user service template in the current directory, which will run the server when started.

MCDeploy uses the file `mcdeploy.toml` in the current directory to decide how to deploy a server.
The most minimal `mcdeploy.toml` which creates a playable server is:
```toml
[Server]
AgreeToEULA = true
```
This will create a server using the latest release,
with the default `server.properties`,
which can be a playable survival server.

If you want to do something more involved, you'll have to provide more configuration.
To get help, run `MCDeploy.jar help` or read the files in [`src/main/resources`](src/main/resources) for explanations.
