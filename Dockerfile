FROM openjdk:15

copy . /jrunner5
workdir /jrunner5

run ./gradlew build
cmd ./gradlew run
