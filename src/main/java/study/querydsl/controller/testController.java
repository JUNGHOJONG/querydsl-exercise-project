package study.querydsl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testController {

    @GetMapping("/hello")
    public String hello() {
        System.out.println("왜 안되는것이냐!!!!!!!!");
        return "hello world";
    }
}
