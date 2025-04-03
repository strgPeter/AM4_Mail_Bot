# AM4MailBot_java

A Java-based automation bot for monitoring fuel and CO2 prices in the browser game Airline Manager 4, sending email notifications when prices drop below defined thresholds.

## Overview

This application uses Selenium WebDriver to scrape pricing data from Airline Manager 4 at regular intervals. When fuel or CO2 prices drop below configurable thresholds, it sends notification emails to registered recipients.

## Features

- Automated login to Airline Manager 4
- Scheduled price checks at configurable intervals
- Email notifications when prices drop below thresholds
- Configurable time windows for operation (to avoid running during off-hours)
- Robust error handling and logging
- Docker support for containerized deployment

## Requirements

- Java 17+ (configured for Java 22 in pom.xml)
- Maven
- Chrome browser (for the Selenium WebDriver)
- Gmail account for sending notifications
- Airline Manager 4 account

## Configuration

The application is configured via environment variables:

| Variable | Description | Required |
|----------|-------------|----------|
| AIRLINE_MANAGER_EMAIL | Your AM4 account email | Yes |
| AIRLINE_MANAGER_PASSWORD | Your AM4 account password | Yes |
| MAIL_SENDER_USN | Gmail address for sending notifications | Yes |
| MAIL_SENDER_PWD | App password for the Gmail account | Yes |
| MAIL_RECIPIENT_1 | Email address to receive notifications | Yes |

## Default Settings

- Fuel price threshold: 800
- CO2 price threshold: 130
- Check interval: 30 minutes
- Operating hours: 06:00 - 23:01

These settings can be modified in the `Main.java` file.

## Building

```bash
mvn clean package
```

This will generate a JAR file at `target/AM4MailBot_java-1.0-SNAPSHOT.jar`

## Running Locally

```bash
export AIRLINE_MANAGER_EMAIL=your_email@example.com
export AIRLINE_MANAGER_PASSWORD=your_password
export MAIL_SENDER_USN=your_gmail@gmail.com
export MAIL_SENDER_PWD=your_gmail_password
export MAIL_RECIPIENT_1=recipient@example.com

java -jar target/AM4MailBot_java-1.0-SNAPSHOT.jar
```

## Docker Deployment

### Building the Docker Image

```bash
mvn clean package
docker build -t am4mailbot .
```

### Running with Docker

```bash
docker run -d \
  -e AIRLINE_MANAGER_EMAIL=your_email@example.com \
  -e AIRLINE_MANAGER_PASSWORD=your_password \
  -e MAIL_SENDER_USN=your_gmail@gmail.com \
  -e MAIL_SENDER_PWD=your_gmail_password \
  -e MAIL_RECIPIENT_1=recipient@example.com \
  --name am4mailbot \
  am4mailbot
```

## Scheduling

The application is designed to run:
- Every 30 minutes (configurable)
- Starting at xx:01 or xx:31 of each hour
- Only between 06:00 and 23:01

## Logs

- When running locally: Logs are written to `logs/app.log` and to console
- When running in Docker: Logs are written to `/app/logs/app.log` and can be viewed with `docker logs am4mailbot`

## Project Structure

- `MailClient.java`: Handles email notifications
- `Main.java`: Application entry point and scheduling logic
- `ScrapeResult.java`: Data model for scraped prices
- `Scraper.java`: Selenium-based web scraping implementation

## Notes

- For Gmail accounts, you may need to use an "App Password" instead of your regular password
- The application requires a headless Chrome browser, which is automatically installed in the Docker image
- If running locally, ensure ChromeDriver is installed or let WebDriverManager handle it automatically

## License

This project is open-source software.

## Author

Originally created by Peter (https://github.com/strgPeter)
