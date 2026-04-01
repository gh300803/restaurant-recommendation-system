package controller;

import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller exposing endpoints for different restaurant recommendation algorithms.
 */
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final BayesianService bayesianService;
    private final CollaborativeFilteringService collaborativeFilteringService;
    private final FuzzyRecommendationService fuzzyRecommendationService;
    private final ContentBasedRecommendationService contentBasedRecommendationService;

    /**
     * Endpoint to generate restaurant recommendations using Bayesian probability.
     * @param database
     * @param userPreferences
     * @return
     */
    @PostMapping("/bayesian")
    public ResponseEntity<Map<String, Object>> recommendBayesian(@RequestHeader(value = "database", defaultValue = "regular") String database,
                                                                 @RequestBody Map<String, Object> userPreferences) {
        Map<String, Object> response = bayesianService.calculateBayesianRecommendations(userPreferences, database);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to generate recommendations using Collaborative Filtering (k-NN).
     * @param database
     * @param userPreferences
     * @return
     */
    @PostMapping("/collaborative")
    public ResponseEntity<Map<String, Object>> recommendCollaborative(@RequestHeader(value = "database", defaultValue = "regular") String database, @RequestBody Map<String, Object> userPreferences) {
        Map<String, Object> response = collaborativeFilteringService.recommendBasedOnSimilarUsers(userPreferences, database);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to generate recommendations using Fuzzy Logic.
     * @param userPreferences
     * @return
     */
    @PostMapping("/fuzzy")
    public ResponseEntity<Map<String, Object>> recommendFuzzy(@RequestBody Map<String, Object> userPreferences) {
        Map<String, Object> response = fuzzyRecommendationService.recommend(userPreferences);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to generate recommendations using Content-Based Filtering with Cosine Similarity.
     * @param userPreferences
     * @return
     */
    @PostMapping("/content")
    public ResponseEntity<Map<String, Object>> recommendContentBased(@RequestBody Map<String, Object> userPreferences) {
        Map<String, Object> response = contentBasedRecommendationService.recommend(userPreferences);
        return ResponseEntity.ok(response);
    }
}