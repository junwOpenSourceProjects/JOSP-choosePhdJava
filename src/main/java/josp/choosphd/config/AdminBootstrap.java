package josp.choosphd.config;

import josp.choosphd.domain.auth.UserPO;
import josp.choosphd.mapper.UserMapper;
import josp.choosphd.security.ChoosePhdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserMapper userMapper;
    private final ChoosePhdProperties props;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminBootstrap(UserMapper userMapper, ChoosePhdProperties props) {
        this.userMapper = userMapper;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAdmin() {
        try {
            String u = props.getSecurity().getBootstrapAdminUsername();
            String p = props.getSecurity().getBootstrapAdminPassword();
            if (userMapper.findByUsername(u) == null) {
                UserPO admin = new UserPO();
                admin.setUsername(u);
                admin.setPasswordHash(encoder.encode(p));
                admin.setRole("ADMIN");
                admin.setCreatedAt(LocalDateTime.now());
                userMapper.insert(admin);
                log.info("Bootstrap admin created: {}", u);
            } else {
                log.info("Bootstrap admin already exists: {}", u);
            }
        } catch (Exception e) {
            // 表还没建 / DB 不可用,启动期容错
            log.warn("AdminBootstrap skipped: {}", e.getMessage());
        }
    }
}
