package com.example.supportportal1.service;


import com.example.supportportal1.domain.User;
import com.example.supportportal1.exception.domain.EmailExistException;
import com.example.supportportal1.exception.domain.UserNotFoundException;
import com.example.supportportal1.exception.domain.UsernameExistException;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException;

    List<User> getUsers();

    User findUserByUsername(String username);

    User findUserByEmail(String email);
    
    User addNewUser(String firstName,String lastName,String username,String email,String role,boolean isNonLocked,boolean isActive,MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException, IOException;
    User updateUser(String currentUsername,String newFirstName,String newLastName,String newUsername,String newEmail,String role,boolean isNonLocked,boolean isActive,MultipartFile profileImage) throws Exception, Exception, Exception;
    void deleteUser(long id);
    void resetPassword(String email) throws Exception;
    User updateProfileImage(String username,MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, Exception;
    
}

