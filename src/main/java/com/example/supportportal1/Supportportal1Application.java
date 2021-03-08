package com.example.supportportal1;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.supportportal1.constant.FileConstant;


@SpringBootApplication
public class Supportportal1Application {

	

	private static final String USER_FOLDER = System.getenv("user.home")+"/supportportal/user/";;

	public static void main(String[] args) {
		SpringApplication.run(Supportportal1Application.class, args);
		new File(USER_FOLDER).mkdirs();
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
