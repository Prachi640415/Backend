package com.example.supportportal1.resource;

import static com.example.supportportal1.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.OK;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.example.supportportal1.constant.FileConstant;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.supportportal1.domain.HttpResponse;
import com.example.supportportal1.domain.User;
import com.example.supportportal1.domain.UserPrincipal;
import com.example.supportportal1.exception1.ExceptionHandling;
import com.example.supportportal1.exception.domain.EmailExistException;
import com.example.supportportal1.exception.domain.EmailNotFoundException;
import com.example.supportportal1.exception.domain.UserNotFoundException;
import com.example.supportportal1.exception.domain.UsernameExistException;
import com.example.supportportal1.service.UserService;
import com.example.supportportal1.utility.JWTTokenProvider;

@RestController
@CrossOrigin
@RequestMapping(path = { "/", "/user"})
public class UserResource extends ExceptionHandling {
    private static final String EMAIL_SENT = "email sent";
	private static final String USER_DELETED_SUCCESFULLY = "yes";
	private static final HttpStatus NO_CONTENT = null;
	public static final String USER_FOLDER=System.getenv("user.home")+"/supportportal/user/";
	public static final String FORWARD_SLASH="/";
	public static final String TEMP_PROFILE_IMAGE_BASE_URL="http://robohash.org/";

	private AuthenticationManager authenticationManager;
    private UserService userService;
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(AuthenticationManager authenticationManager, UserService userService, JWTTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        authenticate(user.getUsername(), user.getPassword());
        User loginUser = userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal = new UserPrincipal(loginUser);
        HttpHeaders jwtHeader = getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser, jwtHeader, OK);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) throws UserNotFoundException, UsernameExistException, EmailExistException {
        User newUser = userService.register(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail());
        return new ResponseEntity<>(newUser, OK);
    }
    
    @PostMapping("/add")
    public  ResponseEntity<User> addNewUser(@RequestParam("firstName")String firstName,
    		@RequestParam("lastName")String lastName,
    		@RequestParam("username")String username,
    		@RequestParam("email")String  email,
    		@RequestParam("role")String role,
    		@RequestParam("isActive")String isActive,
    		@RequestParam("isNonLocked")String isNonLocked,
    		@RequestParam(value="profileImage",required=false)MultipartFile profileImage) throws UserNotFoundException,UsernameExistException,IOException,EmailExistException
    {
              	User newUser=userService.addNewUser(firstName, lastName, username, email, role,Boolean.parseBoolean(isActive),Boolean.parseBoolean(isNonLocked) , profileImage);
               return new ResponseEntity<>(newUser,OK);
    
    }
    
    
    @PostMapping("/update")
    public  ResponseEntity<User> update(@RequestParam("currentUsername")String currentUsername,
    		@RequestParam("firstName")String firstName,
    		@RequestParam("lastName")String lastName,
    		@RequestParam("username")String username,
    		@RequestParam("email")String  email,
    		@RequestParam("role")String role,
    		@RequestParam("isActive")String isActive,
    		@RequestParam("isNonLocked")String isNonLocked,
    		@RequestParam(value="profileImage",required=false)MultipartFile profileImage) throws Exception
    {
              	User updatedUser=userService.updateUser(currentUsername,firstName, lastName, username, email, role,Boolean.parseBoolean(isActive),Boolean.parseBoolean(isNonLocked) , profileImage);
               return new ResponseEntity<>(updatedUser,OK);
     
    }
    
    
    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers(){
    List<User> users=userService.getUsers();
    return new ResponseEntity<>(users,OK);
    }
    
  
    
    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email")String email)throws Exception{
    userService.resetPassword(email);
    return response(OK, EMAIL_SENT +email);
    }
    
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("id")long id)throws Exception{
    	userService.deleteUser(id);
    	return response(NO_CONTENT,USER_DELETED_SUCCESFULLY);
    	
    }
    @PostMapping("/updateProfileImage")
    public  ResponseEntity<User> update(@RequestParam("username")String username,
    		@RequestParam(value="profileImage")MultipartFile profileImage) throws Exception
    {
              	User user=userService.updateProfileImage(username, profileImage);
               return new ResponseEntity<>(user,OK);
     
    }
    
    
    @GetMapping(path="/image/{username}/{fileName}",produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getProfileImage(@PathVariable("username")String username,@PathVariable("fileName")String fileName) throws IOException {
    	return Files.readAllBytes(Paths.get(USER_FOLDER+ username+FORWARD_SLASH+fileName));
    }
    
    
    @GetMapping(path="/image/{profile}/{fileName}",produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getTempProfileImage(@PathVariable("username")String username,@PathVariable("fileName")String fileName) throws IOException {
    	URL url=new URL(TEMP_PROFILE_IMAGE_BASE_URL+username);
    	ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
    	try(InputStream inputStream=url.openStream()){
    		int bytesRead;
    		byte[] chunk=new byte[1024];
    		while((bytesRead=inputStream.read(chunk))>0){
    			byteArrayOutputStream.write(chunk, 0, bytesRead);
    		}  			
    		}
    		return byteArrayOutputStream.toByteArray();
    		
    		
    	}
  
   
  
 
	private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(new HttpResponse(httpStatus.value(),httpStatus,httpStatus.getReasonPhrase().toUpperCase(),message.toLowerCase()),httpStatus);
	
	}

	@GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username")String username){
    User user=userService.findUserByUsername(username);
    return new ResponseEntity<>(user,OK);
    }
    

    private HttpHeaders getJwtHeader(UserPrincipal user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER, jwtTokenProvider.generateJwtToken(user));
        return headers;
    }

    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}

