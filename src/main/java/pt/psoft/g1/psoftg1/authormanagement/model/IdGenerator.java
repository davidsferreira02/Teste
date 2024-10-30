package pt.psoft.g1.psoftg1.authormanagement.model;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

import java.util.UUID;

@Component
public class IdGenerator {


    @Autowired
    private AuthorRepository authorRepository;
    @Profile("24Hex")
    public String generateUniqueHex24() {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        } while (authorRepository.findByAuthorNumber(uniqueId).isPresent());
        return uniqueId;
    }
}