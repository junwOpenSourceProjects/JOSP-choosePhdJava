package wo1261931780.chooseCollegeJava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wo1261931780.chooseCollegeJava.entity.LoginUser;

import java.util.List;

/**
 * LoginUser数据访问层
 */
@Mapper
public interface LoginUserMapper extends BaseMapper<LoginUser> {

}