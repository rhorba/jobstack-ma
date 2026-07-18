package ma.jobstack.admin;

import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.auth.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User setStatus(UUID targetUserId, UserStatus status) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        user.setStatus(status);
        if (status == UserStatus.SUSPENDED) {
            user.clearRefreshToken();
        }
        userRepository.save(user);
        return user;
    }
}
