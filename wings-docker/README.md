# WINGS-Docker repository
## Introduction
This repository contains different WINGS docker images with pre-installed software, as well as the instructions to run them.

* kcapd/wings-base: Clean installation of WINGS and Docker. 
* kcapd/wings-genomics: contains all the software from wings-base plus python-dev, samtools, tophat, cufflinks, RSeQC and R.

## Using Pre-built Images
In order to use this images, you can simply pull them from dockerhub: 

* ```docker pull kcapd/wings-base``` or 
* ```docker pull kcapd/wings-genomics```

## Building the Image
Alternatively, you may build any of the docker files in the "docker" folder of this repository. **Please build them from the folder which contains this readme**. 

Note that this takes a little bit more time than simply pulling the images from dockerhub. However, this approach is better if you want to install additional software on your WINGS image.

There are two ways to build the image:

### Using docker-compose (recommended)
If docker-compose is installed, you may use it to build the image. The instructions are defined in the `docker-compose.yml` file. 

```bash
docker-compose build
```

The parameters to build the image may be changed directly in the compose file (such as the image tag, volume name, ports, etc.).

### Using docker

```bash
docker build -t [IMAGE_NAME] -f docker/default/Dockerfile . 
```

## Executing WINGS:
Once you have pulled or created your images, you can run wings in two ways:

### Using docker-compose (recommended)
To run the server use

```bash
docker-compose up
```

You may also specify the `-d` flag to run in detached mode.

If the server is running, you can enter the container by running

```bash
docker-compose exec [IMAGE_NAME] bash
```

If the server is not running, you can run it inside the container with the following command:

```bash
docker-compose run [IMAGE_NAME] bash
```

### Using the startup script
You should run the file ```start-wings.sh``` that you will find on the "scripts" folder of this repository: 

```bash
# If [NAME] is not specified, it defaults to wings.
./start-wings.sh [NAME]
```

This file will execute the container with the following options (it is assumed that the image you want to execute is kcapd/wings-base):

```bash
docker run --interactive \
               --tty \
               --env WINGS_MODE='dind' \
               --volume "${NAME}_vol":/opt/wings \
               --name ${NAME} \
               --publish 8080:8080 \
               ${ARGS} kcapd/wings-base
```

And now you can access WINGS' web interface from the Docker image: ```http://localhost:8080/wings-portal```

## Stoping WINGS
There are two ways to stop the WINGS container

### Using docker-compose (recommended)
If not in detached mode, you can stop the container with `ctrl + c`. If in detached mode, you can stop the container using:

```bash
docker-compose stop
```

If you wish to completely remove containers, networks and volumes use:

```bash
docker-compose down
```

**NOTE**: The command above only removes unnamed volumes. If you wish to remove *ALL* volumes use:

```bash 
docker-compose down -v
```

### Using docker

Execute the following command:

```bash
docker stop kcapd/wings-base
```

If you start and stop your container several times, sometimes the volume is not mounted correctly and leads to errors. In those cases you should remove your volume: 

```bash
docker volume rm wings_vol
```
And call the ```start-wings.sh``` script again

**Attention: If you remove the volume, you will delete the data, workflows and executions created on the container.** We recommend that you either save your relevant results in your computer, or that you commit all your changes in the image. Both approaches are further detailed below.

### Copy results from different executions into your local computer

You should follow this approach when saving a few results from your workflow execution.

The easiest way of accessing the results from your workflows is using a web browser: ```http://localhost:8080/wings-portal```, going to "Analysis->Access Runs" or "Advanced ->Manage Data". Whenever a file is downloaded, it will be saved to your local computer.

In order to facilitate saving multiple results from your WINGS dockerized image, you have to **mount a volume**. The volume will be used to copy the results of the workflow to your localhost computer:

1.	Edit the ```start-wings.sh``` script adding another volume after the ```--volume "${NAME}_vol":/opt/wings \``` line: 

```bash
--volume "c:/Users/dgarijo/Desktop/sharedFolder":/out \
```
In the tutorial we are sharing a folder on the local computer on path ```c:/Users/dgarijo/Desktop/sharedFolder```. The shared folder in the container will be called ```out```

2. Execute ```start-wings.sh```

3. Select the folder with results that you want to copy. Unless the Dockerfile is changed, it should be on 
```
cd /opt/wings/storage/default/users/username/domain/
```
4. Copy the results you want to the mounted volume: 
```
cp /opt/wings/storage/default/users/admin/blank/data/out1.txt /out/out1.txt
```

Those result will appear on your shared folder.

### Share a docker image with workflows, new software and data.

In order to save ALL the workflows, executions, new installed software and data from an image, you must follow the next steps:

1. Stop the tomcat service in your WINGS image (for consistency). Execute ```service tomcat8 stop```

2. Copy the "default" folder in the image. You must use the same terminal in which the WINGS image was executing: ```cp -r /opt/wings/storage/ /storage```. Also, you may restart the tomcat service now if you want to continue using WINGS: ```service tomcat8 start```

3. Open a **new terminal** and type: ```docker ps -aq | xargs -I  % docker commit % genomics-new```. This will save your current image as "genomics-new". Please change the name if you want your image tobe saved under a different name. **Note:** this assumes that you are only executing one container. If you are executing several containers at the same time, execute ```docker ps``` and then use the id of your container when committing the new image.

All your contents are now saved on "genomics-new". You can check this by executing ```docker images``` and checking that your new image is bigger in size than the original one.

### Load data, workflows and software from a committed image.

In the previous section we have seen how to save your domains in a local image. However, if you run start-wings.sh once again, you will see that WINGS is not loading everything correctly. You must follow the next steps to address this issue:

1. In your image, stop the tomcat service: ```service tomcat8 stop```.

2. Load the folder we just saved in the right path: ```cp -r /storage/ /opt/wings```

3. Restart the tomcat: ```service tomcat8 start```

And that's it! if you reload your browser you should see all your workflows and domains as you left them.
