FROM ubuntu:jammy
COPY  ./build/native/nativeCompile/post-layout-api api-native
EXPOSE 8081
ENTRYPOINT ["./api-native"]
