package gg.nya.imagehosting.beans;

import org.springframework.stereotype.Component;

import gg.nya.imagehosting.security.UserSession;

@Component("greetingBean")
public class GreetingBean {

    private String name;
    private String greeting;
    private final UserSession userSession;

    public GreetingBean(UserSession userSession) {
        this.userSession = userSession;
    }

    public void generateGreeting() {
        this.greeting = "Hello, " + (name != null ? name : "Guest") + "!";
    }

    public String login() {
        userSession.login("1", "test");
        return "logintest.xhtml?faces-redirect=true";
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