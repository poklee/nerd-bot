package xyz.teamnerds.nerdbot.dao;

import java.io.IOException;

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

@Service
public class MongoDBKarmaDatastore implements KarmaDatastore
{

	private String mongoDbUser = System.getenv("MONGODB_USER");
	private String mongoDbPassword = System.getenv("MONGODB_PASSWORD");

	
	@Nonnull
	private MongoClientURI getMongoClientUri() throws IOException
	{
		if (mongoDbUser == null || mongoDbUser.length()==0 || mongoDbPassword == null || mongoDbPassword.length()==0)
		{
			throw new IOException("Could not find mongodb user/password");
		}
		
		MongoClientURI uri = new MongoClientURI("mongodb+srv://" +mongoDbUser+ ":" +mongoDbPassword+ "@cluster0-k5m7w.gcp.mongodb.net/nerdbot?retryWrites=true&w=majority");
		return uri;
	}
	
	@Override
	@CheckForNull
	public Integer getKarmaForUser(@Nonnull String userId) throws IOException
	{
		MongoClientURI uri = getMongoClientUri();
		try (MongoClient mongoClient = new MongoClient(uri))
		{
			MongoDatabase database = mongoClient.getDatabase("nerdbot");
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


	@Override
	public int incrementKarmaForUser(@Nonnull String userId, int amount) throws IOException
	{
		MongoClientURI uri = getMongoClientUri();
	
		// Not sure how to lock, just going to use global lock for now
		synchronized (MongoDBKarmaDatastore.class)
		{
			try (MongoClient mongoClient = new MongoClient(uri))
			{
				MongoDatabase database = mongoClient.getDatabase("nerdbot");
				MongoCollection<Document> userKarmaCollection = database.getCollection("UserKarma");
				
				BasicDBObject dbObject = new BasicDBObject("_id", userId);
				FindIterable<Document> results = userKarmaCollection.find(dbObject);
				Document document = results.first();
				if (document == null)
				{
					Document doc = new Document("_id", userId)
							.append("karma", amount);

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
