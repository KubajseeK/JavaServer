package sample;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
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

    public void insertUser(User user) {
        users.append("fname", user.getFname());
        users.append("lname", user.getLname());
        users.append("login", user.getLogin());
        users.append("password", user.getPassword());
        users.append("token", user.getToken());
        collectionUsers.insertOne(users);
    }

    public void insertMessage(String from, String to, String message) {
        messages.append("from", from);
        messages.append("message", to);
        messages.append("to", message);
        messages.append("time", getTime());
        collectionMessages.insertOne(messages);
    }

    public void log(JSONObject jsonObject) {
        logs.append("type", jsonObject.getString("type"));
        logs.append("login", jsonObject.getString("login"));
        logs.append("time", jsonObject.getString("time"));
        collectionLogs.insertOne(logs);
    }

    public void updateUser(String fname, String lname, String login, String token) {
        User temp=getUser(login);
        BasicDBObject updateQuery=new BasicDBObject();
        updateQuery.put("fname",temp.getFname());
        updateQuery.put("lname",temp.getLname());
        updateQuery.put("token",token);

        collectionUsers.updateOne(updateQuery, new BasicDBObject("$set", new BasicDBObject("fname", fname).append("lname", lname)));
    }

    public boolean changePassword(String oldPassword, String newPassword, String login, String token) {
        BasicDBObject loginQuery=new BasicDBObject();
        loginQuery.append("login", login);
        loginQuery.append("password",oldPassword);
        loginQuery.append("token",token);

        User tempUser=getUser(login);

        if (checkToken(token) && !findLogin(login) && tempUser.getLogin().equals(login)){
            collectionUsers.updateOne(loginQuery, new BasicDBObject("$set", new BasicDBObject("password", hashPass(newPassword))));
            mongoClient.close();
            return true;
        }
        return false;
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

    public boolean loginUser(String login, String password) {


        BasicDBObject loginQuery = new BasicDBObject();
        loginQuery.put("login", login);

        Bson filter = Filters.eq("login", login);
        Document myDoc = collectionUsers.find(filter).first();

        assert myDoc != null;
        String hashed = myDoc.getString("password");
        User temp = getUser(login);

        if (!findLogin(login) && BCrypt.checkpw(password, hashed) && temp.getLogin().equals(login)) {
            BasicDBObject token = new BasicDBObject().append("token", generateToken());
            temp.setToken(token.getString("token"));
            collectionUsers.updateOne(loginQuery, new BasicDBObject("$set", token));
            return true;
        }
        return false;
    }

    public boolean logout(String login, String token) {
        BasicDBObject loginQuery = new BasicDBObject();
        loginQuery.put("login", login);
        loginQuery.put("token", token);

        User tempUser = getUser(login);
        if (!findLogin(login) && checkToken(token) && tempUser.getLogin().equals(login)) {
            collectionUsers.updateOne(loginQuery, new BasicDBObject("$unset", new BasicDBObject("token", token)));

            return true;
        }

        return false;
    }

    public boolean findLogin(String login) {
        BasicDBObject loginQuery = new BasicDBObject();
        loginQuery.put("login", login);
        long count = collectionUsers.countDocuments(loginQuery);

        if (count == 0) {

            return true;
        }

        return false;
    }

    public User getUser(String login) {
        Bson bsonFilter = Filters.eq("login", login);
        Document myDoc = collectionUsers.find(bsonFilter).first();


        if (!findLogin(login)) {
            assert myDoc != null;

            return new User(myDoc.getString("fname"), myDoc.getString("lname"),
                    myDoc.getString("login"), myDoc.getString("password"));

        }

        return null;
    }

    private String generateToken() {
        int size=25;
        Random rnd = new Random();
        String generatedString="";
        for(int i = 0;i<size;i++) {
            int type=rnd.nextInt(4);

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

    public boolean checkPassword(String login, String password) {
        BasicDBObject loginQuery = new BasicDBObject();
        loginQuery.put("login", login);
        loginQuery.put("password", password);
        User temp = getUser(login);


        if (!findLogin(login)) {

            return BCrypt.checkpw(password, temp.getPassword());
        }

        return false;
    }

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
        Bson filter=Filters.eq("from", login);
        FindIterable<Document> message=collectionMessages.find(filter);

        BasicDBObject query=new BasicDBObject();
        query.append("login",login);
        query.append("token", token);

        FindIterable<Document> doc=collectionUsers.find(query);
        List<String> temp=new ArrayList<>();
        JSONObject obj=new JSONObject();

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
}
