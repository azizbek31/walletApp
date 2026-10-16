package uz.fintech.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.fintech.wallet.dto.RegisterRequest;
import uz.fintech.wallet.entity.User;
import uz.fintech.wallet.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request){
        if (userRepository.existsByPhone(request.phone())){
            throw new IllegalArgumentException("Phone number already exists");
        }
        User user = new User();
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return userRepository.save(user);
    }
}
