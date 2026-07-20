// 로컬 테스트용 기본 계정 시드 (회원가입 화면이 없어 서버 기동 시 1회 생성)
package com.mes.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mes.backend.entity.Member;
import com.mes.backend.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (memberRepository.findByUsername("admin").isEmpty()) {
            memberRepository.save(Member.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("1234"))
                    .build());
        }
    }
}
