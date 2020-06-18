package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class Database {

    private final JSONObject config = getConfig();
    private final String uri = config.getString("uri");
    MongoClientURI clientURI = new MongoClientURI(uri);
    MongoClient mongoClient = new MongoClient(clientURI);

    MongoDatabase mongoDatabase = mongoClient.getDatabase(config.getString("databaseName"));
    MongoCollection<Document> collectionLogs = mongoDatabase.getCollection("collectionLogs");
    MongoCollection<Document> collectionUsers = mongoDatabase.getCollection("collectionUsers");
    MongoCollection<Document> collectionMessages = mongoDatabase.getCollection("collectionMessages");

    Document users = new Document();
    Document messages = new Document();
    Document logs = new Document();


    public JSONObject getConfig() {
        JSONObject config = new JSONObject();
        JSONParser parser = new JSONParser();

        try {
            org.json.simple.JSONObject obj = (org.json.simple.JSONObject) parser.parse(new FileReader("src/main/java/sample/config"));

            config.put("uri", obj.get("uri"));
            config.put("port", obj.get("port"));
            config.put("databaseName", obj.get("databaseName"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    public void insertUser(JSONObject jsonObject) {
        users.append("fname", jsonObject.getString("fname"));
        users.append("lname", jsonObject.getString("lname"));
        users.append("login", jsonObject.getString("login"));
        users.append("password", jsonObject.getString("password"));

        collectionUsers.insertOne(users);
    }

    public void insertMessage(JSONObject jsonObject){
        Document document = new Document();
        document.append("from", jsonObject.getString("from"));
        document.append("message", jsonObject.getString("message"));
        document.append("to", jsonObject.getString("to"));
        collectionMessages.insertOne(document);
    }

    public void logLogout(JSONObject jsonObject) {
        Document document = new Document();
        document.append("type", "logout");
        document.append("login", jsonObject.getString("login"));
        document.append("time", jsonObject.getString("time"));
        collectionLogs.insertOne(document);
    }

    public void logLogin(JSONObject jsonObject){
        Document document = new Document();
        document.append("type", "login");
        document.append("login", jsonObject.getString("login"));
        document.append("time", jsonObject.getString("time"));
        collectionLogs.insertOne(document);
    }

    public void updateFName(String name, String firstName) {
        Bson filter = new Document("firstName", name);
        Bson newValue = new Document("firstName", firstName);
        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateOne(filter, updateOperationDocument);
    }

    public void updateLName(String name, String lastName) {
        Bson filter = new Document("lastName", name);
        Bson newValue = new Document("lastName", lastName);
        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateOne(filter, updateOperationDocument);
    }

    public void changePassword(String login, String passHash) {
        Bson filter = new Document("login", login);
        Bson newValue = new Document("password", passHash);
        Bson updateOperationDocument = new Document("$set", newValue);
        collectionUsers.updateOne(filter, updateOperationDocument);
    }

    public void deleteUser(String login){
        BasicDBObject theQuery = new BasicDBObject();
        theQuery.put("login", login);
        collectionUsers.deleteOne(theQuery);

        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("from").equals(login)){
                    theQuery = new BasicDBObject();
                    theQuery.put("from", login);
                    collectionMessages.deleteOne(theQuery);
                }
            }
        }
    }

    public boolean loginUser(String login, String password) {
//
//
//        BasicDBObject loginQuery = new BasicDBObject();
//        loginQuery.put("login", login);
//
//        Bson filter = Filters.eq("login", login);
//        Document myDoc = collectionUsers.find(filter).first();
//
//        assert myDoc != null;
//        String hashed = myDoc.getString("password");
//        User temp = getUser(login);
//
//        if (!findLogin(login) && BCrypt.checkpw(password, hashed) && temp.getLogin().equals(login)) {
//            BasicDBObject token = new BasicDBObject().append("token", generateToken());
//            temp.setToken(token.getString("token"));
//            collectionUsers.updateOne(loginQuery, new BasicDBObject("$set", token));
//            return true;
//        }
        return false;
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

    private String generateToken() {
        int size = 25;
        Random rnd = new Random();
        String generatedString = "";
        for (int i = 0; i < size; i++) {
            int type = rnd.nextInt(4);

            switch (type) {
                case 0:
                    generatedString += (char) ((rnd.nextInt(26)) + 65);
                    break;
                case 1:
                    generatedString += (char) ((rnd.nextInt(10)) + 48);
                    break;
                default:
                    generatedString += (char) ((rnd.nextInt(26)) + 97);
            }
        }
        return generatedString;
    }

    public String getToken(String login) {
        Bson bsonFilter = Filters.eq("login", login);
        Document myDoc = collectionUsers.find(bsonFilter).first();

        if (!findLogin(login)) {
            assert myDoc != null;
            return myDoc.getString("token");
        }
        return null;
    }

//    public boolean checkPassword(String login, String password) {
//        BasicDBObject loginQuery = new BasicDBObject();
//        loginQuery.put("login", login);
//        loginQuery.put("password", password);
//        User temp = getUser(login);
//
//
//        if (!findLogin(login)) {
//
//            return BCrypt.checkpw(password, temp.getPassword());
//        }
//
//        return false;
//    }

    public boolean checkToken(String token) {
        BasicDBObject loginQuery = new BasicDBObject();
        loginQuery.append("token", token);

        long count = collectionUsers.countDocuments(loginQuery);
        if (count > 0) {
            return true;
        }
        return false;
    }

    public String getTime() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("ddMMyy HH:mm:ss");
        LocalDateTime time = LocalDateTime.now();
        return format.format(time);

    }

    public List<String> getMessage(String login, String token) {
        Bson filter = Filters.eq("from", login);
        FindIterable<Document> message = collectionMessages.find(filter);

        BasicDBObject query = new BasicDBObject();
        query.append("login", login);
        query.append("token", token);

        FindIterable<Document> doc = collectionUsers.find(query);
        List<String> temp = new ArrayList<>();
        JSONObject obj = new JSONObject();

        if (!findLogin(login) && checkToken(token)) {
            if (doc.iterator().hasNext()) {
                for (Document p : message) {
                    obj.put("from", p.getString("from"));
                    obj.put("to", p.getString("to"));
                    obj.put("message", p.getString("message"));
                    obj.put("time", p.getString("time"));
                    temp.add(obj.toString());
                }
            }
        }

        return temp;
    }

    public String hashPass(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));

    }

