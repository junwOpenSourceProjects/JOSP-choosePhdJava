package wo1261931780.chooseCollegeJava.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wo1261931780.chooseCollegeJava.entity.UniversityConsider;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsEcharts;

/**
 * Created by Intellij IDEA.
 * Project:chooseCollegeJava
 * Package:wo1261931780.chooseCollegeJava.dto
 *
 * @author liujiajun_junw
 * @Date 2024-10-13-38  星期六
 * @Description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingStatusDTO extends UniversityRankingsEcharts {

	/**
	 * QS排名强弱，0：弱，1：中等，2：强
	 */
	@TableField(value = "status_qs")
	@ApiModelProperty(value = "QS排名强弱，0：弱，1：中等，2：强")
	@Schema(description = "QS排名强弱，0：弱，1：中等，2：强")
	private Byte statusQs;

	/**
	 * QS计算机排名强弱，0：弱，1：中等，2：强
	 */
	@TableField(value = "status_qs_cs")
	@ApiModelProperty(value = "QS计算机排名强弱，0：弱，1：中等，2：强")
	@Schema(description = "QS计算机排名强弱，0：弱，1：中等，2：强")
	private Byte statusQsCs;

	/**
	 * US News排名强弱，0：弱，1：中等，2：强
	 */
	@TableField(value = "status_usnews")
	@ApiModelProperty(value = "US News排名强弱，0：弱，1：中等，2：强")
	@Schema(description = "US News排名强弱，0：弱，1：中等，2：强")
	private Byte statusUsnews;

	/**
	 * US News计算机排名强弱，0：弱，1：中等，2：强
	 */
	@TableField(value = "status_usnews_cs")
	@ApiModelProperty(value = "US News计算机排名强弱，0：弱，1：中等，2：强")
	@Schema(description = "US News计算机排名强弱，0：弱，1：中等，2：强")
	private Byte statusUsnewsCs;

	/**
	 * 整体排名强弱，0：弱，1：中等，2：强
	 */
	@TableField(value = "status_total")
	@ApiModelProperty(value = "整体排名强弱，0：弱，1：中等，2：强")
	@Schema(description = "整体排名强弱，0：弱，1：中等，2：强")
	private Byte statusTotal;

	/**
	 * 是否考虑，0：不考虑，1：考虑
	 */
	@TableField(value = "consider")
	@ApiModelProperty(value = "是否考虑，0：不考虑，1：考虑")
	@Schema(description = "是否考虑，0：不考虑，1：考虑")
	private Byte consider;

}
