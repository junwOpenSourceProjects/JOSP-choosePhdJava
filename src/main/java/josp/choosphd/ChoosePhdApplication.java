package josp.choosphd;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("josp.choosphd.mapper")
@EnableAsync
@EnableScheduling
public class ChoosePhdApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChoosePhdApplication.class, args);
    }
}
