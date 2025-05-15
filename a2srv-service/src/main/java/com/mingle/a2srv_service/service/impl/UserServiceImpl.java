package com.mingle.a2srv_service.service.impl;

import com.mingle.a2srv_service.dto.UserRequest;
import com.mingle.a2srv_service.dto.UserResponse;
import com.mingle.a2srv_service.entity.User;
import com.mingle.a2srv_service.exception.type.*;
import com.mingle.a2srv_service.mapper.UserMapper;
import com.mingle.a2srv_service.repository.UserRepository;
import com.mingle.a2srv_service.security.JwtTokenProvider;
import com.mingle.a2srv_service.service.UserService;
import com.mingle.a2srv_service.validation.UserRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRequestValidator userRequestValidator;
    private final UserMapper userMapper;

    @Override
    public UserResponse register(UserRequest request) {
        validate(request);

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username already in use");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setIsOnline(false);
        user.setVerified(false);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getCurrentUser(String bearerToken) {
        return userMapper.toResponse(getUserFromToken(bearerToken));
    }

    @Override
    public UserResponse updateProfile(String bearerToken, UserRequest request) {
        validate(request);
        User user = getUserFromToken(bearerToken);

        if (userRepository.existsByEmailAndIdNot(request.email(), user.getId())) {
            throw new EmailAlreadyExistsException("Email already in use by another user");
        }

        if (userRepository.existsByUsernameAndIdNot(request.username(), user.getId())) {
            throw new UsernameAlreadyExistsException("Username already in use by another user");
        }

        userMapper.updateEntity(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void updatePassword(String bearerToken, String oldPassword, String newPassword) {
        User user = getUserFromToken(bearerToken);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public Page<UserResponse> findByCity(String city, Pageable pageable) {
        return userRepository.findByCity(city, pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findByGender(String gender, Pageable pageable) {
        return userRepository.findByGender(gender, pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findByDatePreference(String pref, Pageable pageable) {
        return userRepository.findByDatePreference(pref, pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findByRelationshipGoal(String goal, Pageable pageable) {
        return userRepository.findByRelationshipGoal(goal, pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findByAgeRange(int min, int max, Pageable pageable) {
        if (min > max) {
            throw new IllegalArgumentException("Min age cannot be greater than max age");
        }
        return userRepository.findByAgeBetween(min, max, pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findVerifiedUsers(Pageable pageable) {
        return userRepository.findByVerifiedTrue(pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findOnlineUsers(Pageable pageable) {
        return userRepository.findByIsOnlineTrue(pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findLatestActiveUsers(Pageable pageable) {
        return userRepository.findAllByOrderByLastActiveAtDesc(pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findNearbyUsers(float lat, float lon, double radius, Pageable pageable) {
        if (radius <= 0 || radius > 100) {
            throw new IllegalArgumentException("Radius must be between 1 and 100 kilometers");
        }
        return userRepository.findNearbyUsers(lat, lon, radius, pageable).map(userMapper::toResponse);
    }

    private User getUserFromToken(String bearerToken) {
        String token = jwtTokenProvider.resolveToken(bearerToken);
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private void validate(UserRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, "userRequest");
        userRequestValidator.validate(request, errors);
        if (errors.hasErrors()) {
            List<String> messages = errors.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new ValidationException(messages.toString());
        }
    }
}
