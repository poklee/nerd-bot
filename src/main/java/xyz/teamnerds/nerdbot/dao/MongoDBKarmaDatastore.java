package xyz.teamnerds.nerdbot.dao;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.bson.Document;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Implementation that uses MongoDB to store the data
 */
@Service
public class MongoDBKarmaDatastore implements KarmaDatastore
{

    private String mongoDbUser = System.getenv("MONGODB_USER");
    private String mongoDbPassword = System.getenv("MONGODB_PASSWORD");
    private String mongoDbDatabase = System.getenv("MONGODB_DATABASE");
    private String mongoDbHost = System.getenv("MONGODB_HOST");

    @Nonnull
    private MongoClientURI getMongoClientUri() throws IOException
    {
        if (mongoDbUser == null || mongoDbUser.length() == 0 || mongoDbPassword == null
                || mongoDbPassword.length() == 0)
        {
            throw new IOException("Could not find mongodb user/password");
        }

        String connectionString = String.format("mongodb+srv://%s:%s@%s/%s?retryWrites=true&w=majority", mongoDbUser,
                mongoDbPassword, mongoDbHost, mongoDbDatabase);

        MongoClientURI uri = new MongoClientURI(connectionString);
        return uri;
    }

    @Override
    @CheckForNull
    public Integer getKarmaForUser(@Nonnull String userId) throws IOException
    {
        MongoClientURI uri = getMongoClientUri();
        try (MongoClient mongoClient = new MongoClient(uri))
        {
            MongoDatabase database = mongoClient.getDatabase(mongoDbDatabase);
            MongoCollection<Document> userKarmaCollection = database.getCollection("UserKarma");

            BasicDBObject dbObject = new BasicDBObject("_id", userId);
            FindIterable<Document> results = userKarmaCollection.find(dbObject);
            Document document = results.first();
            if (document == null)
            {
                return null;
            }
            else
            {
                return document.getInteger("karma");
            }
        }
    }

    @Nonnull
    @Override
    public List<UserKarma> getUserKarmaRankings() throws IOException
    {
        // TODO: Query mongo
        return Collections.emptyList();
    }

    @Override
    public int incrementKarmaForUser(@Nonnull String userId, int amount) throws IOException
    {
        MongoClientURI uri = getMongoClientUri();

        // Not sure how to lock, just going to use global lock for now
        synchronized (MongoDBKarmaDatastore.class)
        {
            try (MongoClient mongoClient = new MongoClient(uri))
            {
                MongoDatabase database = mongoClient.getDatabase(mongoDbDatabase);
                MongoCollection<Document> userKarmaCollection = database.getCollection("UserKarma");

                BasicDBObject dbObject = new BasicDBObject("_id", userId);
                FindIterable<Document> results = userKarmaCollection.find(dbObject);
                Document document = results.first();
                if (document == null)
                {
                    Document doc = new Document("_id", userId).append("karma", amount);

                    userKarmaCollection.insertOne(doc);
                    return amount;
                }
                else
                {
                    document.put("karma", document.getInteger("karma") + amount);
                    userKarmaCollection.findOneAndReplace(dbObject, document);
                    return document.getInteger("karma");
                }
            }
        }
    }

}
