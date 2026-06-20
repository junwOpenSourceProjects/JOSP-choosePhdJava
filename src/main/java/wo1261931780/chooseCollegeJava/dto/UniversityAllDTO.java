package wo1261931780.chooseCollegeJava.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 大学 4 维排名 DTO（QS/QS-CS/USNews/USNews-CS）
 * <p>
 * 这是一个跨表组装的 DTO（不是 MyBatis-Plus 实体），由 service 层从
 * university_rankings_qs / university_rankings_qs_cs / university_rankings_usnews /
 * university_rankings_usnews_cs 四张表各自聚合而成。
 * <p>
 * 历史 bug：原版所有 4 个 rank 字段都标了 {@code @TableField("current_rank_integer")}，
 * 当 BeanUtils.copyProperties(qs, dto) 时 4 个字段全部被 qs 表的同一列覆盖，
 * 后续 setCurrentQsComputerRank 等显式赋值才能修回一个；任何一步漏改就把 QS All
 * 的值错误地报告成 QS-CS/USNews/USNews-CS。
 * <p>
 * 修复：删 @TableName 让 MyBatis-Plus 彻底不识别为实体；4 个 rank 字段标
 * {@code @TableField(exist=false)}，纯内存赋值，禁止走数据库。
 */
@Schema(description = "大学 4 维排名 DTO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UniversityAllDTO implements Serializable {

	/**
	 * 大学名称（中文）
	 */
	@TableField(value = "university_name_chinese", exist = true)
	@Schema(description = "大学名称（中文）")
	private String universityNameChinese;

	/**
	 * 大学标签（例如：国家）
	 */
	@TableField(value = "university_tags", exist = true)
	@Schema(description = "大学标签（例如：国家）")
	private String universityTags;

	/**
	 * 大学标签（例如：洲）
	 */
	@TableField(value = "university_tags_state", exist = true)
	@Schema(description = "大学标签（例如：洲）")
	private String universityTagsState;

	/**
	 * 排名年份
	 */
	@TableField(value = "ranking_year", exist = true)
	@Schema(description = "排名年份")
	private String rankingYear;

	/**
	 * 当前 QS 综合排名（整数）— 由 service 显式从 university_rankings_qs 设置
	 */
	@TableField(exist = false)
	@Schema(description = "当前 QS 综合排名（整数）")
	private Integer currentQsAllRank;

	/**
	 * 当前 QS 计算机排名（整数）— 由 service 显式从 university_rankings_qs_cs 设置
	 */
	@TableField(exist = false)
	@Schema(description = "当前 QS 计算机排名（整数）")
	private Integer currentQsComputerRank;

	/**
	 * 当前 US News 综合排名（整数）— 由 service 显式从 university_rankings_usnews 设置
	 */
	@TableField(exist = false)
	@Schema(description = "当前 US News 综合排名（整数）")
	private Integer currentUsnewsAllRank;

	/**
	 * 当前 US News 计算机排名（整数）— 由 service 显式从 university_rankings_usnews_cs 设置
	 */
	@TableField(exist = false)
	@Schema(description = "当前 US News 计算机排名（整数）")
	private Integer currentUsnewsComputerRank;

	private static final long serialVersionUID = 1L;
}