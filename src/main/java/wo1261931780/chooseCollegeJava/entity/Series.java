package wo1261931780.chooseCollegeJava.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 折线图内部包含的所有变量
 */
@Schema(description = "折线图内部包含的所有变量")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Series implements Serializable {

	private static final long serialVersionUID = 1L;
	@Schema(description = "大学名称（中文）")
	private String name;

	@Schema(description = "国家/地区（大学标签 country, 从 university_tags 取）")
	private String country;

	@Schema(description = "洲（大学标签 state, 从 university_tags_state 取）")
	private String region;

	@Schema(description = "可视化类型，默认line")
	private String type;

	@Schema(description = "索引，默认第一是asu")
	private Integer xAxisIndex;
	// 可选属性，根据需要使用

	@Schema(description = "是否平滑，默认true")
	private Boolean smooth;

	@Schema(description = "默认线段series")
	private Emphasis emphasis;

	@Schema(description = "排名数据，只要数据")
	private List<Double> data;
}