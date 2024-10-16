package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
*Created by Intellij IDEA.
*Project:JOSP-examinationSystemJava
*Package:wo1261931780.JOSPexaminationSystemJava.entity
*@author liujiajun_junw
*@Date 2023-03-15-23  星期六
*@description
*/

/**
 * 账号角色表
 */
@ApiModel(description = "账号角色表")
@Schema(description = "账号角色表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "account_role")
public class AccountRole implements Serializable {
	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.AUTO)
	@ApiModelProperty(value = "主键")
	@Schema(description = "主键")
	private String id;
	
	/**
	 * 角色
	 */
	@TableField(value = "roles")
	@ApiModelProperty(value = "角色")
	@Schema(description = "角色")
	private List<String> roles;
	
	/**
	 * 说明
	 */
	@TableField(value = "introduction")
	@ApiModelProperty(value = "说明")
	@Schema(description = "说明")
	private String introduction;
	
	/**
	 * 头像
	 */
	@TableField(value = "avatar")
	@ApiModelProperty(value = "头像")
	@Schema(description = "头像")
	private String avatar;
	
	/**
	 * 名称
	 */
	@TableField(value = "`name`")
	@ApiModelProperty(value = "名称")
	@Schema(description = "名称")
	private String name;
	
	private static final long serialVersionUID = 1L;
	
	public static final String COL_ID = "id";
	
	public static final String COL_ROLES = "roles";
	
	public static final String COL_INTRODUCTION = "introduction";
	
	public static final String COL_AVATAR = "avatar";
	
	public static final String COL_NAME = "name";
}
