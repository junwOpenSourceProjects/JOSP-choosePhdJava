package wo1261931780.chooseCollegeJava.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wo1261931780.chooseCollegeJava.entity.ChartData;

import java.util.List;

/**
 * Echarts数据传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EchartsDTO {
	private ChartData chatData;
	private List<String> legendData;
}