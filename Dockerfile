FROM amazoncorretto:17

# Install Chrome and dependencies
RUN yum update -y && \
    yum install -y wget unzip libX11 libXcomposite libXcursor libXdamage libXext \
    libXi libXtst cups-libs libXScrnSaver libXrandr alsa-lib pango atk at-spi2-atk gtk3 && \
    yum clean all

# Install Chrome
RUN wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm && \
    yum install -y ./google-chrome-stable_current_x86_64.rpm && \
    rm google-chrome-stable_current_x86_64.rpm

# Create app and logs directories
WORKDIR /app
RUN mkdir -p /app/logs

# Copy the jar file
COPY target/AM4MailBot_java-1.0-SNAPSHOT.jar app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]