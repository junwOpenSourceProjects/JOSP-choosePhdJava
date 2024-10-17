package wo1261931780.chooseCollegeJava.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.entity
 *
 * @author liujiajun_junw
 * @Date 2024-10-14-32  星期四
 * @Description
 */
@ApiModel(description = "emphasis，默认只有一个数据")
@Schema(description = "emphasis，默认只有一个数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Emphasis {
	@ApiModelProperty(value = "默认线段series")
	@Schema(description = "默认线段series")
	private String focus;
}
