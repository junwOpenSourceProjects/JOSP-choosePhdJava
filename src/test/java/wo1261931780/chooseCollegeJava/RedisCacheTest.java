package wo1261931780.chooseCollegeJava;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 连接测试
 */
@SpringBootTest
class RedisCacheTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void redisShouldBeConnected() {
        stringRedisTemplate.opsForValue().set("test:choosephd", "ok");
        String value = stringRedisTemplate.opsForValue().get("test:choosephd");
        assertEquals("ok", value);
        stringRedisTemplate.delete("test:choosephd");
    }
}
