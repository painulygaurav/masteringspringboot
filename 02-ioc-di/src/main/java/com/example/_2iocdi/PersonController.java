package com.example._2iocdi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class PersonController {

    private final Map<String, Person> persons;

    public PersonController() {
        this.persons = new HashMap<>();
        this.persons.put("1", new Person("Gaurav", "Painuly"));
        this.persons.put("2", new Person("Shekhar", "Painuly"));
    }

    @GetMapping("/person/all")
    public List<Person> getAll(){
        return persons.values().stream().collect(Collectors.toList());
    }
}
