package wo1261931780.chooseCollegeJava.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wo1261931780.chooseCollegeJava.entity.ChartData;

import java.util.List;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.dto
 *
 * @author liujiajun_junw
 * @Date 2024-10-06-56  星期五
 * @Description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EchartsDTO {
	private ChartData chatData;
	private List<String> legendData;
}
