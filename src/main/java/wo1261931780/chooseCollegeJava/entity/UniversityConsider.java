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
*@Date 2024-10-13-39  星期六
*@Description 
*/

/**
 * 意向学校信息
 * @author junw
 */
@ApiModel(description="意向学校信息")
@Schema(description="意向学校信息")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.university_consider")
public class UniversityConsider implements Serializable {
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
     * QS排名强弱，0：弱，1：中等，2：强
     */
    @TableField(value = "status_qs")
    @ApiModelProperty(value="QS排名强弱，0：弱，1：中等，2：强")
    @Schema(description="QS排名强弱，0：弱，1：中等，2：强")
    private Byte statusQs;

    /**
     * QS计算机排名强弱，0：弱，1：中等，2：强
     */
    @TableField(value = "status_qs_cs")
    @ApiModelProperty(value="QS计算机排名强弱，0：弱，1：中等，2：强")
    @Schema(description="QS计算机排名强弱，0：弱，1：中等，2：强")
    private Byte statusQsCs;

    /**
     * US News排名强弱，0：弱，1：中等，2：强
     */
    @TableField(value = "status_usnews")
    @ApiModelProperty(value="US News排名强弱，0：弱，1：中等，2：强")
    @Schema(description="US News排名强弱，0：弱，1：中等，2：强")
    private Byte statusUsnews;

    /**
     * US News计算机排名强弱，0：弱，1：中等，2：强
     */
    @TableField(value = "status_usnews_cs")
    @ApiModelProperty(value="US News计算机排名强弱，0：弱，1：中等，2：强")
    @Schema(description="US News计算机排名强弱，0：弱，1：中等，2：强")
    private Byte statusUsnewsCs;

    /**
     * 整体排名强弱，0：弱，1：中等，2：强
     */
    @TableField(value = "status_total")
    @ApiModelProperty(value="整体排名强弱，0：弱，1：中等，2：强")
    @Schema(description="整体排名强弱，0：弱，1：中等，2：强")
    private Byte statusTotal;

    /**
     * 是否考虑，0：不考虑，1：考虑
     */
    @TableField(value = "consider")
    @ApiModelProperty(value="是否考虑，0：不考虑，1：考虑")
    @Schema(description="是否考虑，0：不考虑，1：考虑")
    private Byte consider;

    private static final long serialVersionUID = 1L;
}
