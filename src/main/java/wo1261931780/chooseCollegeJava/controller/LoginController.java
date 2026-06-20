package wo1261931780.chooseCollegeJava.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import wo1261931780.chooseCollegeJava.config.RoleInterceptor;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.entity.AccountRole;
import wo1261931780.chooseCollegeJava.entity.LoginUser;
import wo1261931780.chooseCollegeJava.service.impl.LoginUserService;

import java.util.Collections;

/**
 * 登录控制器
 */
@RequestMapping("/vue-element-admin/user")
@RestController
public class LoginController {
	@Autowired
	private LoginUserService loginUserService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 用户登录
	 *
	 * @param loginUser 登录信息
	 * @param session   HttpSession
	 * @return 登录成功后的用户信息
	 */
	@PostMapping("/login")
	public ShowResult<LoginUser> userLogin(@Valid @RequestBody LoginUser loginUser, HttpSession session) {
		LambdaQueryWrapper<LoginUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		lambdaQueryWrapper.eq(LoginUser::getUsername, loginUser.getUsername());
		LoginUser userServiceOne = loginUserService.getOne(lambdaQueryWrapper);
		if (userServiceOne == null || !passwordEncoder.matches(loginUser.getPassword(), userServiceOne.getPassword())) {
			return ShowResult.sendError("账号或密码错误");
		}
		userServiceOne.setPassword(null);
		session.setAttribute(RoleInterceptor.CURRENT_USER, userServiceOne);
		return ShowResult.sendSuccess(userServiceOne);
	}

	/**
	 * 获取当前登录用户信息
	 *
	 * @param session HttpSession
	 * @return 用户信息
	 */
	@GetMapping("/info")
	public ShowResult<AccountRole> userInfo(HttpSession session) {
		LoginUser currentUser = (LoginUser) session.getAttribute(RoleInterceptor.CURRENT_USER);
		if (currentUser == null) {
			return ShowResult.sendError("未登录");
		}
		AccountRole accountRole = new AccountRole();
		accountRole.setRoles(Collections.singletonList(currentUser.getRole()));
		accountRole.setIntroduction("I am " + currentUser.getRole());
		accountRole.setAvatar("https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
		accountRole.setName(currentUser.getUsername());
		return ShowResult.sendSuccess(accountRole);
	}
}