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
*Created by Intellij IDEA.
*Project:chooseCollegeJava
*Package:wo1261931780.chooseCollegeJava.entity
*@author liujiajun_junw
*@Date 2024-10-04-18  星期五
*@Description 
*/

/**
 * echarts排名数据
 */
@Schema(description="echarts排名数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_rankings_echarts")
public class UniversityRankingsEcharts implements Serializable {
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
     * qs数据
     */
    @TableField(value = "ranking_qs")
    @Schema(description = "qs数据")
    private String rankingQs;

    /**
     * qs计算机科学数据
     */
    @TableField(value = "ranking_qs_cs")
    @Schema(description = "qs计算机科学数据")
    private String rankingQsCs;

    /**
     * usnews数据
     */
    @TableField(value = "ranking_usnews")
    @Schema(description = "usnews数据")
    private String rankingUsnews;

    /**
     * usnews计算机科学数据
     */
    @TableField(value = "ranking_usnews_cs")
    @Schema(description = "usnews计算机科学数据")
    private String rankingUsnewsCs;

    private static final long serialVersionUID = 1L;
}
