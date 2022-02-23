FROM tomcat:8
RUN sed -i 's/debian testing main/debian testing main contrib non-free/' /etc/apt/sources.list

# Install general tools
RUN apt-get update && apt-get -y install --no-install-recommends \
        graphviz \
        libcurl4-openssl-dev \
        libxml2-dev \
        python3-pip \
        git \
        cgroupfs-mount \
        maven \
        tcl \
        tk \
        apt-transport-https \
        software-properties-common \
        gnupg2

# Install Docker
RUN curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg | apt-key add -
RUN add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
   $(lsb_release -cs) \
   stable"
RUN apt-get update && apt-get -y install docker-ce

# Configure tomcat
# Add the tomcat users file
COPY ./wings-docker/config/tomcat/tomcat-users.xml /usr/local/tomcat/conf/

# Add the tomcat server configuration file
COPY ./wings-docker/config/tomcat/server.xml /usr/local/tomcat/conf/

# Add wings environment variable
ENV WINGS_MODE='dind'

# Install wings
RUN apt-get update && apt-get install -y git default-jdk
RUN mkdir -p /wings-src/wings/
WORKDIR /wings-src/wings


# Add wings properties
RUN mkdir -p /opt/wings/storage/default \
    && mkdir -p /etc/wings/ \
    && mkdir -p /usr/local/tomcat/webapps/wings-portal/META-INF/


# Add Wings build to tomcat (Deploy)
WORKDIR /usr/local/tomcat/webapps/wings-portal
COPY build/wings-portal-*.war .
RUN jar -xvf wings-portal-*.war

# Add wings context file to the wings portal webapp
COPY ./wings-docker/config/default/wings-portal.xml /usr/local/tomcat/webapps/wings-portal/META-INF/context.xml
COPY ./wings-docker/config/default/portal.properties /etc/wings/portal.properties

WORKDIR /usr/local/tomcat
