package com.htc.licenseapproval.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.htc.licenseapproval.constants.LogMessages;
import com.htc.licenseapproval.dto.LoginRequest;
import com.htc.licenseapproval.dto.RegisterUser;
import com.htc.licenseapproval.dto.UpdatePasswordDTO;
import com.htc.licenseapproval.entity.OTP;
import com.htc.licenseapproval.entity.UserCredentials;
import com.htc.licenseapproval.entity.UserLog;
import com.htc.licenseapproval.enums.OTPtype;
import com.htc.licenseapproval.repository.LogRepository;
import com.htc.licenseapproval.repository.OTPrepository;
import com.htc.licenseapproval.repository.UserCredentialsRepository;
import com.htc.licenseapproval.response.BaseResponse;
import com.htc.licenseapproval.service.UserService;
import com.htc.licenseapproval.utils.EmailService;
import com.htc.licenseapproval.utils.OTPservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Authentication Controller", description = "APIs for handling users login and singup")
public class AuthController {

	@Autowired
	private UserService userService;

	@Autowired
	private LogRepository logRepository;

	@Autowired
	private OTPservice otpService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserCredentialsRepository userCredentialsRepository;


	@Autowired
	private OTPrepository otpRepository;

	@PostMapping("/registerUser")
	@Operation(summary = "Register user", description = "To register new user")
	public ResponseEntity<BaseResponse<String>> registerUser( @Valid @RequestBody RegisterUser registerUser) {
		

		UserCredentials userCredentials = new UserCredentials();
		userCredentials.setUsername(registerUser.getUsername());
		userCredentials.setPassword(registerUser.getPassword());
		userCredentials.setEmail(registerUser.getEmail());
		UserCredentials user = userService.saveUser(userCredentials);
		if (user != null) {

			UserLog userLog = UserLog.builder()
					.logDetails(String.format(LogMessages.REGISTER_USER, user.getUsername()))
					.loggedTime(LocalDateTime.now())
					.build();

			logRepository.save(userLog);
			
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.OK.value());
			response.setData("Registered Successfully with username " + user.getUsername());
			response.setMessage("Registration success");
			
			
			return ResponseEntity.ok(response);

		}
		
