package model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "restaurants") //Collection name
public class Restaurant {
    @Id
    private String id;

    private String name;
    private String type;
    private List<String> dietary_preferences;
    private List<String> greek_food;
    private List<String> italian_food;
    private List<String> mexican_food;
    private String budget_range;
    private String seating;
    private String distance;
}
