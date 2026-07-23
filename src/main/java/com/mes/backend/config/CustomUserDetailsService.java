// Employee 기반 UserDetailsService 구현체
package com.mes.backend.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mes.backend.entity.Employee;
import com.mes.backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 계정입니다: " + username));
        String role = employee.getRole() != null ? employee.getRole() : "작업자";
        return User.builder()
                .username(employee.getUsername())
                .password(employee.getPassword())
                .authorities(role)
                .build();
    }
}
