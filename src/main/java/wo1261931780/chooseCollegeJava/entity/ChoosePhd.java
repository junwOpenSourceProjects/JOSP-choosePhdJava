package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
*Created by Intellij IDEA.
*Project:JOSP-choosePhdJava
*Package:wo1261931780.chooseCollegeJava.entity
*@author liujiajun_junw
*@Date 2024-11-16-53  星期日
*@Description 
*/

/**
 * 院校信息表
 */
@ApiModel(description="院校信息表")
@Schema(description="院校信息表")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "computer_rank.choose_phd")
public class ChoosePhd implements Serializable {
    /**
     * 主键，雪花
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value="主键，雪花")
    @Schema(description="主键，雪花")
    private Long id;

    /**
     * 大学名称
     */
    @TableField(value = "university_name")
    @ApiModelProperty(value="大学名称")
    @Schema(description="大学名称")
    private String universityName;

    /**
     * 大学排名相关数据，包含名称和链接
     */
    @TableField(value = "ranking_data")
    @ApiModelProperty(value="大学排名相关数据，包含名称和链接")
    @Schema(description="大学排名相关数据，包含名称和链接")
    private String rankingData;

    /**
     * 院校官方网站链接
     */
    @TableField(value = "official_website")
    @ApiModelProperty(value="院校官方网站链接")
    @Schema(description="院校官方网站链接")
    private String officialWebsite;

    /**
     * 社招网站链接，可为空
     */
    @TableField(value = "recruitment_website")
    @ApiModelProperty(value="社招网站链接，可为空")
    @Schema(description="社招网站链接，可为空")
    private String recruitmentWebsite;

    /**
     * 优先级
     */
    @TableField(value = "priority")
    @ApiModelProperty(value="优先级")
    @Schema(description="优先级")
    private Integer priority;

    /**
     * 国家/地区
     */
    @TableField(value = "country_region")
    @ApiModelProperty(value="国家/地区")
    @Schema(description="国家/地区")
    private String countryRegion;

    /**
     * 奖学金信息
     */
    @TableField(value = "scholarship")
    @ApiModelProperty(value="奖学金信息")
    @Schema(description="奖学金信息")
    private String scholarship;

    /**
     * 薪资金额
     */
    @TableField(value = "salary_amount")
    @ApiModelProperty(value="薪资金额")
    @Schema(description="薪资金额")
    private BigDecimal salaryAmount;

    /**
     * 薪资货币类型，如£、US$
     */
    @TableField(value = "salary_currency")
    @ApiModelProperty(value="薪资货币类型，如£、US$")
    @Schema(description="薪资货币类型，如£、US$")
    private String salaryCurrency;

    /**
     * 生活费用金额
     */
    @TableField(value = "living_expenses_amount")
    @ApiModelProperty(value="生活费用金额")
    @Schema(description="生活费用金额")
    private BigDecimal livingExpensesAmount;

    /**
     * 生活费用货币类型，如£、US$
     */
    @TableField(value = "living_expenses_currency")
    @ApiModelProperty(value="生活费用货币类型，如£、US$")
    @Schema(description="生活费用货币类型，如£、US$")
    private String livingExpensesCurrency;

    /**
     * 研究方向
     */
    @TableField(value = "research_field")
    @ApiModelProperty(value="研究方向")
    @Schema(description="研究方向")
    private String researchField;

    /**
     * 申请要求
     */
    @TableField(value = "application_requirements")
    @ApiModelProperty(value="申请要求")
    @Schema(description="申请要求")
    private String applicationRequirements;

    /**
     * 招生截止时间
     */
    @TableField(value = "application_deadline")
    @ApiModelProperty(value="招生截止时间")
    @Schema(description="招生截止时间")
    private Date applicationDeadline;

    /**
     * 是否禁毒（1为是，0为否）
     */
    @TableField(value = "drug_prohibition")
    @ApiModelProperty(value="是否禁毒（1为是，0为否）")
    @Schema(description="是否禁毒（1为是，0为否）")
    private Boolean drugProhibition;

    /**
     * 是否控枪（1为是，0为否）
     */
    @TableField(value = "gun_control")
    @ApiModelProperty(value="是否控枪（1为是，0为否）")
    @Schema(description="是否控枪（1为是，0为否）")
    private Boolean gunControl;

    /**
     * QS排名
     */
    @TableField(value = "qs_rank")
    @ApiModelProperty(value="QS排名")
    @Schema(description="QS排名")
    private Integer qsRank;

    /**
     * US News排名
     */
    @TableField(value = "usnews_rank")
    @ApiModelProperty(value="US News排名")
    @Schema(description="US News排名")
    private Integer usnewsRank;

    /**
     * 学制，如4年制
     */
    @TableField(value = "education_duration")
    @ApiModelProperty(value="学制，如4年制")
    @Schema(description="学制，如4年制")
    private String educationDuration;

    /**
     * 身份难度，如难
     */
    @TableField(value = "application_difficulty")
    @ApiModelProperty(value="身份难度，如难")
    @Schema(description="身份难度，如难")
    private String applicationDifficulty;

    /**
     * 参考资料链接或描述
     */
    @TableField(value = "reference_material")
    @ApiModelProperty(value="参考资料链接或描述")
    @Schema(description="参考资料链接或描述")
    private String referenceMaterial;

    private static final long serialVersionUID = 1L;
}