# Tahadhari AI server (server/ + shared/) only. The apps are built by Gradle and CI, not here.
# Works on any container host (Google Cloud Run, Render, Railway, Fly.io); see README "Deploy the AI server".
#
#   docker build -t tahadhari-server .
#   docker run -p 8080:8080 -e ANTHROPIC_API_KEY=sk-... tahadhari-server
#
# The build needs no Android SDK: only the JVM target of :shared is compiled.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon --console=plain :server:installDist

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 tahadhari
COPY --from=build /src/server/build/install/server /app
USER tahadhari
# Hosts that set PORT (Cloud Run, Render, Railway) override this.
ENV PORT=8080
EXPOSE 8080
CMD ["/app/bin/server"]
