package wo1261931780.chooseCollegeJava.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * emphasis，默认只有一个数据
 */
@Schema(description = "emphasis，默认只有一个数据")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Emphasis implements Serializable {

	private static final long serialVersionUID = 1L;
	@Schema(description = "默认线段series")
	private String focus;
}