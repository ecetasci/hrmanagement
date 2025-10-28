package com.ecetasci.hrmanagement.security;

import com.ecetasci.hrmanagement.constant.Endpoints;
import com.ecetasci.hrmanagement.utility.JwtManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuhenticationFilter extends OncePerRequestFilter {
	private final JwtManager jwtManager;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
								HttpServletResponse response,
								FilterChain filterChain) throws ServletException, IOException {
		System.out.println("JwtAuthenticationFilter devrede!!!");

		// normalize path: requestURI minus contextPath
		String contextPath = request.getContextPath(); // genelde "" unless app has context
		String requestUri = request.getRequestURI();
		String normalizedPath = requestUri.substring(contextPath.length());
		String method = request.getMethod();

		// debug — hangi path geldiğini görmek için
		System.out.println("[JWT FILTER] normalizedPath=" + normalizedPath + " method=" + method);

		// Bu endpointler token gerektirmez — daha esnek kontrol (context/version farklılıklarına dayanıklı)

		boolean isPublic =
				("GET".equalsIgnoreCase(method) && normalizedPath.endsWith("/reviews/public")) ||
						normalizedPath.startsWith(Endpoints.AUTH) ||
						normalizedPath.startsWith(Endpoints.USER) ||
						normalizedPath.equals(Endpoints.USER + "/login") ||
						normalizedPath.equals(Endpoints.USER + "/register") ||
						normalizedPath.equals(Endpoints.USER + "/forgot-password") ||
						normalizedPath.equals(Endpoints.USER + "/verify") ||
						normalizedPath.startsWith("/swagger-ui") ||
						normalizedPath.startsWith("/v3/api-docs") ||
						normalizedPath.startsWith("/swagger-resources") ||
						normalizedPath.startsWith("/webjars") ||
						normalizedPath.startsWith("/error");   //

		if (isPublic) {
			// JWT kontrolü yapmadan zinciri devam ettir
			filterChain.doFilter(request, response);
			return;
		}

		String authHeader = request.getHeader("Authorization");
		String token = null;
		String username = null;
		System.out.println("Authorization header: " + request.getHeader("Authorization"));

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
			System.out.println(token);
			username = jwtManager.extractUsername(token);
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);

			// ✅ Hem verifyToken hem de blacklist kontrolü
			if (jwtManager.verifyToken(token, userDetails) && jwtManager.isTokenValid(token)) {
				UsernamePasswordAuthenticationToken authToken =
					new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()
					);
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);

			}
		}

		filterChain.doFilter(request, response);
	}
}
