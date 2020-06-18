package sample;

import com.google.gson.Gson;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
public class UserController {

    List<User> userList = new ArrayList<>();
    List<String> logList = new ArrayList<>();
    List<String> messages = new ArrayList<>();
    Database database = new Database();
    int totalLoginAttempts = 3;

    public UserController() {

    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value = "token") String token) {
        Database database = new Database();

        if (token == null) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"No token found.\"}");
        }

        if (database.checkToken(token)) {
            JSONObject jsonObject = new JSONObject();
            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");
            Date now = new Date();
            String strTime = sdfDate.format(now);
            jsonObject.put("time", strTime);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonObject.toString());
        } else {

            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String data) {
        Gson gson = new Gson();
        User tempUser = gson.fromJson(data, User.class);
        JSONObject res = new JSONObject();

        if (tempUser.getLogin()!=null && tempUser.getPassword()!=null){
            if (!matchLogin(tempUser.getLogin(), tempUser.getPassword())){
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String token = generateToken();
            Database db = new Database();
            db.login(tempUser.getLogin(), token);

            JSONObject user = db.getUser(tempUser.getLogin());
            res.put("fname", user.getString("fname"));
            res.put("lname", user.getString("lname"));
            res.put("login", tempUser.getLogin());
            res.put("token", token);
        }
        logLogin(tempUser);
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @RequestMapping("/user/{login}")
    public ResponseEntity<String> getOneUser(@RequestParam(value = "token") String token, @PathVariable String login) {

        JSONObject jsonObject = new JSONObject();
        User user = getUser(login);

        if (token == null) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }

        if (isTokenValid(token)) {

            if (user == null) {
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid User\"}");
            }

            jsonObject.put("fname", user.getFname());
            jsonObject.put("lname", user.getLname());
            jsonObject.put("login", user.getLogin());

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonObject.toString());
        } else
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\")");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) {
        JSONObject jsonObject = new JSONObject(data);
        Database database = new Database();
        Gson gson = new Gson();
        User tempUser = gson.fromJson(data, User.class);


        if (tempUser.getFname()!=null && tempUser.getLname()!=null && tempUser.getLogin()!=null && tempUser.getPassword()!=null) {
            if (findLogin(tempUser.getLogin())) {
                JSONObject response = new JSONObject();
                response.put("error", "User already exists");
                return ResponseEntity.status(400).body(response.toString());
            }

            if (tempUser.getPassword().isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("error", "Password field can not be empty");
                return ResponseEntity.status(400).body(response.toString());
            }
            String hashPass = hash(jsonObject.getString("password"));
            //User user = new User(jsonObject.getString("fname"), jsonObject.getString("lname"), jsonObject.getString("login"), hashPass);

            JSONObject dbUser = new JSONObject();
            dbUser.put("fname", jsonObject.getString("fname"));
            dbUser.put("lname", jsonObject.getString("lname"));
            dbUser.put("login", jsonObject.getString("login"));
            dbUser.put("password", hashPass);
            database.insertUser(dbUser);

            JSONObject response = new JSONObject();
            response.put("fname", jsonObject.getString("fname"));
            response.put("lname", jsonObject.getString("lname"));
            response.put("login", jsonObject.getString("login"));
            return ResponseEntity.status(201).body(response.toString());

        } else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        Gson gson = new Gson();
        User user = gson.fromJson(data, User.class);

        if (findToken(token)){
            Database db = new Database();
            db.logout(user.getLogin(), token);

            logLogout(user);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("Success.");
        }

        JSONObject res = new JSONObject();
        res.put("error","Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private boolean findLogin(String login) {
        Database db = new Database();
        return (db.findLogin(login));
    }

    private boolean checkPassword(String login, String password) {
        User user = getUser(login);
        if (user != null) {
            return BCrypt.checkpw(password, user.getPassword());
        }
        return false;
    }

    private User getUser(String login) {
        for (User user : userList) {
            if (user.getLogin().equals(login))
                return user;
        }
        return null;
    }

    private boolean compareToken(String login, String token) {
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getToken().equals(token)) {
                return true;
            }
        }
        return false;
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

    public boolean isTokenValid(String token) {
        for (User user : userList)
            if (user.getToken() != null && user.getToken().equals(token))
                return true;

        return false;
    }

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private boolean matchLogin(String login, String password) {
        Database db = new Database();
        return db.matchLogin(login, password);
    }

    private void logLogout(User user) {
        org.json.JSONObject log = new org.json.JSONObject();
        log.put("type", "logout");
        log.put("login", user.getLogin());
        log.put("time", getTime());
        logList.add(log.toString());

        Database db = new Database();
        db.logLogout(log);
    }

    private void logLogin(User tempUser) {
        JSONObject log = new org.json.JSONObject();
        log.put("type", "login");
        log.put("login", tempUser.getLogin());
        log.put("time", getTime());
        logList.add(log.toString());

        Database db = new Database();
        db.logLogin(log);
    }

    private String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyy HH:mm:ss");
        LocalDateTime localTime = LocalDateTime.now();
        return dtf.format(localTime);

    }

    @PostMapping(value = "/log")
    public ResponseEntity<String> getLogList(@RequestHeader(name = "Authorization") String token, @RequestBody String data){
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();
        List<String> userLog;
        if (!findToken(token)){
            res.put("error", "invalid token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        Database db = new Database();
        String login = db.getLogin(token);

        userLog = db.getLogs(login, jsonObject.getString("type"));
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(userLog.toString());
    }


    @RequestMapping(method = RequestMethod.POST, value = "/changepassword")
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject user = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        if (user.getString("login") != null && user.getString("oldpassword") != null && user.getString("newpassword") != null){
            if (!matchToken(user.getString("login"), token)){
                res.put("error", "no login with such token");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (!matchLogin(user.getString("login"), user.getString("oldpassword"))){
                res.put("error", "wrong password or login");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String passHash = hash(user.getString("newpassword"));

            Database db = new Database();
            db.changePassword(user.getString("login"), passHash);
            res.put(user.getString("login"), "password changed");
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        res.put("error", "missing body attributes");
        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private boolean matchToken(String login, String token){
        Database db = new Database();
        return db.matchToken(login, token);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/message/new")
    public ResponseEntity<String> sendMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject res = new JSONObject();

        String login = jsonObject.getString("from");

        if (login == null  || !findToken(token) ) {
            res.put("error", "invalid token or login");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        if (matchToken(jsonObject.getString("from"), token) && findLogin(jsonObject.getString("from")) && findLogin(jsonObject.getString("to")) && jsonObject.has("message")) {
            res.put("from", jsonObject.getString("from"));
            res.put("message", jsonObject.getString("message"));
            res.put("to", jsonObject.getString("to"));
            messages.add(res.toString());

            org.json.JSONObject dbMessage = new org.json.JSONObject();
            dbMessage.put("from", jsonObject.getString("from"));
            dbMessage.put("message", jsonObject.getString("message"));
            dbMessage.put("to", jsonObject.getString("to"));

            Database db = new Database();
            db.insertMessage(dbMessage);

            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        } else {
            res.put("error", "wrong input data");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    private boolean findToken(String token) {
        Database db = new Database();
        return (db.findToken(token));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/messages")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        String login;
        String fromLogin = null;

        if (jsonObject.has("login")){
            if (findLogin(jsonObject.getString("login")) && findToken(token)) {
                login = jsonObject.getString("login");
            }else {
                res.put("error", "login or token not found");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if (jsonObject.has("from")){
                fromLogin = jsonObject.getString("from");
            }
        }else {
            res.put("error", "invalid input data");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        List<String> messages;
        Database db = new Database();

        if (fromLogin != null && matchToken(login, token)){
            messages = db.getMessages(login, fromLogin);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messages.toString());
        }else if (fromLogin == null){
            messages = db.getMessages(login);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(messages.toString());
        }else {
            res.put("error", "not authorised");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{login}")
    public ResponseEntity<String> deleteUser(@RequestHeader(name = "Authorization") String token, @PathVariable String login) {
        JSONObject res = new JSONObject();
        if (matchToken(login, token)){
            org.json.JSONObject jsonObject;
            for (int i=0; i<messages.size(); i++){
                jsonObject = new org.json.JSONObject(messages.get(i));
                if (jsonObject.getString("from").equals(login)){
                    messages.remove(messages.get(i));
                }
            }
            for (int i=0; i<userList.size();i++){
                if (userList.get(i).getLogin().equals(login)){
                    userList.remove(userList.get(i));
                }
            }

            Database db = new Database();
            db.deleteUser(login);

            res.put("status", "user removed");
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }else {
            res.put("error", "wrong login or token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping(method = RequestMethod.PATCH, value = "/update/{login}")
    public ResponseEntity<String> updateLogin(@RequestBody String data, @RequestHeader String token, @PathVariable String login) {
        org.json.JSONObject jsonObject = new org.json.JSONObject(data);
        JSONObject res = new JSONObject();

        if (!matchToken(login, token)){
            res.put("error", "wrong token or login");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }

        if (jsonObject.has("firstName")) {
            String name = "";
            for (User user : userList) {
                if (user.getLogin().equals(login))
                    name = user.getFname();
                user.setFname(jsonObject.getString("firstName"));
            }
            Database db = new Database();
            db.updateFName(name, jsonObject.getString("firstName"));
        }
        if (jsonObject.has("lastName")) {
            String name = "";
            for (User user : userList) {
                if (user.getLogin().equals(login))
                    name = user.getLname();
                user.setLname(jsonObject.getString("lastName"));
            }
            Database db = new Database();
            db.updateLName(name, jsonObject.getString("lastName"));
        }

        res.put("status", "data changed");
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    @GetMapping(value = "/users")
    public ResponseEntity<String> getUsers(@RequestHeader(name = "token") String token){
        List<org.json.JSONObject> list;
        JSONObject res = new JSONObject();

        if (findToken(token)){
            Database db = new Database();
            list=db.getUsers();
            for (int i=0; i<list.size(); i++){
                res.put(String.valueOf(i), list.get(i));
            }
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        res.put("error", "no users logged in");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }


}
