package wo1261931780.chooseCollegeJava.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import wo1261931780.chooseCollegeJava.config.ShowResult;
import wo1261931780.chooseCollegeJava.entity.AccountRole;
import wo1261931780.chooseCollegeJava.entity.LoginUser;
import wo1261931780.chooseCollegeJava.service.impl.LoginUserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:JOSP-javaFirst
 * Package:wo1261931780.javaFirst.controller
 *
 * @author liujiajun_junw
 * @Date 2023-03-20-08  星期四
 * @description
 */
@RequestMapping("/vue-element-admin/user")
@RestController
public class LoginController {
	@Autowired
	private LoginUserService loginUserService;
	//http://localhost:8081/vue-element-admin/user/login
	@PostMapping("/login")
	public ShowResult<LoginUser> userLogin(@RequestBody LoginUser loginUser) {
		// 判断
		if (loginUser != null) {
			// 查询
			//LoginUser byId = loginUserService.getById(loginUser.getId());
			//if (StrUtil.isEmptyIfStr(byId)) {
			//	return ShowResult.sendError("用户不存在");
			//}
			LambdaQueryWrapper<LoginUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
			//这里密码有一个加密的过程
			String passwordMd5DigestAsHex = DigestUtils.md5DigestAsHex(loginUser.getPassword().getBytes());
			// 查询账号密码是否正确
			lambdaQueryWrapper.eq(LoginUser::getUsername, loginUser.getUsername())
					.eq(LoginUser::getPassword, passwordMd5DigestAsHex);
			LoginUser userServiceOne = loginUserService.getOne(lambdaQueryWrapper);
			if (StrUtil.isEmptyIfStr(userServiceOne)) {
				return ShowResult.sendError("账号或密码错误");
			}
			return ShowResult.sendSuccess(userServiceOne);
		}

		// 没有id就执行注册流程
		//if (CharSequenceUtil.isNotEmpty(loginUser.getUsername()) && CharSequenceUtil.isNotEmpty(loginUser.getPassword())) {
		//	loginUser.setId(Long.valueOf(UUID.randomUUID().toString()));// 随机一个id
		//	//新增这里有问题，因为id是long类型，而UUID是String类型，所以会报错
		//	loginUser.setPassword(DigestUtils.md5DigestAsHex(loginUser.getPassword().getBytes()));// 密码通过MD5加密，然后保存回去
		//	loginUserService.insertOrUpdate(loginUser);// 插入一条数据
		//	return ShowResult.sendSuccess(loginUser);
		//}
		return ShowResult.sendError("登录失败");
	}

	/**
	 * 获取用户信息，暂时写死
	 * @return 用户信息
	 */
	@GetMapping("/info")
	public ShowResult<AccountRole> userInfo() {
		AccountRole accountRole = new AccountRole();
		List<String> strings = new ArrayList<>();
		strings.add("admin");
		accountRole.setRoles(strings);
		accountRole.setIntroduction("I am a super administrator");
		accountRole.setAvatar("https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
		accountRole.setName("Super Admin");
		return ShowResult.sendSuccess(accountRole);
	}
}
