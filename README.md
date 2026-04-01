# Restaurant Recommendation System

A full-stack restaurant recommendation system developed as part of a final-year capstone project. The system collects user preferences through a web-based form, processes them using multiple decision-making algorithms, and returns ranked restaurant recommendations.

The system follows a three-layer architecture consisting of a WordPress frontend, a Spring Boot backend, and MongoDB for storage.

---

## Overview

The project addresses the problem of helping users make restaurant choices based on multiple criteria such as cuisine, dietary preferences, dishes, budget, seating, and distance.

User inputs are converted into structured numerical preferences, sent to the backend as JSON, and processed by different recommendation algorithms. The backend then returns ranked restaurant recommendations.

---

## Key Features

- Structured preference collection through web-based questionnaires  
- REST API built with Spring Boot  
- MongoDB integration for restaurant data and historical preference storage  
- Multiple recommendation approaches implemented and compared  
- Ranked restaurant recommendations returned in JSON format  
- Support for single-selection, weighted-preference, and AI-assisted preference forms  

---

## Implemented Recommendation Methods

- **Bayesian Inference** with weighted probabilities and Laplace smoothing  
- **Collaborative Filtering** using cosine similarity  
- **Content-Based Filtering** using feature vector similarity  
- **Fuzzy Logic** for handling imprecise preferences  

---

## System Architecture

1. **Frontend** — WordPress-based interface  
2. **Backend** — Spring Boot application  
3. **Database** — MongoDB  

Communication between components is handled through RESTful APIs using JSON.

---

## Backend Structure

- `config/` — MongoDB configuration  
- `controller/` — REST endpoints  
- `model/` — data models  
- `service/` — recommendation algorithms  
- `decisionmaking/` — application entry point  

---

## API Endpoints

- `/api/recommend/bayesian`  
- `/api/recommend/collaborative`  
- `/api/recommend/fuzzy`  
- `/api/recommend/content`  

---

## Technologies Used

Java, Spring Boot, MongoDB, REST APIs, WordPress, PHP, JSON, Maven  

---

## Configuration

The backend requires a MongoDB connection string.

Set the environment variable before running the application:

Mac / Linux:

    export MONGO_URI="your-mongodb-uri"

Windows PowerShell:

    $env:MONGO_URI="your-mongodb-uri"

**Note:** Credentials have been removed from this repository for security reasons.

---

## Running the Backend

1. Open the backend project in an IDE (e.g. IntelliJ)  
2. Ensure Java and Maven are installed  
3. Set the `MONGO_URI` environment variable  
4. Build the project  
5. Run the Spring Boot application  

Example:

    mvn clean install

---

## Notes

- Academic capstone project  
- Backend-focused repository  
- Sample data included for demonstration  
- Not intended for production deployment  
