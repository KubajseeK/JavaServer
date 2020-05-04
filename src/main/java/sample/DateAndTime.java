package sample;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DateAndTime {
    List<User> userList = new ArrayList<>();
    List<String> logList = new ArrayList<>();

    public DateAndTime() {
        userList.add(new User("Roman", "Simko", "roman", "heslo"));
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value = "token") String token) {
        JSONObject jsonObject = new JSONObject();

        if (token == null) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"No token found.\"}");
        }

        if (findToken(token)) {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            jsonObject.put("time", formatter);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonObject.toString());
        } else {
            jsonObject.put("error", "Invalid token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(jsonObject.toString());
        }
    }

    @RequestMapping("/primenumber/{number}")
    public ResponseEntity<String> checkPrimeNumber(@PathVariable int number) {
        JSONObject jsonObj = new JSONObject();
        boolean flag = false;
        jsonObj.put("number", number);

        for (int i = 2; i < number / 2; ++i) {
            if (number % i == 0) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            System.out.println(number + " is a prime number.");
            jsonObj.put("primenumber", true);
        } else {
            System.out.println(number + " is not a prime number.");
            jsonObj.put("primenumber", false);
        }
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonObj.toString());
    }

    @RequestMapping("/time/hour")
    public ResponseEntity<String> getHour() {
        JSONObject jsonObject = new JSONObject();

        LocalTime time = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH");
        jsonObject.put("hour", formatter);
        return ResponseEntity.status(200).body(jsonObject.toString());
    }

    @RequestMapping("/hello")
    public String getHello() {
        return "Hello. How are you? ";
    }

    @RequestMapping("/hello/{name}")
    public String getHelloWithName(@PathVariable String name) {
        return "Hello " + name + ". How are you? ";
    }

    @RequestMapping("/hi")
    public String getHi(@RequestParam(value = "fname") String fname, @RequestParam(value = "age") String age) {
        return "Hello. How are you? Your name is " + fname + " and you are " + age;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/login")
    public ResponseEntity<String> login(@RequestBody String credential) {
        JSONObject jsonObject = new JSONObject(credential);

        if (jsonObject.has("login") && jsonObject.has("password")) {
            JSONObject response = new JSONObject();

            if (jsonObject.getString("password").isEmpty() || jsonObject.getString("login").isEmpty()) {
                response.put("error", "One of the mandatory fields was empty");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }

            if (findLogin(jsonObject.getString("login")) && checkPassword(jsonObject.getString("login"), jsonObject.getString("password"))) {
                User templateUser = getUser(jsonObject.getString("login"));
                if (templateUser == null) {
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{}");
                }

                String token = Long.toString(Math.abs(new SecureRandom().nextLong()), 16);
                response.put("fname", templateUser.getFname());
                response.put("lname", templateUser.getLname());
                response.put("login", templateUser.getLogin());
                response.put("token", token);
                templateUser.setToken(token);
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());

            } else {
                response.put("error", "Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }

        } else {
            JSONObject res = new JSONObject();
            res.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping("/users/{login}")
    public ResponseEntity<String> getOneUser(@RequestParam(value="token") String token, @PathVariable String login) {

        JSONObject jsonObject = new JSONObject();
        User user = getUser(login);

        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }

        if(findToken(token)){

            if(user==null){
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid User\"}");
            }

            jsonObject.put("fname",user.getFname());
            jsonObject.put("lname",user.getLname());
            jsonObject.put("login",user.getLogin());

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonObject.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\")");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/signup")
    public ResponseEntity<String> signup(@RequestBody String data) {
        JSONObject jsonObject = new JSONObject(data);
        String password = BCrypt.hashpw(jsonObject.getString("password"), BCrypt.gensalt(6));

        if (jsonObject.has("fname") && jsonObject.has("lname") && jsonObject.has("login") && jsonObject.has("password")) {
            if (findLogin(jsonObject.getString("login"))) {
                JSONObject response = new JSONObject();
                response.put("error", "User already exists");
                return ResponseEntity.status(400).body(response.toString());
            }
            if (password.isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("error", "Password field can not be empty");
                return ResponseEntity.status(400).body(response.toString());
            }
            User user = new User(jsonObject.getString("fname"), jsonObject.getString("lname"), jsonObject.getString("login"), jsonObject.getString("password"));
            userList.add(user);

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

        JSONObject jsonObject = new JSONObject(data);
        String login = jsonObject.getString("login");
        User user = getUser(login);

        if(user!=null && findToken(token)){
            user.setToken(null);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }
        JSONObject repsonse = new JSONObject();
        repsonse.put("error","Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(repsonse.toString());
    }

    private boolean findLogin(String login) {
        for (User user : userList) {
            if (user.getLogin().equalsIgnoreCase(login))
                return true;
        }
        return false;
    }

    private boolean findToken(String token) {
        for (User user : userList) {
            if (user.getToken().equals(token) && user.getToken() != null) {
                return true;
            }
        }
        return false;
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

    private boolean compareToken(String login, String token){
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getToken().equals(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareLogin(String login, String password) {
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getPassword().equals(password)) {
                return false;
            }
        }
        return true;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/log")
    public ResponseEntity<String> getLogList(@RequestHeader(name = "Authorization") String token){
        JSONObject response = new JSONObject();
        String login = "";
        JSONObject logObj = null;

        List<String> userLog = new ArrayList<>();
        if (!findToken(token)){
            response.put("error", "invalid token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }


        for(User user : userList){
            if(user.getToken().equals(token) && user.getToken()!=null){
                login = user.getLogin();
            }
        }


        for (String log:logList) {
            logObj = new JSONObject(log);
            if (logObj.getString("login").equals(login)){
                userLog.add(log);
            }
        }
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(logObj.toString());
    }

    private void logLogin(User user) {
        JSONObject log = new JSONObject();
        log.put("type", "login");
        log.put("login", user.getLogin());
        log.put("datetime", getTime(user.getToken()));
        logList.add(log.toString());
    }

    private void logLogout(User user) {
        JSONObject log = new JSONObject();
        log.put("type", "logout");
        log.put("login", user.getLogin());
        log.put("datetime", getTime(user.getToken()));
        logList.add(log.toString());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/changepassword")
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        JSONObject user = new JSONObject(data);
        JSONObject response = new JSONObject();

        if (user.getString("login") != null && user.getString("oldpassword") != null && user.getString("newpassword") != null){
            if (!compareToken(user.getString("login"), token)){
                response.put("error", "Invalid Token");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            if (compareLogin(user.getString("login"), user.getString("oldpassword"))){
                response.put("error", "Incorrect login or Password");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }

            for (User users : userList) {
                if (users.getLogin().equals(user.getString("login"))) {
                    users.setPassword(user.getString("newpassword"));
                    response.put(users.getLogin(), "password changed");
                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
                }
            }
        }
        response.put("error", "Invalid body attributes");
        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

}
