package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Intellij IDEA.
 * Project:JOSP-javaFirst
 * Package:wo1261931780.javaFirst.demo
 *
 * @author liujiajun_junw
 * @Date 2023-03-20-20  星期四
 * @description
 */

/**
 * 登录用户表
 */
@Schema(description = "登录用户表")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "login_user")
public class LoginUser implements Serializable {
	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.INPUT)
	@Schema(description = "主键")
	private Long id;
	
	/**
	 * 姓名
	 */
	@TableField(value = "`name`")
	@Schema(description = "姓名")
	private String name;
	
	/**
	 * 用户名
	 */
	@TableField(value = "username")
	@Schema(description = "用户名")
	private String username;
	
	/**
	 * 密码
	 */
	@TableField(value = "`password`")
	@Schema(description = "密码")
	private String password;
	
	/**
	 * 手机号
	 */
	@TableField(value = "phone")
	@Schema(description = "手机号")
	private String phone;
	
	/**
	 * 性别
	 */
	@TableField(value = "sex")
	@Schema(description = "性别")
	private String sex;
	
	/**
	 * 身份证号
	 */
	@TableField(value = "id_number")
	@Schema(description = "身份证号")
	private String idNumber;
	
	/**
	 * 状态 0:禁用，1:正常
	 */
	@TableField(value = "`status`")
	@Schema(description = "状态 0:禁用，1:正常")
	private Integer status;
	
	/**
	 * 创建时间
	 */
	@TableField(value = "create_time",fill = FieldFill.INSERT)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
	@Schema(description = "创建时间")
	private Date createTime;
	
	/**
	 * 更新时间
	 */
	@TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
	@Schema(description = "更新时间")
	private Date updateTime;
	
	/**
	 * 创建人
	 */
	@TableField(value = "create_user",fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private Long createUser;
	
	/**
	 * 修改人
	 */
	@TableField(value = "update_user",fill = FieldFill.INSERT_UPDATE)
	@Schema(description = "修改人")
	private Long updateUser;
	
	@Serial
	private static final long serialVersionUID = 1L;
	
	public static final String COL_ID = "id";
	
	public static final String COL_NAME = "name";
	
	public static final String COL_USERNAME = "username";
	
	public static final String COL_PASSWORD = "password";
	
	public static final String COL_PHONE = "phone";
	
	public static final String COL_SEX = "sex";
	
	public static final String COL_ID_NUMBER = "id_number";
	
	public static final String COL_STATUS = "status";
	
	public static final String COL_CREATE_TIME = "create_time";
	
	public static final String COL_UPDATE_TIME = "update_time";
	
	public static final String COL_CREATE_USER = "create_user";
	
	public static final String COL_UPDATE_USER = "update_user";
}
