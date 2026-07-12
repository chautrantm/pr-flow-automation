FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY src/ src/

RUN mkdir -p out && \
    javac -d out src/main/java/com/example/prflow/*.java

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "out", "com.example.prflow.GithubPrWebhookServer"]
