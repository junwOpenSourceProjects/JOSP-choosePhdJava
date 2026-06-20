package wo1261931780.chooseCollegeJava.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import wo1261931780.chooseCollegeJava.config.RoleConstants;
import wo1261931780.chooseCollegeJava.config.RoleInterceptor;
import wo1261931780.chooseCollegeJava.entity.LoginUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 角色权限拦截测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class RbacAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession adminSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        LoginUser user = new LoginUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(RoleConstants.ROLE_ADMIN);
        session.setAttribute(RoleInterceptor.CURRENT_USER, user);
        return session;
    }

    private MockHttpSession userSession() {
        MockHttpSession session = new MockHttpSession();
        LoginUser user = new LoginUser();
        user.setId(2L);
        user.setUsername("testUser");
        user.setRole(RoleConstants.ROLE_USER);
        session.setAttribute(RoleInterceptor.CURRENT_USER, user);
        return session;
    }

    @Test
    void adminShouldAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/query/updateEchartsData")
                        .session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series").exists());
    }

    @Test
    void userShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/query/updateEchartsData")
                        .session(userSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/query/updateEchartsData"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void loginShouldCreateSessionAndInfoReturnRole() throws Exception {
        MvcResult result = mockMvc.perform(post("/vue-element-admin/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        mockMvc.perform(get("/vue-element-admin/user/info")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.roles[0]").value(RoleConstants.ROLE_ADMIN))
                .andExpect(jsonPath("$.data.name").value("admin"));
    }
}