		BaseResponse<String> response = new BaseResponse<>();
		response.setCode(HttpStatus.NO_CONTENT.value());
		response.setData("No content");
		response.setMessage("Registration");
		
		
		return new ResponseEntity<BaseResponse<String>>(response,HttpStatus.NO_CONTENT);
		
	}

	
	@DeleteMapping("/deleteUser")
	@Operation(summary = "Delete user", description = "To register user by username")
	public  ResponseEntity<BaseResponse<String>>  deleteUser(@RequestParam String userName) {
		if (userService.deleteUser(userName) != null) {
			
			UserLog userLog = UserLog.builder()
					.logDetails(String.format(LogMessages.USER_DELETED,  userName))
					.loggedTime(LocalDateTime.now())
					.build();

			logRepository.save(userLog);
			

			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.OK.value());
			response.setData("User Deleted Successfully with username " + userName);
			response.setMessage("Delete User");
			
			
			return ResponseEntity.ok(response);
			
		}
		BaseResponse<String> response = new BaseResponse<>();
		response.setCode(HttpStatus.NO_CONTENT.value());
		response.setData("User Deleted failed with username " + userName);
		response.setMessage("Delete User");
		
		
		return new ResponseEntity<BaseResponse<String>>(response,HttpStatus.NO_CONTENT);
	}

	@PostMapping("/login")
	@Operation(summary = "Login user", description = "To login user by username")
	public ResponseEntity<BaseResponse<String>> login(@RequestBody @Valid LoginRequest loginRequest){
		try {
			
			String username = loginRequest.getUsername();
		
			UserCredentials user = userService.findByUsername(username);
	
			authenticate(username, loginRequest.getPassword());

			
			if (!user.isOTPenabled()) {
				
				OTP otp = otpService.generateOTP(user);
				
			  
				
			    emailService.sendVerficationOtpEmail(user.getEmail(), otp.getOtp(),user.getUsername(),OTPtype.LOGIN);
			    otpRepository.save(otp);
			    user.setOTPenabled(true);
				userCredentialsRepository.save(user);
				UserLog userLog = UserLog.builder()
						.logDetails(String.format(LogMessages.LOGIN_SUCCESS, username))
						.loggedTime(LocalDateTime.now())
						.build();
				
				logRepository.save(userLog);
				
				log.info("Logged successfully as username : " + username);
				
				BaseResponse<String> response = new BaseResponse<>();
				response.setCode(HttpStatus.OK.value());
				response.setData("OTP send to your registered mail : " +user.getEmail());
				response.setMessage("Login");
				
				
				return ResponseEntity.ok(response);
			    
			}
			
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.ALREADY_REPORTED.value());
			response.setData("Use the previous OTP sent through the email within 2 mins");
			response.setMessage("Login");
			
			
			return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body(response);


		} catch( MessagingException |  MailException e) {
			
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.BAD_REQUEST.value());
			response.setData(e.getMessage());
			response.setMessage("Login");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		
		
				
		catch (AuthenticationException e) {
			
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.UNAUTHORIZED.value());
			response.setData("Invalid credentials");
			response.setMessage("Login");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			
		}
	}

	@PostMapping("/login/otpVerification")
	public ResponseEntity<BaseResponse<String>> otpVerification(@RequestParam String OTP, @RequestParam String username,
			HttpServletRequest request) {
		UserCredentials userCredentials = userService.findByUsername(username);
		if (otpService.validateOTP(OTP, username) && userCredentials.isOTPenabled()) {
			
			UserDetails userDetails = userService.loadUserByUsername(username);
			UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
					userDetails, null, userDetails.getAuthorities());
			authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authenticationToken);
			request.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
			request.getSession(true).setMaxInactiveInterval(30*60);
			UserLog userLog = UserLog.builder ()
					.logDetails(String.format(LogMessages.OTP_VERIFIED, userCredentials.getEmail()))
					.loggedTime(LocalDateTime.now())
					.build();

			logRepository.save(userLog);
			otpService.removeOtp(username);
			log.info("OTP verified successfully");
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.ACCEPTED.value());
			response.setData("OTP verified successfully ");
			response.setMessage("Login - otp verification");
			
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

		}
		
		BaseResponse<String> response = new BaseResponse<>();
		response.setCode(HttpStatus.UNAUTHORIZED.value());
		response.setData("Invalid OTP");
		response.setMessage("Login - otp verification");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

	

	}

	@PostMapping("/forgotpassword")
	public ResponseEntity<BaseResponse<String>> forgotPasssword(@RequestParam String username) throws MessagingException {
		OTP otp = null;
		UserCredentials user = userService.findByUsername(username);
		if (!user.isOTPenabled()) {
			otp = otpService.generateOTP(user);
			
			UserLog userLog2 = UserLog.builder()
					.logDetails(String.format(LogMessages.FORGET_PASSWORD, user.getUsername()))
					.loggedTime(LocalDateTime.now())
					.build();
			

			logRepository.save(userLog2);
			emailService.sendVerficationOtpEmail(user.getEmail(), otp.getOtp(),user.getUsername(),OTPtype.FORGOT_PASSWORD);
			
			  otpRepository.save(otp);
			  user.setOTPenabled(true);
			  userCredentialsRepository.save(user);
			
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.ACCEPTED.value());
			response.setData("OTP successfully sent to your registered e-mail id : "+user.getEmail());
			response.setMessage("Forgot password");
						
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
		}
		BaseResponse<String> response = new BaseResponse<>();
		response.setCode(HttpStatus.ALREADY_REPORTED.value());
		response.setData("Use the previous OTP sent through the email within 2 mins");
		response.setMessage("Forgot password");
		
		
		return ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body(response);

	}

	@PostMapping("/changePassword/{OTP}")
	public ResponseEntity<BaseResponse<String>> changePassword(@RequestBody @Valid UpdatePasswordDTO loginRequest,
			@PathVariable String OTP) {
		if (otpService.validateOTP(OTP, loginRequest.getUsername())) {

			UserCredentials credentials = userService.updatePassword(loginRequest.getUsername(),
					loginRequest.getPassword());
			if (credentials != null) {
				
				UserLog userLog = UserLog.builder()
						.logDetails(String.format(LogMessages.OTP_VERIFIED, credentials.getUsername()))
						.loggedTime(LocalDateTime.now())
						.build();

				logRepository.save(userLog);
				otpService.removeOtp(loginRequest.getUsername());
				
				BaseResponse<String> response = new BaseResponse<>();
				response.setCode(HttpStatus.ACCEPTED.value());
				response.setData("Password changed successfully for username : "+loginRequest.getUsername());
				response.setMessage("Forgot password - otp verification");
							
				return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
				
			}
			BaseResponse<String> response = new BaseResponse<>();
			response.setCode(HttpStatus.UNAUTHORIZED.value());
			response.setData("Credentials is null");
			response.setMessage("Forgot password - otp verification");
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
		
		BaseResponse<String> response = new BaseResponse<>();
		response.setCode(HttpStatus.UNAUTHORIZED.value());
		response.setData("Invalid OTP");
		response.setMessage("Forgot password - otp verification");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

	}

	@PostMapping("/logout")
	public ResponseEntity<BaseResponse<String>> logout(HttpServletRequest request) {
		
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		
		HttpSession session = request.getSession(false);
		
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		
		UserLog userLog = UserLog.builder()
				.logDetails(String.format(LogMessages.USER_LOGGEDOUT, username))
				.loggedTime(LocalDateTime.now())
				.build();

		logRepository.save(userLog);
		
		BaseResponse<String> response = new BaseResponse<>();
		response.setCode(HttpStatus.ACCEPTED.value());
		response.setData("Logged out successfully for username : "+username);
		response.setMessage("Logged out");
					
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
		
	
	}
	
	@GetMapping("/{username}")
	public UserCredentials crdd(@PathVariable String username) {
		return userService.findByUsername(username);
	}

	private Authentication authenticate(String userName, String password) {
		return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
	}
	
	 @GetMapping("/page")
	    public Page<UserCredentials> getUsers(
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "2") int size,
	        @RequestParam(defaultValue = "id") String sortBy
	    ) {
	        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortBy));
	        return userCredentialsRepository.findAll(pageable);
	    }

}
