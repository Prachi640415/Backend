package com.example.supportportal1.service.impl;


import com.example.supportportal1.domain.User;
import com.example.supportportal1.domain.UserPrincipal;
import com.example.supportportal1.enumeration.Role;
import com.example.supportportal1.exception.domain.EmailExistException;
import com.example.supportportal1.exception.domain.EmailNotFoundException;
import com.example.supportportal1.exception.domain.UserNotFoundException;
import com.example.supportportal1.exception.domain.UsernameExistException;
import com.example.supportportal1.repositories.UserRepository;
import com.example.supportportal1.service.EmailService;
import com.example.supportportal1.service.UserService;
import com.example.supportportal1.services.LoginAttemptService;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.supportportal1.constant.FileConstant;

import javax.mail.MessagingException;
import javax.transaction.Transactional;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.file.Paths;
import java.util.Date;
import java.util.List;


import static com.example.supportportal1.constant.UserImplConstant.*;
import static com.example.supportportal1.enumeration.Role.*;
import static com.example.supportportal1.constant.UserImplConstant.DEFAULT_USER_IMAGE_PATH;
import static com.example.supportportal1.constant.UserImplConstant.EMAIL_ALREADY_EXISTS;
import static com.example.supportportal1.constant.UserImplConstant.FOUND_USER_BY_USERNAME;
import static com.example.supportportal1.constant.UserImplConstant.NO_USER_FOUND_BY_USERNAME;
import static com.example.supportportal1.constant.UserImplConstant.USERNAME_ALREADY_EXISTS;
import static com.example.supportportal1.enumeration.Role.ROLE_USER;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {
    private static final String USER_FOLDER = System.getenv("user.home")+"/supportportal/user/";
	private static final String DIRECTORY_CREATED = "Created Directory for:";
	public static final String JPG_EXTENSION="jpg";
	public static final String FILE_SAVED_IN_FILE_SYSTEM="Saved file in file system by name:";
	public static final String DOT=".";
	public static final String USER_IMAGE_PATH="/user/image";
	public static final String FORWARD_SLASH="/";
	private static final CopyOption REPLACE_EXISTING = null;
	private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private EmailService emailService;
    

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,LoginAttemptService loginAttemptService,EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService=loginAttemptService;
        this.emailService=emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        } else {
        	validateLoginAttempt(user);
            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());
            userRepository.save(user);
            UserPrincipal userPrincipal = new UserPrincipal(user);
            LOGGER.info(FOUND_USER_BY_USERNAME + username);
            return userPrincipal;
        }
    }
    
    private void validateLoginAttempt(User user) {
    	if(user.isNotLocked()) {
    		if(loginAttemptService.hasExceededMaxAttempts(user.getUsername())) {
    			user.setNotLocked(false);
    		}else {
    			user.setNotLocked(true);
    		}
    	}else {
    		loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
    	}
    	
    }

    @Override
    public User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, UsernameExistException, EmailExistException{
        validateNewUsernameAndEmail(EMPTY, username, email);
        User user = new User();
        user.setUserId(generateUserId());
        String password = generatePassword();
        String encodedPassword = encodePassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodedPassword);
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(ROLE_USER.name());
        user.setAuthorities(ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        userRepository.save(user);
        LOGGER.info("New user password: " + password);
        try {
        emailService.sendNewPasswordEmail(firstName, password, email);
        
        }catch (Exception e) {
			
		}
        return user;
    }


    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    private String getTemporaryProfileImageUrl(String username) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH + username).toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    private User validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {
        User userByNewUsername = findUserByUsername(newUsername);
        User userByNewEmail = findUserByEmail(newEmail);
        if(StringUtils.isNotBlank(currentUsername)) {
            User currentUser = findUserByUsername(currentUsername);
            if(currentUser == null) {
                throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
            }
            if(userByNewUsername != null && !currentUser.getId().equals(userByNewUsername.getId())) {
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }
            if(userByNewEmail != null && !currentUser.getId().equals(userByNewEmail.getId())) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return currentUser;
        } else {
            if(userByNewUsername != null) {
                throw new UsernameExistException(USERNAME_ALREADY_EXISTS);
            }
            if(userByNewEmail != null) {
                throw new EmailExistException(EMAIL_ALREADY_EXISTS);
            }
            return null;
        }
    }

	@Override
	public User addNewUser(String firstName, String lastName, String username, String email, String role,
			boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, UsernameExistException, EmailExistException,IOException {
	validateNewUsernameAndEmail(EMPTY, username, email);
	User user=new User();
	 String password = generatePassword();
     user.setUserId(generateUserId());
     user.setFirstName(firstName);
     user.setLastName(lastName);
     user.setJoinDate(new Date());
     user.setUsername(username);
     user.setEmail(email);
     user.setPassword(encodePassword(password));
     user.setActive(isActive);
     user.setNotLocked(isNonLocked);
     user.setRole(getRoleEnumName(role).name());
     user.setAuthorities(getRoleEnumName(role).getAuthorities());
     user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
     userRepository.save(user);
     saveProfileImage(user,profileImage);
	
		return user;
	}

	

	

	private void saveProfileImage(User user, MultipartFile profileImage ) throws IOException {
		// TODO Auto-generated method stub
		if(profileImage!=null) {
		Path userFolder=Paths.get(USER_FOLDER +user.getUsername()).toAbsolutePath().normalize();
		if(Files.exists(userFolder)) {
			Files.createDirectories(userFolder);
			LOGGER.info(DIRECTORY_CREATED + userFolder);
		}
		Files.deleteIfExists(Paths.get(userFolder + user.getUsername()+DOT+JPG_EXTENSION));
		Files.copy(profileImage.getInputStream(), userFolder.resolve(user.getUsername()+DOT+JPG_EXTENSION),REPLACE_EXISTING);
		user.setProfileImageUrl(setProfileImageUrl(user.getUsername()));
		userRepository.save(user);
		LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM + profileImage.getOriginalFilename());
		}
		
	}

	private String setProfileImageUrl(String username) {
		 return ServletUriComponentsBuilder.fromCurrentContextPath().path(USER_IMAGE_PATH + username+ FORWARD_SLASH+username+ DOT + JPG_EXTENSION).toUriString();
	}

	private Role getRoleEnumName(String role) {
		// TODO Auto-generated method stub
		return Role.valueOf(role.toUpperCase());
	}

	@Override
	public User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername,
			String newEmail, String role, boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws Exception, Exception, Exception {
		User currentuser=validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
		
		
		currentuser.setFirstName(newFirstName);
		currentuser.setLastName(newLastName);
		currentuser.setJoinDate(new Date());
	     currentuser.setUsername(newUsername);
	     currentuser.setEmail(newEmail);
	     currentuser.setActive(isActive);
	     currentuser.setNotLocked(isNonLocked);
	     currentuser.setRole(getRoleEnumName(role).name());
	     currentuser.setAuthorities(getRoleEnumName(role).getAuthorities());
	     userRepository.save(currentuser);
	     saveProfileImage(currentuser,profileImage);
		
			return currentuser;
		}

	



	@Override
	public void deleteUser(long id) {
		userRepository.deleteById(id);
		
	}

	@Override
	public void resetPassword(String email) throws MessagingException,EmailNotFoundException {
		User user=userRepository.findUserByEmail(email);
		if(user==null) {
			throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
			
		}
		String password=generatePassword();
		user.setPassword(encodePassword(password));
		userRepository.save(user);
		emailService.sendNewPasswordEmail(user.getFirstName(), password, user.getEmail());
		
	}

	@Override
	public User updateProfileImage(String username, MultipartFile profileImage)throws UserNotFoundException,UsernameExistException,Exception{
		User user=validateNewUsernameAndEmail(username, null, null);
		saveProfileImage(user, profileImage);
		
		return user;
	}
}

	