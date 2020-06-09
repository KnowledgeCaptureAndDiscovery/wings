docker build -t kcapd/wings:latest -f wings-docker/docker/default/Dockerfile .
docker tag kcapd/wings kcapd/wings:$1
docker push kcapd/wings:$1
docker push kcapd/wings:latest
