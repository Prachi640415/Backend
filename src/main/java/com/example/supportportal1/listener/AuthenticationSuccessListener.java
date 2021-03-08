package com.example.supportportal1.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.example.supportportal1.domain.User;
import com.example.supportportal1.services.LoginAttemptService;

@Component
public class AuthenticationSuccessListener {
	private LoginAttemptService loginAttemptService;
	
	@Autowired
	public AuthenticationSuccessListener(LoginAttemptService loginAttemptService) {
		this.loginAttemptService=loginAttemptService;
	}
	
	@EventListener
	public  void onAuthenticationSucess(AuthenticationSuccessEvent event) {
		Object principal=event.getAuthentication().getPrincipal();
		if(principal instanceof User) {
			User user=(User)event.getAuthentication().getPrincipal();
			loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
		}
	}
	

}
