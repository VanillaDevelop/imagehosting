package gg.nya.imagehosting.beans;

import org.springframework.stereotype.Component;

@Component("greetingBean")
public class GreetingBean {

    private String name;
    private String greeting;

    public void generateGreeting() {
        this.greeting = "Hello, " + (name != null ? name : "Guest") + "!";
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}