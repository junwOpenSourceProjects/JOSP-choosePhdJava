package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.entity
 *
 * @author liujiajun_junw
 * @Date 2024-10-15-06  星期四
 * @Description
 */

/**
 * 大学排名数据汇总
 * @author junw
 */
@ApiModel(description = "大学排名数据汇总")
@Schema(description = "大学排名数据汇总")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_rankings_all")
public class UniversityRankingsAll implements Serializable {
	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.AUTO)
	@ApiModelProperty(value = "主键")
	@Schema(description = "主键")
	private Integer id;

	/**
	 * 大学名称（中文）
	 */
	@TableField(value = "university_name_chinese")
	@ApiModelProperty(value = "大学名称（中文）")
	@Schema(description = "大学名称（中文）")
	private String universityNameChinese;

	/**
	 * 大学名称（英文）
	 */
	@TableField(value = "university_name_english")
	@ApiModelProperty(value = "大学名称（英文）")
	@Schema(description = "大学名称（英文）")
	private String universityNameEnglish;

	/**
	 * 大学标签（例如：国家）
	 */
	@TableField(value = "university_tags")
	@ApiModelProperty(value = "大学标签（例如：国家）")
	@Schema(description = "大学标签（例如：国家）")
	private String universityTags;

	/**
	 * 大学标签（例如：洲）
	 */
	@TableField(value = "university_tags_state")
	@ApiModelProperty(value = "大学标签（例如：洲）")
	@Schema(description = "大学标签（例如：洲）")
	private String universityTagsState;

	/**
	 * 排名年份
	 */
	@TableField(value = "ranking_year")
	@ApiModelProperty(value = "排名年份")
	@Schema(description = "排名年份")
	private Object rankingYear;

	/**
	 * 当前qs排名（整数）
	 */
	@TableField(value = "current_rank_integer_qs")
	@ApiModelProperty(value = "当前qs排名（整数）")
	@Schema(description = "当前qs排名（整数）")
	private Integer currentRankIntegerQs;

	/**
	 * 当前qs计算机排名（整数）
	 */
	@TableField(value = "current_rank_integer_qs_cs")
	@ApiModelProperty(value = "当前qs计算机排名（整数）")
	@Schema(description = "当前qs计算机排名（整数）")
	private Integer currentRankIntegerQsCs;

	/**
	 * 当前usnews排名（整数）
	 */
	@TableField(value = "current_rank_integer_usnews")
	@ApiModelProperty(value = "当前usnews排名（整数）")
	@Schema(description = "当前usnews排名（整数）")
	private Integer currentRankIntegerUsnews;

	/**
	 * 当前usnews计算机排名（整数）
	 */
	@TableField(value = "current_rank_integer_usnews_cs")
	@ApiModelProperty(value = "当前usnews计算机排名（整数）")
	@Schema(description = "当前usnews计算机排名（整数）")
	private Integer currentRankIntegerUsnewsCs;

	private static final long serialVersionUID = 1L;
}
