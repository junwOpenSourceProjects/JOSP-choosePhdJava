package josp.choosphd.controller;

import josp.choosphd.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    @GetMapping
    public ApiResult<Map<String, Object>> ping() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("service", "choosephd-api");
        out.put("time", Instant.now().toString());
        return ApiResult.ok(out);
    }
}
