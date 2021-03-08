package com.example.supportportal1.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.supportportal1.domain.User;
public interface UserRepository extends JpaRepository<User, Long> {

    User findUserByUsername(String username);

    User findUserByEmail(String email);
}
