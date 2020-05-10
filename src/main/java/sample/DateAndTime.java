package sample;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
public class DateAndTime {
    List<User> userList = new ArrayList<>();
    List<String> logList = new ArrayList<>();
    List<String> messagesList = new ArrayList<>();

    public DateAndTime() {
        userList.add(new User("Roman", "Simko", "roman", "heslo"));
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value = "token") String token) {

        if (token == null) {
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"No token found.\"}");
        }

        if (isTokenValid(token)) {
            JSONObject jsonObject = new JSONObject();
            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");
            Date now = new Date();
            String strTime = sdfDate.format(now);
            jsonObject.put("time",strTime);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(jsonObject.toString());
        } else {

            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
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
        SimpleDateFormat sdfDate = new SimpleDateFormat("HH");
        Date now = new Date();
        String strHour = sdfDate.format(now);
        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{\"hour\":"+strHour+"}");
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

                response.put("fname", templateUser.getFname());
                response.put("lname", templateUser.getLname());
                response.put("login", templateUser.getLogin());
                String token = generateToken();
                response.put("token", token);
                templateUser.setToken(token);
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());

            } else {
                response.put("error", "Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }

        } else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping("/users/{login}")
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

        if (jsonObject.has("fname") && jsonObject.has("lname") && jsonObject.has("login") && jsonObject.has("password")) {
            if (findLogin(jsonObject.getString("login"))) {
                JSONObject response = new JSONObject();
                response.put("error", "User already exists");
                return ResponseEntity.status(400).body(response.toString());
            }
            String password = jsonObject.getString("password");
            if (password.isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("error", "Password field can not be empty");
                return ResponseEntity.status(400).body(response.toString());
            }
            String hashPass = hash(jsonObject.getString("password"));
            User user = new User(jsonObject.getString("fname"), jsonObject.getString("lname"), jsonObject.getString("login"), hashPass);
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

        if (user != null && isTokenValid(token)) {
            user.setToken(null);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }
        JSONObject repsonse = new JSONObject();
        repsonse.put("error", "Incorrect login or token");
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

    private boolean compareToken(String login, String token) {
        for (User user : userList) {
            if (user.getLogin().equals(login) && user.getToken().equals(token)) {
                return true;
            }
        }
        return false;
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

    public boolean isTokenValid(String token) {
        for(User useri : userList)
            if(useri.getToken()!=null && useri.getToken().equals(token))
                return true;

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

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/log?type={logType}")
    public ResponseEntity<String> getLogList(@RequestHeader(name = "Authorization") String token, @PathVariable String logType) {
        JSONObject response = new JSONObject();
        String login = "";
        JSONObject logObj = null;
        List<String> userLog = new ArrayList<>();


        if (!findToken(token)) {
            response.put("error", "invalid token");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        for (User user : userList) {
            if (user.getToken().equals(token) && user.getToken() != null) {
                login = user.getLogin();
            }
        }
        for (String log : logList) {
            logObj = new JSONObject(log);
            if (logObj.getString("login").equals(login) && logObj.getString("type").equals("logType")) {
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
    public ResponseEntity<String> changePassword(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject user = new JSONObject(data);
        JSONObject response = new JSONObject();

        if (user.getString("login") != null && user.getString("oldpassword") != null && user.getString("newpassword") != null) {
            if (!compareToken(user.getString("login"), token)) {
                response.put("error", "Invalid Token");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
            if (!findLogin(user.getString("login")) || !checkPassword(user.getString("login"), user.getString("oldpassword"))) {
                response.put("error", "Incorrect login or Password");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }

            String hashPass = hash(user.getString("newpassword"));
            for (User users : userList) {
                if (users.getLogin().equals(user.getString("login"))) {
                    users.setPassword(hashPass);
                    response.put(users.getLogin(), "password changed");
                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(response.toString());
                }
            }
        }
        response.put("error", "Invalid body attributes");
        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/message/new")
    public ResponseEntity<String> sendMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token) {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject response = new JSONObject();
        User user = getUser(jsonObject.getString("from"));

        if (user == null || !findToken(token)) {
            response.put("error", "No such login or token");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }

        if (jsonObject.has("from") && jsonObject.has("message") && jsonObject.has("to")) {
            if (findLogin(jsonObject.getString("from")) && findLogin(jsonObject.getString("to"))) {
                response.put("from", jsonObject.getString("from"));
                response.put("message", jsonObject.getString("message"));
                response.put("to", jsonObject.getString("to"));

                messagesList.add(response.toString());
                return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            } else {
                response.put("error", "No such entry in database");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
            }
        } else {
            response.put("error", "Empty message or login");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/messages?from={fromLogin}")
    public ResponseEntity<String> getMessages(@RequestBody String data, @RequestHeader(name = "Authorization") String token, @PathVariable String fromLogin) {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject response = new JSONObject();

        User user = getUser(jsonObject.getString("login"));

        if (user == null || !findToken(token)) {
            response.put("error", "No such login or token");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        JSONObject message;
        if (jsonObject.has("login") && findLogin(jsonObject.getString("login"))) {
            response.put("from", jsonObject.getString("login"));
            for (int i = 0; i < messagesList.size(); i++) {
                message = new JSONObject(messagesList.get(i));
                if (message.getString("from").equals(fromLogin) && !fromLogin.equals("")) {
                    response.put("message: " + i, messagesList.get(i));
                }
            }
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());

        } else {
            response.put("error", "Wrong/Empty login.");
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{login}")
    public ResponseEntity<String> deleteUser(@RequestHeader(name = "Authorisation") String token, @PathVariable String login) {
        JSONObject response = new JSONObject();
        JSONObject jsonObject;

        if (compareToken(login, token)) {
            for (int i = 0; i < messagesList.size(); i++) {
                jsonObject = new JSONObject(messagesList.get(i));
                if (jsonObject.getString("from").equals(login)) {
                    messagesList.remove(messagesList.get(i));
                }
            }
            for (int i = 0; i < userList.size(); i++) {
                if (userList.get(i).getLogin().equals(login)) {
                    userList.remove(userList.get(i));
                }
            }
            response.put("success", "User Removed.");
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        } else {
            response.put("error", "Either login or token is wrong.");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
    }

    @RequestMapping(method = RequestMethod.PATCH, value = "/update/{login}")
    public ResponseEntity<String> updateLogin(@RequestBody String data, @RequestHeader String token, @PathVariable String login) {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject response = new JSONObject();

        if (!compareToken(login, token)) {
            response.put("error", "Either login or token is wrong.");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(response.toString());
        }
        if (jsonObject.has("fname") || jsonObject.has("lname") || (jsonObject.has("fname") && jsonObject.has("lname"))) {
            for (User user : userList) {
                if (user.getLogin().equals(login)) {
                    user.setFname(jsonObject.getString("fname"));
                    user.setLname(jsonObject.getString("lname"));
                }
            }
        }
        response.put("success", "Data changed");
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

}
