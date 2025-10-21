package com.ecetasci.hrmanagement.config;

import com.ecetasci.hrmanagement.exceptions.MailSendException;
import com.ecetasci.hrmanagement.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailTestRunner implements CommandLineRunner {

    private final EmailService emailService;

    @Override
    public void run(String... args) throws MailSendException {
       // emailService.send("kaan.aydemir.iu@gmail.com", "Test maili başarıyla gönderildi!");
        System.out.println("Test maili gönderildi, inbox'u kontrol et 🎉");
    }
}
