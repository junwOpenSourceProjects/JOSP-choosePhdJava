package josp.choosphd.controller;

import josp.choosphd.api.auth.LoginRequest;
import josp.choosphd.api.auth.LoginResponse;
import josp.choosphd.common.ApiResult;
import josp.choosphd.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return ApiResult.ok(service.login(req));
    }
}
