package com.ecetasci.hrmanagement.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Service
public class JwtManager {
	//1. token üretme
	// token oluştururken verilmesi gereken değerler.
	// 1. secretKey : token imzalamak için gerekli bir şifre
	// 2. Issuer: token sahibine ait bilgi
	// 3. IssuerAt: token ne zaman üretildi.
	// 4. ExpiresAt: token geçerlilik son zamanı
	// 5. Claim: token içinde key-value olarak değer tutulabilecek nesne
	// 6. sign algorithm : token imzası için kullanılacak algoritma
	
	private final long EXPIRATION_TIME = 1000*60*60*24; //24saat
	
	//private final SecretKey SECRET_KEY=Jwts.SIG.HS256.key().build();

	@Value("${app.jwtSecret}")
	private String jwtSecret;

	private SecretKey getSignInKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	public String generateToken(String username) {
		String compact = Jwts.builder()
				.subject(username)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
				.signWith(getSignInKey(), Jwts.SIG.HS256)
				.compact();
				return compact;
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}
	
	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration); //method referans
	}
	
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = extractClaims(token);
		return claimsResolver.apply(claims);
	}
	
	public Claims extractClaims(String token){
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
	}
	
	private Boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}
	
	//2. token doğrulama
	public boolean verifyToken(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	private final Set<String> blackList = new HashSet<>();

	public void invalidateToken(String token) {
		blackList.add(token);
	}

	public boolean isTokenValid(String token) {
		return !blackList.contains(token);
	}
}