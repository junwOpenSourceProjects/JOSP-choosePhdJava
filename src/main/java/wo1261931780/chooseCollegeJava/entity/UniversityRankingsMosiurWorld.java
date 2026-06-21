package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大学MOSIUR全球排名
 * @author junw
 */
@Schema(description="大学MOSIUR全球排名")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_rankings_mosiur_world")
public class UniversityRankingsMosiurWorld implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键")
    private Integer id;

    /**
     * 大学名称（中文）
     */
    @TableField(value = "university_name_chinese")
    @Schema(description = "大学名称（中文）")
    private String universityNameChinese;

    /**
     * 大学名称（英文）
     */
    @TableField(value = "university_name_english")
    @Schema(description = "大学名称（英文）")
    private String universityNameEnglish;

    /**
     * 大学标签（例如：国家）
     */
    @TableField(value = "university_tags")
    @Schema(description = "大学标签（例如：国家）")
    private String universityTags;

    /**
     * 大学标签（例如：洲）
     */
    @TableField(value = "university_tags_state")
    @Schema(description = "大学标签（例如：洲）")
    private String universityTagsState;

    /**
     * 排名类别
     */
    @TableField(value = "ranking_category")
    @Schema(description = "排名类别")
    private String rankingCategory;

    /**
     * 排名年份
     */
    @TableField(value = "ranking_year")
    @Schema(description = "排名年份")
    private String rankingYear;

    /**
     * 当前排名（整数）
     */
    @TableField(value = "current_rank_integer")
    @Schema(description = "当前排名（整数）")
    private Integer currentRankInteger;

    /**
     * 当前排名（原始数据，例如\"#1\"）
     */
    @TableField(value = "current_rank_raw")
    @Schema(description = "当前排名（原始数据，例如\"#1\"）")
    private String currentRankRaw;

    /**
     * rank_variant slug
     */
    @TableField(value = "rank_variant")
    @Schema(description = "rank_variant slug")
    private String rankVariant;

    private static final long serialVersionUID = 1L;
}
