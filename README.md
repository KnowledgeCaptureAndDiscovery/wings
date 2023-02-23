[![Test](https://github.com/KnowledgeCaptureAndDiscovery/wings/actions/workflows/maven.yml/badge.svg)](https://github.com/KnowledgeCaptureAndDiscovery/wings/actions/workflows/maven.yml)

# Wings

## Installation

### Docker 

You must install [Docker](https://www.docker.com/) and [docker-compose](https://docs.docker.com/compose/install/).

Clone the repository

```bash
$ git clone https://github.com/KnowledgeCaptureAndDiscovery/wings.git
```

Deploy the container with the following command:

```bash
$ docker-compose up -d
```


Open the browser [http://localhost:8080/wings-portal](http://localhost:8080/wings-portal) to access the Wings portal.


Go to [README Docker](wings-docker/) for additional instructions on running the Docker image.


#### Images

Docker images are available at [DockerHub](https://hub.docker.com/repository/docker/ikcap/wings)

### Maven

Please follow the instructions in [README Maven](docs/maven.md) to install the Wings project.
