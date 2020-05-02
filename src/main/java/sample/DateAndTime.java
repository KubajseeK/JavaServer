package sample;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DateAndTime {
    List<User> list =  new ArrayList<User>();

    public DateAndTime() {
        list.add(new User("Roman","Simko","roman","heslo"));
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestBody String data) {
        JSONObject jsonObject = new JSONObject(data);

        if(!findLogin(data)) {
            LocalTime time = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            jsonObject.put("time", formatter);
            return ResponseEntity.status(200).body(jsonObject.toString());
        }
        else {
            jsonObject.put("error", "Invalid token");
            return ResponseEntity.status(400).body(jsonObject.toString());
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
        return ResponseEntity.status(200).body(jsonObj.toString());
    }

    @RequestMapping("/time/hour")
    public ResponseEntity<String> getHour(){
        JSONObject jsonObject = new JSONObject();

        LocalTime time = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH");
        jsonObject.put("hour", formatter);
        return ResponseEntity.status(200).body(jsonObject.toString());
    }

    @RequestMapping("/hello")
    public String getHello(){
        return "Hello. How are you? ";
    }

    @RequestMapping("/hello/{name}")
    public String getHelloWithName(@PathVariable String name){
        return "Hello "+name+". How are you? ";
    }

    @RequestMapping("/hi")
    public String getHi(@RequestParam(value="fname") String fname, @RequestParam(value="age") String age){
        return "Hello. How are you? Your name is "+fname+" and you are "+age;
    }

    @RequestMapping(method=RequestMethod.POST, value="/login")
    public String login(@RequestBody String credential){
        System.out.println(credential);
        return "{\"Error\":\"Login already exists\"}";
    }

    @RequestMapping(method=RequestMethod.POST, value="/signup")
    public ResponseEntity<String> signup(@RequestBody String data){
        JSONObject jsonObject = new JSONObject(data);
        String password = BCrypt.hashpw(jsonObject.getString("password"), BCrypt.gensalt());

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
            list.add(user);

            JSONObject response = new JSONObject();
            response.put("fname", jsonObject.getString("fname"));
            response.put("lname", jsonObject.getString("lname"));
            response.put("login", jsonObject.getString("login"));
            return ResponseEntity.status(201).body(response.toString());

        }
        else {
            JSONObject response = new JSONObject();
            response.put("error", "Invalid body request");
            return ResponseEntity.status(400).body(response.toString());
        }
    }
    @RequestMapping(method = RequestMethod.POST, value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data) {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject response = new JSONObject();
        System.out.println(jsonObject.getString("login"));
        System.out.println(data);
        response.put("message", "Logout successful");
        response.put("login", "yay");
        return ResponseEntity.status(200).body(response.toString());
    }

    private boolean findLogin(String login) {
        for (User user : list) {
            if (user.getLogin().equalsIgnoreCase(login))
                return true;
        }
        return false;
    }

}
