package wo1261931780.chooseCollegeJava.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 折线图渲染的数据
 */
@Schema(description = "折线图渲染的数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartData implements Serializable {

	private static final long serialVersionUID = 1L;
	@Schema(description = "数据体")
	private List<Series> series;
}