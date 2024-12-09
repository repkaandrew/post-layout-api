FROM gradle:jdk21-graal-jammy AS build
COPY --chown=gradle:gradle . /home/src
WORKDIR /home/src
RUN gradle nativeCompile --no-daemon

FROM ubuntu:jammy
COPY --from=build home/src/build/native/nativeCompile/* api-native
EXPOSE 8081
ENTRYPOINT ["./api-native"]
