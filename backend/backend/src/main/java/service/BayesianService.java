package service;

import lombok.RequiredArgsConstructor;
import model.DecisionPreference;
import model.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculates restaurant recommendations using Bayesian probability based on user preferences and historical data.
 */
@Service
@RequiredArgsConstructor
public class BayesianService {


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
     * Calculates weighted probability for a given key based on Laplace smoothing and user weight
     * @param userPrefs
     * @param dbType
     * @return
     */
    public Map<String, Object> calculateBayesianRecommendations(Map<String, Object> userPrefs, String dbType) {
        MongoTemplate activeDecisionTemplate = decisionMongoTemplate;


        //Choose which DB we will take historical data
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

        Map<String, Double> scores = new HashMap<>();

        //Loop through all the restaurants
        for (Restaurant r : restaurants) {
            double score = 1.0;

            //Calculate Weight propability based on user choice for cuisine
            Map<String, String> cuisineMap = castToStringMap(userPrefs.get("cuisine"));
            score = score * weightedProbability("cuisine", r.getType().toLowerCase(), cuisineMap, history);

            //Calculate Weight propability based on user choice for dietary_preferences
            Map<String, String> dietaryMap = castToStringMap(userPrefs.get("dietary_preferences"));
            for (String dp : r.getDietary_preferences()) {
                score = score * weightedProbability("dietary_preferences", dp.toLowerCase(), dietaryMap, history);
            }

            //Calculate Weight propability based on user choice for dishes (greek, or italian, or mexican)
            List<String> dishes = getDishes(r);
            Map<String, String> dishMap = castToStringMap(userPrefs.get(r.getType().toLowerCase() + "_cuisine"));
            for (String d : dishes) {
                score = score * weightedProbability(r.getType().toLowerCase() + "_cuisine", d.toLowerCase(), dishMap, history);
            }

            //Calculate Weight propability based on user choice for budget_range
            Map<String, String> budgetMap = castToStringMap(userPrefs.get("budget_range"));
            score = score * weightedProbability("budget_range", normalize(r.getBudget_range()), budgetMap, history);

            //Calculate Weight propability based on user choice for seating
            Map<String, String> seatingMap = castToStringMap(userPrefs.get("seating"));
            score = score * weightedProbability("seating", normalize(r.getSeating()), seatingMap, history);

            //Calculate Weight propability based on user choice for distance
            Map<String, String> distanceMap = castToStringMap(userPrefs.get("distance"));
            score = score * weightedProbability("distance", normalize(r.getDistance()), distanceMap, history);

            scores.put(r.getName(), score);
        }

        List<Map<String, Object>> recommendations = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    double roundedScore = Math.round(entry.getValue() * 10000.0) / 10000.0;
                    map.put("score", roundedScore); // raw Bayesian probability
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("recommendations", recommendations);
        return response;
    }

    /**
     * Calculates weighted probability for a given key based on Laplace smoothing and user weight.
     * @param category
     * @param key
     * @param userPrefs
     * @param history
     * @return
     */
    private double weightedProbability(String category, String key, Map<String, String> userPrefs, List<DecisionPreference> history) {
        int count = 0, total = 0;

        //Loop through historical data and
        for (DecisionPreference dp : history) {
            Map<String, String> map = extractMap(dp, category);
            if (map != null && map.containsKey(key)) {
                try {
                    count += Integer.parseInt(map.get(key));
                } catch (NumberFormatException ignored) {
                }
                total++;
            }
        }

        //Laplace thats why we add +1
        double basePropability = (count + 1.0) / (total + 1.0);

        //Remove Spaces for batter checkup
        String normalizeKey = key.replace(" ", "").toLowerCase();

        //Weight if user Preference is there else default to 1
        String weightForUserPreference = userPrefs.getOrDefault(normalizeKey, "1");

        double weight = 1.0;
        try {
            weight = Double.parseDouble(weightForUserPreference);
        } catch (NumberFormatException ignored) {
        }

        // user gave 0 weight (doesn't care), skip this field entirely
        if (weight == 0.0) {
            return 1.0; // marker to skip multiplication
        }

        //Apply exponential weight
        return basePropability * (1 + 0.25 * weight);
    }

    /**
     * Extracts the correct map from a DecisionPreference object based on the given category.
     * @param dp
     * @param category
     * @return
     */
    private Map<String, String> extractMap(DecisionPreference dp, String category) {
        return switch (category) {
            case "cuisine" -> dp.getCuisine();
            case "dietary_preferences" -> dp.getDietary_preferences();
            case "greek_cuisine" -> dp.getGreek_cuisine();
            case "italian_cusine" -> dp.getItalian_cusine();
            case "mexican_cuisine" -> dp.getMexican_cuisine();
            case "budget_range" -> dp.getBudget_range();
            case "seating" -> dp.getSeating();
            case "distance" -> dp.getDistance();
            default -> null;
        };
    }

    /**
     *
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
     * Normalizes a string by removing symbols and making it lowercase.
     * @param input
     * @return
     */
    private String normalize(String input) {
        return input == null ? "" : input.toLowerCase().replace("£", "").replace("-", "to").replace(" ", "");
    }

    /**
     * Safely casts an object to a Map<String, String>, if valid.
     * @param obj
     * @return
     */
    private Map<String, String> castToStringMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> (String) e.getValue()
                    ));
        }
        return new HashMap<>();
    }
}
