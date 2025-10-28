package com.ecetasci.hrmanagement.seeder;

import com.ecetasci.hrmanagement.entity.*;
import com.ecetasci.hrmanagement.enums.CompanyStatus;
import com.ecetasci.hrmanagement.enums.ExpenseStatus;
import com.ecetasci.hrmanagement.enums.Role;
import com.ecetasci.hrmanagement.enums.UserStatus;
import com.ecetasci.hrmanagement.repository.*;
import com.ecetasci.hrmanagement.service.ExpenseDocumentService;
import com.ecetasci.hrmanagement.utility.JwtManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.beans.Encoder;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpenseRepository expenseRepository;
    private final ExpenseDocumentRepository expenseDocumentRepository;
    private final ExpenseDocumentService expenseDocumentService;
    private final JwtManager jwtManager;


    @Override
    public void run(String... args) {
        seedFirstSiteAdmin();
        seedDevData();
    }

    private void seedFirstSiteAdmin() {
        if (userRepository.findByRole(Role.SITE_ADMIN).isEmpty()) {
            User firstSiteAdmin = User.builder()
                    .name("System Admin")
                    .email("admin@hrmanagement.com")
                    .password(passwordEncoder.encode("123456"))
                    .createdAt(LocalDateTime.now())
                    .emailVerificationToken(jwtManager.generateToken("admin@hrmanagement.com"))
                    .tokenExpiryDate(LocalDateTime.now().plusDays(300))
                    .role(Role.SITE_ADMIN)
                    .isFirstAdmin(true)
                    .userStatus(UserStatus.ACTIVE)
                    .build();

            userRepository.save(firstSiteAdmin);
            System.out.println(" İlk Site Admin oluşturuldu: admin@hrmanagement.com");
        }
    }

    @Profile("dev") // sadece dev ortamında çalışır
    private void seedDevData() {
        if (companyRepository.count() == 0) {
            Company company = Company.builder()
                    .companyName("Test ")
                    .companyEmail("info@company.com")
                    .phoneNumber("+905551112233")
                    .address("Istanbul, Turkey")
                    .taxNumber("12345678")
                    .website("www.company.com")
                    .employeeCount(2)
                    .companyStatus(CompanyStatus.APPROVED)
                    .foundedDate(LocalDate.of(2010, 1, 1))
                    .description("This is a test company for development profile")
                    .isActive(true)
                    .employees(null)
                    .subscriptions(null)
                    .companyReview(null)
                    .build();

            companyRepository.save(company);

            User user = User.builder().name("ece").email("ecetasci.iu@gmail.com").password("1234")
                    .role(Role.COMPANY_ADMIN)
                    .userStatus(UserStatus.ACTIVE)
                    .isFirstAdmin(false)
                    .emailVerificationToken(jwtManager.generateToken(("ecetasci.iu@gmail.com")))
                    .passwordResetToken("1234")
                    .tokenExpiryDate(LocalDateTime.of(2027, 10, 14, 19, 45, 0))
                    .createdAt(LocalDateTime.now()).build();


            userRepository.save(user);
            System.out.println("user oluştu");

            Employee employee1 = Employee.builder()
                    .employeeNumber("EMP001")
                    .name("Ece Taşçı")

                    .email("ali@testcompany.com")
                    .hireDate(LocalDate.of(2020, 5, 10))
                    .birthDate(LocalDate.of(1994, 8, 10))
                    .company(company)
                    .role(Role.EMPLOYEE)
                    .position("hr")
                    .leaveBalance(21)
                    .salary(BigDecimal.valueOf(100000))
                    .password("1234")
                    .build();
            employeeRepository.save(employee1);

            Employee employee2 = Employee.builder()
                    .employeeNumber("EMP002")
                    .name("Ayşe Yılmaz")
                    .email("ayse@testcompany.com")
                    .hireDate(LocalDate.of(2021, 3, 15))
                    .birthDate(LocalDate.of(1999, 8, 10))
                    .company(company)
                    .role(Role.EMPLOYEE)
                    .position("Manager")
                    .leaveBalance(15)
                    .salary(BigDecimal.valueOf(100000))
                    .password(passwordEncoder.encode("1234"))
                    .expenses(new ArrayList<>())
                    .build();

            employeeRepository.save(employee2);

// EXPENSE
            Expense expense = Expense.builder()
                    .description("Taksi")
                    .amount(BigDecimal.valueOf(450))
                    .expenseDate(LocalDate.of(2025, 5, 7))
                    .status(ExpenseStatus.APPROVED)
                    .employee(employee2) // ✅ ilişkiyi buradan kur
                    .build();

            Expense savedExpense = expenseRepository.save(expense);
            employee2.getExpenses().add(savedExpense);


// cascade = ALL olduğu için employee kaydedilince expense de otomatik kaydolur
            employeeRepository.save(employee2);

            String desktopPath = System.getProperty("user.home") + "/Desktop/receipt.pdf";
            File file = new File(desktopPath);
            ExpenseDocument doc = ExpenseDocument.builder()
                    .expense(savedExpense)
                    .fileName(file.getName())
                    .filePath(file.getAbsolutePath()) // 🔹 işte burası file path
                    .fileType("application/pdf")
                    .uploadDate(LocalDate.now())
                    .build();

            expenseDocumentRepository.save(doc);

            System.out.println("Expense ve belge kaydedildi: " + doc.getFilePath());
        }

        System.out.println(" Development test verileri eklendi.");
    }


    // public String token(String username) {
    //   String token = jwtManager.generateToken(username);
    // System.out.println(token);
    //return token;
    //}
}



