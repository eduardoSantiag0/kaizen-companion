
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY libs-repo/ libs-repo/
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package -DskipTests


FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home --shell /usr/sbin/nologin kaizan \
    && mkdir -p /app/data /app/tmp \
    && chown -R kaizan:kaizan /app

COPY --from=builder /build/target/*.jar app.jar

USER kaizan
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
