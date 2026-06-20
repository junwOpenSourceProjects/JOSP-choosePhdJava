package wo1261931780.chooseCollegeJava.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import wo1261931780.chooseCollegeJava.entity.LoginUser;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 角色权限拦截器
 */
@Slf4j
@Component
public class RoleInterceptor implements HandlerInterceptor {

	/**
	 * Session 中当前登录用户的 key
	 */
	public static final String CURRENT_USER = "currentUser";

	/**
	 * 权限拦截前置处理，校验用户角色权限
	 *
	 * @param request  请求
	 * @param response 响应
	 * @param handler  处理器
	 * @return 是否放行
	 * @throws Exception 异常
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
		if (requireRole == null) {
			requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
		}
		if (requireRole == null) {
			return true;
		}

		LoginUser currentUser = (LoginUser) request.getSession().getAttribute(CURRENT_USER);
		if (currentUser == null) {
			log.warn("未登录访问受保护接口: {}", request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
			return false;
		}

		Set<String> requiredRoles = Arrays.stream(requireRole.value()).collect(Collectors.toSet());
		String userRole = currentUser.getRole();
		if (userRole == null || !requiredRoles.contains(userRole)) {
			log.warn("用户 [{}] 角色 [{}] 无权访问: {}", currentUser.getUsername(), userRole, request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\"}");
			return false;
		}

		return true;
	}
}