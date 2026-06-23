package josp.choosphd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test:仅验证 Spring 上下文能起来(测试环境用 H2,无 admin bootstrap 跑实际 SQL)
 * 注意:AdminBootstrap 在 DataSource 未就绪时会 catch 所有异常(见 AdminBootstrap.java)
 */
@SpringBootTest
@ActiveProfiles("test")
class HealthSmokeTest {
    @Test
    void contextLoads() {
        // 仅验证 Spring 上下文能起
    }
}
