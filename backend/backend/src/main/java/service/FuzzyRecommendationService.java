package service;

import model.Restaurant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for restaurant recommendation using Fuzzy Logic.
 */
@Service
public class FuzzyRecommendationService {

    private final MongoTemplate restaurantMongoTemplate;

    public FuzzyRecommendationService(@Qualifier("restaurantMongoTemplate") MongoTemplate restaurantMongoTemplate) {
        this.restaurantMongoTemplate = restaurantMongoTemplate;
    }

    /**
     * Main method to return top 3 fuzzy-matched restaurants based on user preferences.
     * @param userPrefs
     * @return
     */
    public Map<String, Object> recommend(Map<String, Object> userPrefs) {
        List<Restaurant> restaurants = restaurantMongoTemplate.findAll(Restaurant.class);

        Map<String, String> budgetPref = castToStringMap(userPrefs.get("budget_range"));
        Map<String, String> distancePref = castToStringMap(userPrefs.get("distance"));
        Map<String, String> cuisinePref = castToStringMap(userPrefs.get("cuisine"));
        Map<String, String> seatingPref = castToStringMap(userPrefs.get("seating"));
        Map<String, String> dietaryPref = castToStringMap(userPrefs.get("dietary_preferences"));

        Map<String, Double> scores = new HashMap<>();

        for (Restaurant r : restaurants) {
            double score = 1.0;

            String normalizedBudget = normalize(r.getBudget_range());
            String normalizedDistance = normalize(r.getDistance());
            String normalizedCuisine = r.getType().toLowerCase();
            String normalizedSeating = normalize(r.getSeating());


            score *= fuzzyScore(normalizedBudget, budgetPref);
            score *= fuzzyScore(normalizedDistance, distancePref);
            score *= fuzzyScore(normalizedCuisine, cuisinePref);
            score *= fuzzyScore(normalizedSeating, seatingPref);

            if (r.getDietary_preferences() != null) {
                for (String tag : r.getDietary_preferences()) {
                    score *= fuzzyScore(tag.toLowerCase(), dietaryPref);
                }
            }

            // Fuzzy scoring for cuisine-specific dishes
            List<String> dishes = getDishes(r);
            Map<String, String> dishMap = castToStringMap(userPrefs.get(r.getType().toLowerCase() + "_cuisine"));
            for (String d : dishes) {
                score *= fuzzyScore(d.toLowerCase(), dishMap);
            }
            scores.put(r.getName(), score);
        }

        List<Map<String, Object>> recommendations = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("name", entry.getKey());
                    rec.put("score", (int) Math.round(entry.getValue() * 100)); // scale to 0–100
                    return rec;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recommendations", recommendations);
        return response;
    }

    /**
     * Calculates fuzzy score for a specific attribute based on user preference weights.
     * @param key
     * @param userPrefs
     * @return
     */
    private double fuzzyScore(String key, Map<String, String> userPrefs) {
        if (!userPrefs.containsKey(key)) return 0.2;
        try {
            double weight = Double.parseDouble(userPrefs.get(key));
            if (weight == 0.0) return 0.2;
            return 0.5 + (0.5 * (weight / 3.0));
        } catch (NumberFormatException e) {
            return 0.2;
        }
    }

    /**
     * Function to normalize values to be able to map them
     * @param input
     * @return
     */
    private String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase().replace("£", "").replace("-", "to").replace(" ", "");
    }

    private List<String> getDishes(Restaurant r) {
        return switch (r.getType().toLowerCase()) {
            case "greek" -> r.getGreek_food();
            case "italian" -> r.getItalian_food();
            case "mexican" -> r.getMexican_food();
            default -> new ArrayList<>();
        };
    }

    private Map<String, String> castToStringMap(Object obj) {
        try {
            return (Map<String, String>) obj;
        } catch (ClassCastException e) {
            return new HashMap<>();
        }
    }
}
