package wo1261931780.chooseCollegeJava.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 大学排名查询接口测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class QueryAllUniversityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void queryAllWithDefaultParamsShouldReturnPage() throws Exception {
        mockMvc.perform(get("/query/queryAll")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void queryAllWithInvalidPageShouldReturnValidationError() throws Exception {
        mockMvc.perform(get("/query/queryAll")
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void queryQsWithDefaultParamsShouldReturnPage() throws Exception {
        mockMvc.perform(get("/query/queryQs")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(1));
    }
}
