### Maven

1. Get war file for the [releases page](https://github.com/KnowledgeCaptureAndDiscovery/wings/releases)
2. Move the war file to a Servlet container (Tomcat) and rename to wings-portal.war
 - $ mv /path/to/wings-portal-<version>.war /path/to/tomcat/webapps/wings-portal.war

3. Setup an admin user in /path/to/tomcat/conf/tomcat-users.xml
 - Wings admin users need to have the "WingsUser" and "WingsAdmin" roles
Example: (The following needs to go inside the <tomcat-users ..> Section.

` <user name="adminUser" password="adminPassword" roles="WingsUser,WingsAdmin" />`

4. In /path/to/tomcat/conf/server.xml, add 'readonly="false"' to the UserDatabase Resource 

 `<Resource name="UserDatabase" ...  readonly="false"/>`

5. Add the following to the "Context" section
 
 `<ResourceLink name="users" global="UserDatabase" type="org.apache.catalina.UserDatabase" />`

6. Start tomcat

 `$ /path/to/tomcat/bin/startup.sh`

7. Login to the server from http://<your-server-name>:8080/wings-portal/login
 - Note: 8080 is the default Tomcat port, but this can be changed in server.xml

8. After first Login, go to $HOME/.wings directory and open portal.properties
 - Change /path/to/dot (graphviz) if it is set incorrectly
 - Check that the server name is set correctly
 - (Optional) Change any other settings if needed

9. Manage users at http://<your-server-name>:8080/wings-portal/users/common/list
 - From this UI, you can add or remove more users.

10. To see a list of domains to download, please go to the "Manage Domains" interface
 - To Import a domain: From the "Manage Domains" interface, do the following:
 - Add -> Import Domain
 - Enter Domain Location: the url (or path) to the domain zip file
 - Note that some domains might require installation of infrastructure software on your server
