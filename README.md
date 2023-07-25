[![Test](https://github.com/KnowledgeCaptureAndDiscovery/wings/actions/workflows/maven.yml/badge.svg)](https://github.com/KnowledgeCaptureAndDiscovery/wings/actions/workflows/maven.yml)

# Wings

## Installation

### Docker

You must install [Docker](https://www.docker.com/) and [docker-compose](https://docs.docker.com/compose/install/).

Clone the repository

```console
$ git clone https://github.com/KnowledgeCaptureAndDiscovery/wings.git
```

Deploy the container with the following command:

```console
$ docker-compose up -d
```

Open the browser [http://localhost:8080/wings-portal](http://localhost:8080/wings-portal) to access the Wings portal.

To stop the container, run the following command:

```bash
$ docker-compose down
```

#### Users

The default user is `admin` and the password is `4dm1n!23`. You can change the password in the `./wings-docker/config/tomcat/tomcat-users.xml` and rebuild the container.

```
$ docker-compose build
$ docker-compose up -d
```

#### Configuration

Please follow the instructions in [README Configuration](docs/configuration.md) to configure the Wings project.

#### Images

Docker images are available at [DockerHub](https://hub.docker.com/repository/docker/ikcap/wings)

### Maven

Please follow the instructions in [README Maven](docs/maven.md) to install the Wings project.
