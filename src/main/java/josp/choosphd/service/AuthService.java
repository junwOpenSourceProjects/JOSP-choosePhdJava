package josp.choosphd.service;

import josp.choosphd.api.auth.LoginRequest;
import josp.choosphd.api.auth.LoginResponse;
import josp.choosphd.domain.auth.UserPO;
import josp.choosphd.mapper.UserMapper;
import josp.choosphd.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtService jwt) {
        this.userMapper = userMapper;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest req) {
        UserPO u = userMapper.findByUsername(req.getUsername());
        if (u == null || !encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLastLogin(u.getId(), now);
        String token = jwt.issue(u.getUsername(), u.getRole());

        LoginResponse r = new LoginResponse();
        r.setToken(token);
        r.setUsername(u.getUsername());
        r.setRole(u.getRole());
        r.setExpiresIn(jwt.ttlSeconds());
        return r;
    }
}
