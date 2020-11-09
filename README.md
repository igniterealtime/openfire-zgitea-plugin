# Gitea Plugin for openfire

This plugin provides an Openfire hosted GIT service powered by Gitea. 
For more information on why anyone would want to do that, see the following:

- https://bejamas.io/blog/git-based-cms-vs-api-first-cms/
- https://craftercms.org/blog/2020/09/git-based-cms-what-it-is-and-why-you-need-it
- https://forestry.io/blog/why-we-created-a-git-backed-content-manager/
- https://snipcart.com/blog/netlify-cms-react-git-workflow

This plugin adds a real-time communication to content management using a familiar GIT based workflow to create a very responsive collaboration platform that will enable an agile team to create, manage and deliver any type of content with quality assurance. It combines the familiar GitHub front-end user interface with the XMPP client ConverseJS to create a a new user experience where all the tools and people need to get a job done are within easy access.

<img src=https://user-images.githubusercontent.com/110731/98564534-62a69f80-22a4-11eb-8b20-9cb81095609e.png />

**Please note that this plugin works as intended when Openfire is configured to use an external JDBC database. It will automatically replace the default Openfire user/group providers with custom providers that make Gitea the provider for users and groups data. In other words, user and groups are now managed by Gitea and will become read-only in Openfire. Use at own risk.**

## Installation

Copy the gitea.jar file to the OPENFIRE_HOME/plugins directory

## Configuration

Under Server|Git tab you can configure the parameters. 

<img src=https://user-images.githubusercontent.com/110731/98564036-ca101f80-22a3-11eb-9f79-6d6705136cd8.png />

Click on Gitea Admin to configure Gitea. On the first time you do this, Gitea will prompt you to setup the database. The plugin will provide by default, the connection details obtained form Openfire. Modify the values accordingly and proceed with the setup.

## How to use

See https://gitea.io/en-us/

## Known Issues

This version has embedded binaries for only Linux 64 and Windows 64.

