package wo1261931780.chooseCollegeJava.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import wo1261931780.chooseCollegeJava.dto.UniversityAllDTO;
import wo1261931780.chooseCollegeJava.entity.ChartData;
import wo1261931780.chooseCollegeJava.entity.UniversityRankingsAll;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 查询服务层测试
 */
@SpringBootTest
class AllQueryServiceImplTest {

    @Autowired
    private AllQueryService allQueryService;

    @Test
    void queryAllDataShouldReturnPage() {
        Page<UniversityRankingsAll> result = allQueryService.queryAllData(1, 10, null, null, null, 100);
        assertNotNull(result);
        assertEquals(1, result.getCurrent());
        assertTrue(result.getSize() <= 10);
    }

//    @Test
//    void queryUniversityRankShouldReturnPage() {
//        Page<UniversityAllDTO> result = allQueryService.queryUniversityRank(1, 10, "qs", null, null, 100);
//        assertNotNull(result);
//        assertEquals(1, result.getCurrent());
//    }

    @Test
    void queryAllEchartsDataShouldReturnChartData() {
        ChartData result = allQueryService.queryAllEchartsData(null, null, null, null, "qs");
        assertNotNull(result);
        assertNotNull(result.getSeries());
    }
}
