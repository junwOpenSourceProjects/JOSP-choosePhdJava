package wo1261931780.chooseCollegeJava.entity;

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
@Schema(description = "折线图内部包含的所有变量")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Series {
	@Schema(description = "大学名称（中文）")
	private String name;

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
