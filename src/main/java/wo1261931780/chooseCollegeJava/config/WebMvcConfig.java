package wo1261931780.chooseCollegeJava.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册 RBAC 拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Autowired
	private RoleInterceptor roleInterceptor;

	/**
	 * 注册 Web 拦截器
	 *
	 * @param registry 拦截器注册表
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(roleInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/vue-element-admin/user/login",
						"/vue-element-admin/user/info",
						"/actuator/**",
						"/error"
				);
	}
}