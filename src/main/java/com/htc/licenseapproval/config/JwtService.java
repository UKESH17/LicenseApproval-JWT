package com.htc.licenseapproval.config;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.htc.licenseapproval.service.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

	@Autowired
	@Lazy
	private UserService usersService;
	
	private Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public Claims extractAllClaims(String token) {

		try {
			return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();

		} catch (ExpiredJwtException e) {

			LoggerFactory.getLogger(JwtService.class).warn("JWT token is expired: {}", token);
			throw new ExpiredJwtException(null, null, "JWT token is expired");
		} catch (Exception e) {

			LoggerFactory.getLogger(JwtService.class).error("Error parsing JWT token: {}", token, e);
			throw new RuntimeException("Invalid JWT token", e);
		}
	}

	private Key getSignKey() {
		byte[] keyBytes = Decoders.BASE64.decode(JwtConstants.SECRET);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private Boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());

	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !isTokenBlacklisted(token));
	}

	public String generateToken(String name) {

		UserDetails userDetails = usersService.loadUserByUsername(name);
		List<String> roles = userDetails.getAuthorities().stream().map(auth -> auth.getAuthority())
				.collect(Collectors.toList());
		return Jwts.builder().setSubject(userDetails.getUsername()).claim("roles", roles).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)).signWith(getSignKey()).compact();
	}

	public void invalidateToken(String token) {
		if(token!=null) {
	    tokenBlacklist.add(token);
	    log.warn("JWT token is blacklisted : {}", token);
		}
		else 
		{
			throw new RuntimeException("The token cannot be null while invalidating");
		}

	}

	public boolean isTokenBlacklisted(String token) {
	    return tokenBlacklist.contains(token);
	}

}
