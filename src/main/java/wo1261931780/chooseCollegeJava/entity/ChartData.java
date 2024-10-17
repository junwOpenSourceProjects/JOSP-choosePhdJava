package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
 * @Date 2024-10-14-30  星期四
 * @Description
 */
@ApiModel(description = "折线图渲染的数据")
@Schema(description = "折线图渲染的数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartData {
	@ApiModelProperty(value = "数据体")
	@Schema(description = "数据体")
	private List<Series> series;
}
