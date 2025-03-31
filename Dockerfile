# Stage 1: Choose a base image with Java and tools to install Chrome
# Using Eclipse Temurin (Adoptium) as a good OpenJDK distribution
FROM eclipse-temurin:17-jre-focal

# Set Debconf to Noninteractive (avoids prompts during apt-get)
ENV DEBIAN_FRONTEND=noninteractive

# Install Chrome, fonts, and necessary tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    libappindicator3-1 \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcairo2 \
    libcups2 \
    libdbus-1-3 \
    libexpat1 \
    libfontconfig1 \
    libgbm1 \
    libgcc1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libstdc++6 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrandr2 \
    libxrender1 \
    libxss1 \
    libxtst6 \
    lsb-release \
    xdg-utils \
    # Download and install Google Chrome Stable
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list' \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    # Clean up APT cache
    && rm -rf /var/lib/apt/lists/* \
    # Reset Debconf to default
    && unset DEBIAN_FRONTEND

# Set the working directory inside the container
WORKDIR /app

# Copy the executable JAR file from your build output
# Assumes you run 'docker build' from the project root and the JAR is in 'target/'
COPY target/AM4MailBot_java-1.0-SNAPSHOT.jar app.jar

# Add Chrome options needed for WebDriverManager/Selenium in Docker
# WebDriverManager should detect Chrome now, no need to manually install chromedriver usually
ENV JAVA_OPTS=""

# Command to run the application when the container starts
# It will inherit environment variables passed via `docker run -e`
CMD ["java", "-jar", "app.jar"]