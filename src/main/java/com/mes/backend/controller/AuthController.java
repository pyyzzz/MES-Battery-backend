// 세션 기반 로그인/로그아웃 API
package com.mes.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.LoginRequest;
import com.mes.backend.entity.Employee;
import com.mes.backend.repository.EmployeeRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            return ResponseEntity.ok(toUserInfo(authentication));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    /* 세션 복구용 - 세션 쿠키가 유효하면 200+사용자 정보, 없으면 SecurityConfig의 anyRequest().authenticated()가 401 처리 */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return ResponseEntity.ok(toUserInfo(authentication));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        httpRequest.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toUserInfo(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null);
        Employee employee = employeeRepository.findByUsername(authentication.getName()).orElse(null);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("role", role);
        userInfo.put("employeeId", employee != null ? employee.getId() : null);
        userInfo.put("employeeNo", employee != null ? employee.getEmployeeNo() : null);
        userInfo.put("employeeName", employee != null ? employee.getEmployeeName() : null);
        userInfo.put("displayName", displayName(authentication.getName(), role, employee));
        return userInfo;
    }

    private String displayName(String username, String role, Employee employee) {
        if (employee != null && employee.getEmployeeName() != null && !employee.getEmployeeName().isBlank()) {
            return employee.getEmployeeName();
        }
        if ("admin".equalsIgnoreCase(username) && role != null && !role.isBlank()) {
            return role;
        }
        return username;
    }
}
