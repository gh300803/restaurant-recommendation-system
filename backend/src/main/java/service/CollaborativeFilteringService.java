package service;

import model.DecisionPreference;
import model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class that provides collaborative filtering-based restaurant recommendations.
 *
 * It uses cosine similarity to compare the current user’s preferences with historical data
 *  recommends restaurants liked by the most k similar users.
 */
@Service
public class CollaborativeFilteringService {

    @Autowired
    @Qualifier("decisionMongoTemplate")
    private MongoTemplate decisionMongoTemplate;

    @Autowired
    @Qualifier("decisionMongoTemplatePreferences")
    private MongoTemplate decisionMongoTemplateWeightPreferences;

    @Autowired
    @Qualifier("decisionMongoTemplatePreferencesAI")
    private MongoTemplate decisionMongoTemplateWeightPreferencesAI;

    @Autowired
    @Qualifier("restaurantMongoTemplate")
    private MongoTemplate restaurantMongoTemplate;

    /**
     * Recommends restaurants based on similar historical user preferences using collaborative filtering (k-NN in our case k = 3).
     * @param userPrefs
     * @param dbType
     * @return
     */
    public Map<String, Object> recommendBasedOnSimilarUsers(Map<String, Object> userPrefs, String dbType) {

        MongoTemplate activeDecisionTemplate = decisionMongoTemplate;

        if ("regular".equalsIgnoreCase(dbType)) {
            activeDecisionTemplate = decisionMongoTemplate;
        }


        if ("preferences".equalsIgnoreCase(dbType)) {
            activeDecisionTemplate = decisionMongoTemplateWeightPreferences;
        }
        if ("preferencesAI".equalsIgnoreCase(dbType)) {
            activeDecisionTemplate = decisionMongoTemplateWeightPreferencesAI;
        }

        List<DecisionPreference> history = activeDecisionTemplate.findAll(DecisionPreference.class);
        List<Restaurant> restaurants = restaurantMongoTemplate.findAll(Restaurant.class);

        //Convert user preferences into vector
        Map<String, Double> currentUserVector = flattenPreferences(userPrefs);

        List<SimilarUser> similarUsers = new ArrayList<>();


        //k-NN (k-nearest neighbors) where k = 3 in our case.
        for (DecisionPreference dp : history) {
            Map<String, Double> historicalVector = flattenPreferences(dp);
            double similarity = cosineSimilarity(currentUserVector, historicalVector);
            if (similarity > 0.0) {
                similarUsers.add(new SimilarUser(dp, similarity));
            }
        }
        similarUsers.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        List<SimilarUser> topUsers = similarUsers.stream().limit(3).toList();


        Map<String, Double> restaurantScores = new HashMap<>();
        for (SimilarUser user : topUsers) {
            for (Restaurant r : restaurants) {
                double score = 1.0;

                if (r.getType() != null)
                    score *= getScore(user.dp.getCuisine(), r.getType().toLowerCase());

                if (r.getDietary_preferences() != null) {
                    for (String dp : r.getDietary_preferences()) {
                        double val = getScore(user.dp.getDietary_preferences(), dp.toLowerCase());
                        if (val == 0.0) continue; //Skip zero-weight preferences
                        score *= val;
                    }
                }

                List<String> dishes = getDishes(r);
                Map<String, String> dishMap = getDishMap(r.getType(), user.dp);
                for (String dish : dishes) {
                    double val = getScore(dishMap, dish.toLowerCase());
                    if (val == 0.0) continue; //Skip zero-weight dishes
                    score *= val;
                }

                if (r.getBudget_range() != null) {
                    String budgetKey = normalize(r.getBudget_range()); //Remove symbols to normalize it
                    double val = getScore(user.dp.getBudget_range(), budgetKey);
                    if (val > 0.0) score *= val;
                }

                if (r.getSeating() != null) {
                    String seatingKey = normalize(r.getSeating());
                    double val = getScore(user.dp.getSeating(), seatingKey);
                    if (val > 0.0) score *= val;
                }

                if (r.getDistance() != null) {
                    String distanceKey = normalize(r.getDistance());
                    double val = getScore(user.dp.getDistance(), distanceKey);
                    if (val > 0.0) score *= val;
                }


                if (score > 0.0) {
                    restaurantScores.merge(r.getName(), score * user.similarity, Double::sum);
                }
            }
        }

        List<Map<String, Object>> recommendations = restaurantScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("score", (int) Math.round(entry.getValue()));
                    return map;
                })
                .collect(Collectors.toList());

        //Wrap in final response
        Map<String, Object> response = new HashMap<>();
        response.put("recommendations", recommendations);
        return response;
    }

    /**
     * Flattens a nested preferences map into a flat map with numeric weights.
     * @param prefs
     * @return
     */
    private Map<String, Double> flattenPreferences(Map<String, Object> prefs) {
        Map<String, Double> flat = new HashMap<>();
        for (Map.Entry<String, Object> entry : prefs.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> innerMap) {
                for (Map.Entry<?, ?> sub : innerMap.entrySet()) {
                    String key = (sub.getKey() + "").replace(" ", "").toLowerCase();
                    try {
                        double value = Double.parseDouble(sub.getValue().toString());
                        flat.put(key, value);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return flat;
    }

    /**
     * Flattens a DecisionPreference object into a flat vector map.
     * @param dp
     * @return
     */
    private Map<String, Double> flattenPreferences(DecisionPreference dp) {
        Map<String, Object> combined = new HashMap<>();
        combined.put("cuisine", dp.getCuisine());
        combined.put("dietary_preferences", dp.getDietary_preferences());
        combined.put("greek_cuisine", dp.getGreek_cuisine());
        combined.put("italian_cusine", dp.getItalian_cusine());
        combined.put("mexican_cuisine", dp.getMexican_cuisine());
        combined.put("budget_range", dp.getBudget_range());
        combined.put("seating", dp.getSeating());
        combined.put("distance", dp.getDistance());

        return flattenPreferences(combined);
    }

    /**
     * Computes cosine similarity between two preference vectors.
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

    /**
     * gets a numeric score from a map using a key.
     * @param map
     * @param key
     * @return
     */
    private double getScore(Map<String, String> map, String key) {
        if (map == null || !map.containsKey(key)) return 1.0;
        try {
            return Double.parseDouble(map.get(key));
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    private Map<String, String> getDishMap(String type, DecisionPreference dp) {
        return switch (type.toLowerCase()) {
            case "greek" -> dp.getGreek_cuisine();
            case "italian" -> dp.getItalian_cusine();
            case "mexican" -> dp.getMexican_cuisine();
            default -> null;
        };
    }

    /**
     * Retrieves the list of dishes for a given restaurant based on cuisine type.
     * @param r
     * @return
     */
    private List<String> getDishes(Restaurant r) {
        return switch (r.getType().toLowerCase()) {
            case "greek" -> r.getGreek_food();
            case "italian" -> r.getItalian_food();
            case "mexican" -> r.getMexican_food();
            default -> new ArrayList<>();
        };
    }

    /**
     * Internal class used to store a similar user and their similarity score.
     */
    private static class SimilarUser {
        DecisionPreference dp;
        double similarity;

        SimilarUser(DecisionPreference dp, double similarity) {
            this.dp = dp;
            this.similarity = similarity;
        }
    }


    /**
     * Normalizes a string by lowercasing and removing extra characters for better matching.
     * @param input
     * @return
     */
    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replace("£", "").replace("-", "to").replace(" ", "");
    }

}

