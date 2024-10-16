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
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.entity
*@author liujiajun_junw
*@Date 2024-10-00-56  星期四
*@Description 
*/

/**
 * 大学USNews排名计算机数据
 */
@ApiModel(description="大学USNews排名计算机数据")
@Schema(description="大学USNews排名计算机数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_rankings_usnews_cs")
public class UniversityRankingsUsnewsCs implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value="主键")
    @Schema(description="主键")
    private Integer id;

    /**
     * 大学名称（中文）
     */
    @TableField(value = "university_name_chinese")
    @ApiModelProperty(value="大学名称（中文）")
    @Schema(description="大学名称（中文）")
    private String universityNameChinese;

    /**
     * 大学名称（英文）
     */
    @TableField(value = "university_name_english")
    @ApiModelProperty(value="大学名称（英文）")
    @Schema(description="大学名称（英文）")
    private String universityNameEnglish;

    /**
     * 大学标签（例如：国家）
     */
    @TableField(value = "university_tags")
    @ApiModelProperty(value="大学标签（例如：国家）")
    @Schema(description="大学标签（例如：国家）")
    private String universityTags;

    /**
     * 大学标签（例如：洲）
     */
    @TableField(value = "university_tags_state")
    @ApiModelProperty(value="大学标签（例如：洲）")
    @Schema(description="大学标签（例如：洲）")
    private String universityTagsState;

    /**
     * 排名类别（例如：计算机科学）
     */
    @TableField(value = "ranking_category")
    @ApiModelProperty(value="排名类别（例如：计算机科学）")
    @Schema(description="排名类别（例如：计算机科学）")
    private String rankingCategory;

    /**
     * 排名年份
     */
    @TableField(value = "ranking_year")
    @ApiModelProperty(value="排名年份")
    @Schema(description="排名年份")
    private String rankingYear;

    /**
     * 当前排名（整数）
     */
    @TableField(value = "current_rank_integer")
    @ApiModelProperty(value="当前排名（整数）")
    @Schema(description="当前排名（整数）")
    private Integer currentRankInteger;

    /**
     * 当前排名（原始数据，例如“=2”）
     */
    @TableField(value = "current_rank_raw")
    @ApiModelProperty(value="当前排名（原始数据，例如“=2”）")
    @Schema(description="当前排名（原始数据，例如“=2”）")
    private String currentRankRaw;

    /**
     * 排名类别
     */
    @TableField(value = "rank_variant")
    @ApiModelProperty(value="排名类别")
    @Schema(description="排名类别")
    private String rankVariant;

    private static final long serialVersionUID = 1L;
}
