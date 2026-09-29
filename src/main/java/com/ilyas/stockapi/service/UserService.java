package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.UserRequest;
import com.ilyas.stockapi.dto.UserResponse;
import com.ilyas.stockapi.entity.AppUser;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserResponse> find(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username " + request.username() + " is already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        AppUser user = userRepository.findById(id).orElseThrow(NotFoundException::new);
        if (user.getUsername().equals(currentUsername)) {
            throw new ConflictException("You cannot delete your own account");
        }
        userRepository.delete(user);
    }
}
