package service;

import model.Restaurant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that provides content-based restaurant recommendations
 * using cosine similarity between user preferences and restaurant features.
 */
@Service
public class ContentBasedRecommendationService {

    /**
     * Injest mongo DB template for restaurants
     */
    private final MongoTemplate restaurantMongoTemplate;

    public ContentBasedRecommendationService(@Qualifier("restaurantMongoTemplate") MongoTemplate restaurantMongoTemplate) {
        this.restaurantMongoTemplate = restaurantMongoTemplate;
    }

    /**
     * Recommendations for content-based . Use Cosine similarity to find closes restaurant to user preferences
     * @param userPrefs
     * @return
     */
    public Map<String, Object> recommend(Map<String, Object> userPrefs) {
        List<Restaurant> restaurants = restaurantMongoTemplate.findAll(Restaurant.class);

        Map<String, Double> userVector = flattenPreferences(userPrefs);
        Map<String, Double> scores = new HashMap<>();

        for (Restaurant r : restaurants) {
            Map<String, Double> restaurantVector = extractRestaurantVector(r);
            double similarity = cosineSimilarity(userVector, restaurantVector);
            scores.put(r.getName(), similarity);
        }

        List<Map<String, Object>> recommendations = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("name", entry.getKey());
                    rec.put("score", (int) Math.round(entry.getValue() * 100)); //Scale to 0–100
                    return rec;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recommendations", recommendations);
        return response;
    }

    private Map<String, Double> flattenPreferences(Map<String, Object> prefs) {
        Map<String, Double> flat = new HashMap<>();
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> innerMap) {
                for (Map.Entry<?, ?> sub : innerMap.entrySet()) {
                    String key = (sub.getKey() + "").replace(" ", "").toLowerCase();
                    try {
                        double value = Double.parseDouble(sub.getValue().toString());
                        flat.put(key, value);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return flat;
    }

    /**
     * Converts a restaurant object into a vector representation of its features.
     * @param r
     * @return
     */
    private Map<String, Double> extractRestaurantVector(Restaurant r) {
        Map<String, Double> vector = new HashMap<>();

        //Cuisine
        if (r.getType() != null) {
            vector.put(r.getType().toLowerCase(), 1.0);
        }

        //Dietary preferences
        if (r.getDietary_preferences() != null) {
            for (String d : r.getDietary_preferences()) {
                vector.put(d.toLowerCase(), 1.0);
            }
        }

        //Cuisine-specific dishes
        if (r.getGreek_food() != null) {
            for (String dish : r.getGreek_food()) {
                vector.put(dish.toLowerCase(), 1.0);
            }
        }
        if (r.getItalian_food() != null) {
            for (String dish : r.getItalian_food()) {
                vector.put(dish.toLowerCase(), 1.0);
            }
        }
        if (r.getMexican_food() != null) {
            for (String dish : r.getMexican_food()) {
                vector.put(dish.toLowerCase(), 1.0);
            }
        }

        //Budget, seating, distance
        vector.put(normalize(r.getBudget_range()), 1.0);
        vector.put(normalize(r.getSeating()), 1.0);
        vector.put(normalize(r.getDistance()), 1.0);

        return vector;
    }

    /**
     * Normalizes a string value for comparison by making it lowercase and removing symbols.
     * @param input
     * @return
     */
    private String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase().replace("£", "").replace("-", "to").replace(" ", "");
    }

    /**
     * Calculates cosine similarity between two vectors represented as maps.
     * @param a
     * @param b
     * @return
     */
    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        Set<String> keys = new HashSet<>(a.keySet());
        keys.retainAll(b.keySet());

        double dot = 0.0, normA = 0.0, normB = 0.0;

        for (String key : keys) {
            dot += a.get(key) * b.get(key);
        }

        for (double v : a.values()) normA += v * v;
        for (double v : b.values()) normB += v * v;

        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
