# Restaurant Recommendation System

A full-stack restaurant recommendation system developed as part of a final-year capstone project. The system collects user preferences through a web-based form, processes them through multiple decision-making algorithms, and returns ranked restaurant recommendations. It was designed around a three-layer architecture consisting of a WordPress frontend, a Spring Boot backend, and MongoDB for storage. :contentReference[oaicite:0]{index=0} 

## Overview

The project addresses the problem of helping users make restaurant choices based on multiple criteria such as cuisine, dietary preferences, dishes, budget, seating, and distance. User inputs are converted into structured numerical preferences, sent to the backend as JSON, and processed by different recommendation algorithms. The backend then stores and returns the top restaurant recommendations. :contentReference[oaicite:2]{index=2}

## Key Features

- Structured preference collection through web-based questionnaires
- REST API built with Spring Boot
- MongoDB integration for restaurant data and historical preference storage
- Multiple recommendation approaches implemented and compared
- Ranked restaurant recommendations returned in JSON format
- Support for single-selection, weighted-preference, and AI-assisted preference forms 

## Implemented Recommendation Methods

The backend implements four recommendation strategies:

- **Bayesian Inference** with weighted probabilities and Laplace smoothing
- **Collaborative Filtering** using cosine similarity over historical user preferences
- **Content-Based Filtering** using cosine similarity between user and restaurant feature vectors
- **Fuzzy Logic** for handling partial and imprecise preferences 

## System Architecture

The system is organised into three main layers:

1. **Frontend**: WordPress-based interface used to collect user preferences
2. **Backend**: Spring Boot application exposing recommendation endpoints
3. **Database**: MongoDB used to store restaurants, user preferences, and recommendation results

Communication between components is handled through RESTful APIs using JSON. 

## Backend Structure

The backend includes:

- `config/` — MongoDB configuration
- `controller/` — REST controller exposing recommendation endpoints
- `model/` — MongoDB document models for preferences and restaurants
- `service/` — algorithm implementations for Bayesian, collaborative, content-based, and fuzzy recommendations
- `decisionmaking/` — Spring Boot application entry point 

## API Endpoints

The backend exposes the following endpoints:

- `/api/recommend/bayesian`
- `/api/recommend/collaborative`
- `/api/recommend/fuzzy`
- `/api/recommend/content`

The Bayesian and collaborative endpoints support selecting different historical datasets through a request header. 

## Data Model

The system stores:

- restaurant metadata such as cuisine, dishes, budget, seating, and distance
- user preference records for historical analysis
- separate datasets for regular, weighted, and AI-assisted preferences 

## Evaluation Summary

The project evaluated recommendation quality using user feedback across different input styles. Content-based filtering and fuzzy logic performed better when historical data was limited, while Bayesian inference and collaborative filtering depended more heavily on the quality and quantity of historical data. Weighted user input produced the strongest satisfaction scores overall. :contentReference[oaicite:9]{index=9}

## Technologies Used

- Java
- Spring Boot
- MongoDB
- REST APIs
- WordPress
- PHP
- JSON
- Maven 

## Environment Variables

Before running the backend, set your MongoDB connection string as an environment variable.

### Mac / Linux
`export MONGO_URI="your-mongodb-uri"`

### Windows PowerShell
`$env:MONGO_URI="your-mongodb-uri"`

## Running the Backend

1. Open the backend project in IntelliJ IDEA or another Java IDE
2. Make sure Java and Maven are installed
3. Set the `MONGO_URI` environment variable
4. Build the project
5. Run the Spring Boot application

Example build command:

`mvn clean install` 

## Notes

- This repository is adapted from an academic capstone project
- The original system used a WordPress frontend, but this repository focuses mainly on the backend recommendation system and sample data
- Sample database files are included to illustrate the structure of stored preferences and recommendation outputs
- This project was designed as a functional prototype and evaluation platform rather than a production deployment 