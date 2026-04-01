package model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Document(collection = "preferences") //Collection Name
public class DecisionPreference {
    @Id
    private String id;

    private Map<String, String> cuisine;
    private Map<String, String> dietary_preferences;
    private Map<String, String> greek_cuisine;
    private Map<String, String> italian_cusine;
    private Map<String, String> mexican_cuisine;
    private Map<String, String> budget_range;
    private Map<String, String> seating;
    private Map<String, String> distance;
}

