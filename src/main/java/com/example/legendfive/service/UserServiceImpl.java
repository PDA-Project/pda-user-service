package com.example.legendfive.service;

import com.example.legendfive.dto.JwtDto;
import com.example.legendfive.dto.UserDto;
import com.example.legendfive.entity.Role;
import com.example.legendfive.entity.User;
import com.example.legendfive.exception.UserErrorResult;
import com.example.legendfive.exception.UserException;
import com.example.legendfive.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.legendfive.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserDto.SignInResponseDto signIn(UserDto.SignInRequestDto signInRequestDto) {
        //이미 존재하는 회원
        userRepository.save(User.builder()
                .email(signInRequestDto.getEmail())
                .password(passwordEncoder.encode(signInRequestDto.getPassword()))
                .nickname(signInRequestDto.getNickname())
                .role(Role.valueOf("USER"))
                .build());

        Optional<User> user = userRepository.findByEmail(signInRequestDto.getEmail());

        log.info("회원가입 성공");
        return UserDto.SignInResponseDto.builder()
                .userId(user.get().getId())
                .createdAt(user.get().getCreatedAt())
                .build();

    }

    public UserDto.LoginResponseDto login(UserDto.LoginRequestDto loginRequestDto) {

        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        JwtDto.JwtRequestDto jwtRequestDto = JwtDto.JwtRequestDto.builder()
                .email(loginRequestDto.getEmail())
                .userId(user.getId())
                .build();

        String jwt = jwtTokenProvider.createAccessToken(jwtRequestDto);

        return UserDto.LoginResponseDto.builder()
                .userId(user.getId())
                .createdAt(LocalDateTime.now())
                .accessToken(jwt)
                .build();
    }

    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDto.UpdatePasswordResponseDto updatePassword(UserDto.UpdatePasswordRequestDto updatePasswordRequestDto) {

        User originalUser = userRepository.findByEmail(updatePasswordRequestDto.getEmail())
                .orElseThrow(() -> new UserException(UserErrorResult.NOT_FOUND_USER));

        User updatedUser = originalUser.toBuilder()
                .password(passwordEncoder.encode(updatePasswordRequestDto.getNewPassword()))
                .build();

        userRepository.save(updatedUser);

        return UserDto.UpdatePasswordResponseDto.builder()
                .userId(updatedUser.getId())
                .updatedAt(updatedUser.getUpdatedAt())
                .build();
    }

    @Override
    public boolean userExistsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Override
    public boolean userExistsByEmailAndPassword(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (passwordEncoder.matches(password, user.getPassword())) {
                return true; // 인증 성공
            }
        }

        return false; // 인증 실패
    }

}