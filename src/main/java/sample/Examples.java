package sample;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class Examples {

    public Examples() {
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

}
