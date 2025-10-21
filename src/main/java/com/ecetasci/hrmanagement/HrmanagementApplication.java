package com.ecetasci.hrmanagement;

import com.ecetasci.hrmanagement.security.JwtAuhenticationFilter;
import com.ecetasci.hrmanagement.utility.JwtManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HrmanagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmanagementApplication.class, args);

    }

}
