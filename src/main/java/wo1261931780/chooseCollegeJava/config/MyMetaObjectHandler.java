package wo1261931780.chooseCollegeJava.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * MyBatis-Plus 自动填充处理器
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

	/**
	 * 插入时自动填充创建时间和更新时间
	 *
	 * @param metaObject 元对象
	 */
	@Override
	public void insertFill(MetaObject metaObject) {
		fillDateOrLocalDateTime(metaObject, "createTime");
		fillDateOrLocalDateTime(metaObject, "updateTime");
	}

	/**
	 * 更新时自动填充更新时间
	 *
	 * @param metaObject 元对象
	 */
	@Override
	public void updateFill(MetaObject metaObject) {
		fillDateOrLocalDateTime(metaObject, "updateTime");
	}

	/**
	 * 兼容 Date 和 LocalDateTime 字段类型填充
	 *
	 * @param metaObject 元对象
	 * @param fieldName  字段名
	 */
	private void fillDateOrLocalDateTime(MetaObject metaObject, String fieldName) {
		Object value = metaObject.getValue(fieldName);
		if (value == null) {
			return;
		}
		if (value instanceof Date) {
			this.strictInsertFill(metaObject, fieldName, Date.class, new Date());
		} else if (value instanceof LocalDateTime) {
			this.strictInsertFill(metaObject, fieldName, LocalDateTime.class, LocalDateTime.now());
		}
	}
}