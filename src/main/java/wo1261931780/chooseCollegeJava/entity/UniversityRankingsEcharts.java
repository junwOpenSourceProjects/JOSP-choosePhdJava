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
import springfox.documentation.spring.web.json.Json;

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
@ApiModel(description="echarts排名数据")
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
     * qs数据
     */
    @TableField(value = "ranking_qs")
    @ApiModelProperty(value="qs数据")
    @Schema(description="qs数据")
    private String rankingQs;

    /**
     * qs计算机科学数据
     */
    @TableField(value = "ranking_qs_cs")
    @ApiModelProperty(value="qs计算机科学数据")
    @Schema(description="qs计算机科学数据")
    private String rankingQsCs;

    /**
     * usnews数据
     */
    @TableField(value = "ranking_usnews")
    @ApiModelProperty(value="usnews数据")
    @Schema(description="usnews数据")
    private String rankingUsnews;

    /**
     * usnews计算机科学数据
     */
    @TableField(value = "ranking_usnews_cs")
    @ApiModelProperty(value="usnews计算机科学数据")
    @Schema(description="usnews计算机科学数据")
    private String rankingUsnewsCs;

    private static final long serialVersionUID = 1L;
}
