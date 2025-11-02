package com.ecetasci.hrmanagement.repository;

import com.ecetasci.hrmanagement.entity.User;
import com.ecetasci.hrmanagement.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);

    Optional<User>  findUserByEmail(String email);

    Optional<User> findFirstByRole(Role role);

    Optional<User> findByRole(Role role);

    List<User >findAllByRole(Role role);

    Boolean existsUserByEmail(String email);

    List<User> findAllByNameContaining(String firstname);


    Optional<User> findUserByPasswordResetToken(String passwordResetToken);

    Optional<User> findByEmailVerificationToken(String emailVerificationToken);
}
