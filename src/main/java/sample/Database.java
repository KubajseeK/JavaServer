package sample;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONObject;

public class Database {
    String uri = "mongodb+srv://Admin:admin@samplecluster-6cqhx.mongodb.net/test";
    MongoClientURI clientURI = new MongoClientURI(uri);
    MongoClient mongoClient = new MongoClient(clientURI);

    MongoDatabase mongoDatabase = mongoClient.getDatabase("SchoolProject");
    MongoCollection<Document> collectionLogs = mongoDatabase.getCollection("collectionLogs");
    MongoCollection<Document> collectionUsers = mongoDatabase.getCollection("collectionUsers");
    MongoCollection<Document> collectionMessages = mongoDatabase.getCollection("collectionMessages");

    public void insertUser(JSONObject jsonObject) {
        Document users = new Document();
        users.append("fname", jsonObject.getString("fname"));
        users.append("lname", jsonObject.getString("lname"));
        users.append("login", jsonObject.getString("login"));
        users.append("password", jsonObject.getString("password"));
        collectionUsers.insertOne(users);
    }

    public void insertMessage(JSONObject jsonObject) {
        Document messages = new Document();
        messages.append("from", jsonObject.getString("from"));
        messages.append("message", jsonObject.getString("message"));
        messages.append("to", jsonObject.getString("to"));
        collectionMessages.insertOne(messages);
    }

    public void log(JSONObject jsonObject) {
        Document logs = new Document();
        logs.append("type", jsonObject.getString("logType"));
        logs.append("login", jsonObject.getString("login"));
        logs.append("time", jsonObject.getString("timeStamp"));
        collectionLogs.insertOne(logs);
    }


}
