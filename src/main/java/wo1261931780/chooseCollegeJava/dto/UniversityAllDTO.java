package wo1261931780.chooseCollegeJava.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsQs;

import java.io.Serializable;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.dto
 *
 * @author liujiajun_junw
 * @Date 2024-10-17-43  星期三
 * @Description
 */
@ApiModel(description = "大学qs排名数据")
@Schema(description = "大学qs排名数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_rankings_qs")
public class UniversityAllDTO implements Serializable {

	/**
	 * 大学名称（中文）
	 */
	@TableField(value = "university_name_chinese")
	@ApiModelProperty(value = "大学名称（中文）")
	@Schema(description = "大学名称（中文）")
	private String universityNameChinese;

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
	private String rankingYear;

	/**
	 * 当前qs世界排名（整数）
	 */
	@TableField(value = "current_rank_integer")
	@ApiModelProperty(value = "当前qs世界排名（整数）")
	@Schema(description = "当前qs世界排名（整数）")
	private Integer currentQsAllRank;

	/**
	 * 当前qs计算机排名（整数）
	 */
	@TableField(value = "current_rank_integer")
	@ApiModelProperty(value = "当前qs计算机排名（整数）")
	@Schema(description = "当前qs计算机排名（整数）")
	private Integer currentQsComputerRank;
	/**
	 * 当前USNews世界排名（整数）
	 */
	@TableField(value = "current_rank_integer")
	@ApiModelProperty(value = "当前USNews世界排名（整数）")
	@Schema(description = "当前USNews世界排名（整数）")
	private Integer currentUsnewsAllRank;
	/**
	 * 当前USNews计算机排名（整数）
	 */
	@TableField(value = "current_rank_integer")
	@ApiModelProperty(value = "当前USNews计算机排名（整数）")
	@Schema(description = "当前USNews计算机排名（整数）")
	private Integer currentUsnewsComputerRank;

	private static final long serialVersionUID = 1L;
}
