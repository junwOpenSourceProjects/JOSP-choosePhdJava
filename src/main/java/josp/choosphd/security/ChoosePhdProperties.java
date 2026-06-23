package josp.choosphd.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "choosephd")
public class ChoosePhdProperties {

    private Data data = new Data();
    private Security security = new Security();
    private Cors cors = new Cors();

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    public static class Data {
        private String rawDir;
        public String getRawDir() { return rawDir; }
        public void setRawDir(String rawDir) { this.rawDir = rawDir; }
    }

    public static class Security {
        private String bootstrapAdminUsername = "admin";
        private String bootstrapAdminPassword = "admin";
        private String jwtSecret = "choosePhd-jwt-secret-please-change-in-prod-32bytes-min";
        private int jwtTtlHours = 168;

        public String getBootstrapAdminUsername() { return bootstrapAdminUsername; }
        public void setBootstrapAdminUsername(String v) { this.bootstrapAdminUsername = v; }
        public String getBootstrapAdminPassword() { return bootstrapAdminPassword; }
        public void setBootstrapAdminPassword(String v) { this.bootstrapAdminPassword = v; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String v) { this.jwtSecret = v; }
        public int getJwtTtlHours() { return jwtTtlHours; }
        public void setJwtTtlHours(int v) { this.jwtTtlHours = v; }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://127.0.0.1:3000");
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> v) { this.allowedOrigins = v; }
    }
}
