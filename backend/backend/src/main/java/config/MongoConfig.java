package config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    private static final String URI = System.getenv("MONGO_URI");

    @Primary
    @Bean(name = "decisionMongoTemplate")
    public MongoTemplate decisionMongoTemplate() {
        return new MongoTemplate(MongoClients.create(URI), "Decisions");//Decision DB with Preferences
    }

    @Bean(name = "restaurantMongoTemplate")
    public MongoTemplate restaurantMongoTemplate() {
        return new MongoTemplate(MongoClients.create(URI), "Restaurants"); //Restaurants DB
    }

    @Bean(name = "decisionMongoTemplatePreferences")
    public MongoTemplate decisionMongoTemplatePreferences() {
        return new MongoTemplate(MongoClients.create(URI), "DecisionPreferences"); //DecisionDB for Preferences
    }

    @Bean(name = "decisionMongoTemplatePreferencesAI")
    public MongoTemplate decisionMongoTemplatePreferencesAI() {
        return new MongoTemplate(MongoClients.create(URI), "DecisionPreferencesAI"); //DecisionDB for Preferences AI
    }
}
