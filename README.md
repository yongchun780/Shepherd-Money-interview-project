# Shepherd Money Interview Project - Yongchun Chen

## Description
This project is a Spring Boot application designed for the Shepherd Money interview process. It showcases a backend system capable of managing user profiles and associated credit cards. The application allows for the creation and deletion of users, the addition of credit cards to user profiles, and the updating of credit card balance histories.

The main goal of this project is to demonstrate clean API design, effective data management, and best practices in Spring Boot development. It includes robust error handling and follows clear coding conventions to ensure maintainability and scalability.

## Getting Started

### Prerequisites
List everything needed to get your project up and running:
- JDK 17
- Gradle 
- Any other software or service dependencies

### Installation
Step-by-step guide on setting up a local development environment:
1. Clone the repository:
   ```bash
   git clone https://github.com/yongchun780/shepherd-money-interview-project.git
   ```
2. Navigate to the project directory
   ```bash
   cd [root directory of this project]
   ```
3. Build the project:
   ```bash
   ./gradlew build
   ```
   You may need to check the permission.
### Running the Application
Instructions on how to run the application:
Open Terminal or Command Prompt in the project's root directory, and run:
   ```bash
   ./gradlew build
   ```
The application will start running at `http://localhost:8080`.

## API Usage

### UserController APIs
* `PUT http://localhost:8080/user` - createUser
* `DELETE http://localhost:8080/user` - deleteUser

### CreditCardController APIs
* `POST http://localhost:8080/credit-card` - addCreditCardToUser
* `GET http://localhost:8080/credit-card:all` - getAllCardOfUser
* `GET http://localhost:8080/credit-card:user-id` - getUserIdForCreditCard
* `POST http://localhost:8080/credit-card:update-balance` - updateBalanceHistory
* `GET http://localhost:8080/credit-card:balance-history` - getBalanceHistoryByCreditCardId
   This is for debugging
* `POST http://localhost:8080/credit-card:add-balance` - addBalanceHistory
   This is for debugging


