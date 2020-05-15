package sample;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;

public class Database {
    String uri = "mongodb+srv://Admin:admin@samplecluster-6cqhx.mongodb.net/test";
    MongoClientURI clientURI = new MongoClientURI(uri);
    MongoClient mongoClient = new MongoClient(clientURI);

    MongoDatabase mongoDatabase = mongoClient.getDatabase("SchoolProject");
    MongoCollection<Document> collectionLogs = mongoDatabase.getCollection("collectionLogs");
    MongoCollection<Document> collectionUsers = mongoDatabase.getCollection("collectionUsers");
    MongoCollection<Document> collectionMessages = mongoDatabase.getCollection("collectionMessages");

    Document users = new Document();
    Document messages = new Document();
    Document logs = new Document();

    public void insertUser(JSONObject jsonObject) {
        users.append("fname", jsonObject.getString("fname"));
        users.append("lname", jsonObject.getString("lname"));
        users.append("login", jsonObject.getString("login"));
        users.append("password", jsonObject.getString("password"));
        collectionUsers.insertOne(users);
    }

    public void insertMessage(JSONObject jsonObject) {
        messages.append("from", jsonObject.getString("from"));
        messages.append("message", jsonObject.getString("message"));
        messages.append("to", jsonObject.getString("to"));
        collectionMessages.insertOne(messages);
    }

    public void log(JSONObject jsonObject) {
        logs.append("type", jsonObject.getString("logType"));
        logs.append("login", jsonObject.getString("login"));
        logs.append("time", jsonObject.getString("timeStamp"));
        collectionLogs.insertOne(logs);
    }

    public void updateFName(String name, String fname) {
        Document search = new Document("fname", name);
        Document found = (Document)collectionUsers.find(search).first();

        if (found != null) {
            Bson updatedValue = new Document("fname", fname);
            Bson updateOperation = new Document("$set", updatedValue);
            collectionUsers.updateOne(found, updateOperation);
        }
    }

    public void updateLName(String surname, String lname) {
        Document search = new Document("lname", surname);
        Document found = (Document)collectionUsers.find(search).first();

        if (found != null) {
            Bson updatedValue = new Document("lname", lname);
            Bson updateOperation = new Document("$set", updatedValue);
            collectionUsers.updateOne(found, updateOperation);
        }
    }

    public void changePassword(String login, String hashPass) {
        Document search = new Document("login", login);
        Document found = collectionUsers.find(search).first();

        if (found != null) {
            Bson updatedValue = new Document("password", hashPass);
            Bson updateOperation = new Document("$set", updatedValue);
            collectionUsers.updateOne(found, updateOperation);
        }
    }


}
