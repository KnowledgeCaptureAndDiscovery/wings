
COMMIT=${TRAVIS_COMMIT::6}
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker build -t kcapd/wings:latest -f wings-docker/docker/default/Dockerfile .
docker tag kcapd/wings kcapd/wings:$COMMIT
docker push kcapd/wings:$COMMIT
docker push kcapd/wings:latest
