package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;

import java.util.Properties;

public class Database {

    Properties props = new Properties();
    private final String uri = "mongodb+srv://Admin:admin@samplecluster-6cqhx.mongodb.net/test";
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
        logs.append("type", jsonObject.getString("type"));
        logs.append("login", jsonObject.getString("login"));
        logs.append("time", jsonObject.getString("time"));
        collectionLogs.insertOne(logs);
    }

    public void updateFName(String name, String fname) {
        Document search = new Document("fname", name);
        Document found = (Document) collectionUsers.find(search).first();

        if (found != null) {
            Bson updatedValue = new Document("fname", fname);
            Bson updateOperation = new Document("$set", updatedValue);
            collectionUsers.updateOne(found, updateOperation);
        }
    }

    public void updateLName(String surname, String lname) {
        Document search = new Document("lname", surname);
        Document found = (Document) collectionUsers.find(search).first();

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

    public void deleteUser(String login){
        BasicDBObject query = new BasicDBObject();
        query.put("login", login);
        collectionUsers.deleteOne(query);

        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("from").equals(login)){
                    query = new BasicDBObject();
                    query.put("from", login);
                    collectionMessages.deleteOne(query);
                }
            }
        }
    }

    public void login(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (login.equals(object.getString("login"))){
                    Document filterDoc = new Document().append("login", login);
                    Document updateDoc = new Document().append("$set", new Document().append("token", token));
                    collectionUsers.updateOne(filterDoc, updateDoc);
                }
            }
        }
    }

    public void logout(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (login.equals(object.getString("login"))){
                    Document filterDoc = new Document().append("login", login);
                    Document updateDoc = new Document().append("$unset", new Document().append("token", token));
                    collectionUsers.updateOne(filterDoc, updateDoc);
                }
            }
        }
    }

    public boolean findLogin(String login) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login)){
                    return true;
                }
            }
        }
        return false;
    }

    public JSONObject getUser(String login) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login)){
                    return object;
                }
            }
        }
        return null;
    }
}