//    public List<String> logList(String login, String token) {
//        BasicDBObject loginQuery = new BasicDBObject();
//        BasicDBObject checkQuery = new BasicDBObject();
//        JSONObject jsonObject = new JSONObject();
//        User user = getUser(login);
//        List<String> temp = new ArrayList<>();
//
//        loginQuery.append("type", login);
//
//        checkQuery.append("login", login);
//        checkQuery.append("token", token);
//
//        Bson bsonFilter = Filters.eq("login", login);
//        Document document = collectionLogs.find(bsonFilter).first();
//        FindIterable<Document> documentLogFinder = collectionLogs.find(bsonFilter);
//        FindIterable<Document> documentUserFinder = collectionUsers.find(checkQuery);
//
//        if (!findLogin(login) && checkToken(token) && user.getLogin().equals(login) && document != null) {
//            if (documentUserFinder.iterator().hasNext()) {
//                for (Document doc : documentLogFinder) {
//                    jsonObject.put("type", doc.getString("type"));
//                    jsonObject.put("login", doc.getString("login"));
//                    jsonObject.put("time", doc.getString("time"));
//                    temp.add(jsonObject.toString());
//                }
//            } else {
//                return null;
//            }
//            return temp;
//        }
//        return null;
//    }

    public List<JSONObject> getUsers() {
        List<JSONObject> list = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")){
                    list.add(object);
                }
            }
        }
        return list;
    }

    public boolean matchLogin(String login, String password) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("login").equals(login) && BCrypt.checkpw(password, object.getString("password"))){
                    return true;
                }
            }
        }
        return false;
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

    public boolean matchToken(String login, String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")) {
                    if (object.getString("login").equals(login) && object.getString("token").equals(token)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean findToken(String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")) {
                    if (object.getString("token").equals(token)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<String> getMessages(String login, String fromLogin) {
        List<String> messages = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("to").equals(login)){
                    messages.add(object.toString());
                }
            }
        }
        return messages;
    }

    public List<String> getMessages(String login) {
        List<String> messages = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionMessages.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("to").equals(login)){
                    messages.add(object.toString());
                }
            }
        }
        return messages;
    }

    public String getLogin(String token) {
        try (MongoCursor<Document> cursor = collectionUsers.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.has("token")) {
                    if (object.getString("token").equals(token)) {
                        return object.getString("login");
                    }
                }
            }
        }
        return null;
    }

    public List<String> getLogs(String login, String type) {
        List<String> userLog = new ArrayList<>();
        try (MongoCursor<Document> cursor = collectionLogs.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                JSONObject object = new JSONObject(doc.toJson());
                if (object.getString("type").equals(type) && object.getString("login").equals(login)){
                    userLog.add(object.toString());
                }
            }
        }
        return userLog;
    }
}
