# CareerConnect - Student Placement Management System

## Technology
- Java 17
- Spring Boot 3.5.5
- Thymeleaf
- JDBC
- MySQL
- HTML/CSS/JavaScript
- Maven

## Important
This project uses MySQL Connector/J 5.1.35 because the existing WAMP installation uses an old MySQL 5.1 server.

## Database
Database name:
placement_db

The project initializes the database using:
- src/main/resources/schema.sql
- src/main/resources/data.sql

## Run
1. Start WAMP/MySQL.
2. Open this project folder in VS Code.
3. Open the terminal.
4. Run:
   mvn clean
5. Then:
   mvn spring-boot:run
6. Open:
   http://localhost:8080

## Demo accounts

Admin:
admin@careerconnect.com
admin123

Student:
rahul@student.com
student123

Company:
hr@technova.com
company123


## Registration
Students and companies can create their own accounts from the Register page. Admin accounts remain controlled by the system and are not publicly registered.
