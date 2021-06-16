package com.evilbas.discgm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "com.evilbas.discgm.dao.mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Autowired
    Environment env;

    @Override
    protected String getDatabaseName() {
        return "rsl_game";
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(env.getProperty("mongo.db.url"));
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder().applyConnectionString(connectionString)
                .build();

        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    @Override
    protected String getMappingBasePackage() {
        return "com.evilbas.discgm.dao.mongo";
    }
}