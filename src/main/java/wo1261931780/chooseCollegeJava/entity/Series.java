package wo1261931780.chooseCollegeJava.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.entity
 *
 * @author liujiajun_junw
 * @Date 2024-10-14-31  星期四
 * @Description
 */
@ApiModel(description = "折线图内部包含的所有变量")
@Schema(description = "折线图内部包含的所有变量")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Series {
	@ApiModelProperty(value = "大学名称（中文）")
	@Schema(description = "大学名称（中文）")
	private String name;

	@ApiModelProperty(value = "可视化类型，默认line")
	@Schema(description = "可视化类型，默认line")
	private String type;

	@ApiModelProperty(value = "索引，默认第一是asu")
	@Schema(description = "索引，默认第一是asu")
	private Integer xAxisIndex;
	// 可选属性，根据需要使用

	@ApiModelProperty(value = "是否平滑，默认true")
	@Schema(description = "是否平滑，默认true")
	private Boolean smooth;

	@ApiModelProperty(value = "默认线段series")
	@Schema(description = "默认线段series")
	private Emphasis emphasis;

	@ApiModelProperty(value = "排名数据，只要数据")
	@Schema(description = "排名数据，只要数据")
	private List<Double> data;
}
