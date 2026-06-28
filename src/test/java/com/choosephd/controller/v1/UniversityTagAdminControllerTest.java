package com.choosephd.controller.v1;

import com.choosephd.common.BusinessException;
import com.choosephd.dto.UniversityTagRequest;
import com.choosephd.entity.UniversityTag;
import com.choosephd.service.UniversityTagService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UniversityTagAdminController 单元测试 — standaloneSetup() 模式.
 *
 * <p>注: requireAdmin 内部读 request.getAttribute("role"), 测里手动 mock 注入.
 * 不走 WebConfig → AdminInterceptor 链路 (那部分在 AdminInterceptorTest 覆盖).
 *
 * <p>6 端点覆盖:
 * <ol>
 *   <li>GET / (listAllTags) - 全部含 active=0</li>
 *   <li>POST / (createTag)</li>
 *   <li>PUT /{id} (updateTag)</li>
 *   <li>DELETE /{id} (deleteTag)</li>
 *   <li>GET /{id}/universities (listTagUniversities)</li>
 *   <li>PUT /universities/{urlId} (setUniversityTags)</li>
 * </ol>
 */
class UniversityTagAdminControllerTest {

    private UniversityTagService universityTagService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        universityTagService = mock(UniversityTagService.class);
        UniversityTagAdminController controller = new UniversityTagAdminController(universityTagService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private UniversityTag newTag(Integer id, String nameZh, Integer active) {
        UniversityTag t = new UniversityTag();
        t.setId(id);
        t.setNameZh(nameZh);
        t.setActive(active);
        return t;
    }

    private UniversityTagRequest newTagRequest(String nameZh, String color, Integer active) {
        UniversityTagRequest req = new UniversityTagRequest();
        req.setNameZh(nameZh);
        req.setColor(color);
        req.setActive(active);
        return req;
    }

    // ===== 1. listTags (GET /) =====
    @Test
    void listTags_admin_returnsAllIncludingDisabled() throws Exception {
        when(universityTagService.listAllTags()).thenReturn(List.of(
            newTag(1, "常春藤", 1),
            newTag(2, "退市", 0)
        ));

        mockMvc.perform(get("/api/v1/admin/university-tags").requestAttr("role", "ROLE_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].nameZh").value("常春藤"))
            .andExpect(jsonPath("$.data[1].active").value(0));

        verify(universityTagService).listAllTags();
    }

    @Test
    void listTags_nonAdmin_throwsBusinessException() {
        UniversityTagAdminController controller = new UniversityTagAdminController(universityTagService);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("role", "ROLE_USER");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> controller.listTags(req));
        assertEquals("无权限", ex.getMessage());
        verifyNoInteractions(universityTagService);
    }

    // ===== 2. createTag (POST /) =====
    @Test
    void createTag_admin_returnsCreated() throws Exception {
        UniversityTagRequest req = newTagRequest("G5", "#10b981", 1);
        UniversityTag created = newTag(99, "G5", 1);
        when(universityTagService.createTag(any(UniversityTagRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/university-tags")
                .contentType(APPLICATION_JSON)
                .content("{\"nameZh\":\"G5\",\"color\":\"#10b981\",\"active\":1}")
                .requestAttr("role", "ROLE_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.nameZh").value("G5"));

        verify(universityTagService).createTag(any(UniversityTagRequest.class));
    }

    // ===== 3. updateTag (PUT /{id}) =====
    @Test
    void updateTag_admin_returnsUpdated() throws Exception {
        UniversityTag updated = newTag(7, "G5-updated", 0);
        when(universityTagService.updateTag(eq(7), any(UniversityTagRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/university-tags/{id}", 7)
                .contentType(APPLICATION_JSON)
                .content("{\"nameZh\":\"G5-updated\",\"color\":\"#10b981\",\"active\":0}")
                .requestAttr("role", "ROLE_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.nameZh").value("G5-updated"))
            .andExpect(jsonPath("$.data.active").value(0));

        verify(universityTagService).updateTag(eq(7), any(UniversityTagRequest.class));
    }

    // ===== 4. deleteTag (DELETE /{id}) =====
    @Test
    void deleteTag_admin_callsService() throws Exception {
        doNothing().when(universityTagService).deleteTag(123);

        mockMvc.perform(delete("/api/v1/admin/university-tags/{id}", 123)
                .requestAttr("role", "ROLE_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(universityTagService).deleteTag(123);
    }

    // ===== 5. listTagUniversities (GET /{id}/universities) =====
    @Test
    void listTagUniversities_admin_returnsUrlIdSet() throws Exception {
        when(universityTagService.listUniversityIdsByTag(5))
            .thenReturn(Set.of("mit", "stanford", "harvard"));

        mockMvc.perform(get("/api/v1/admin/university-tags/{id}/universities", 5)
                .requestAttr("role", "ROLE_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data", containsInAnyOrder("mit", "stanford", "harvard")));

        verify(universityTagService).listUniversityIdsByTag(5);
    }

    // ===== 6. setUniversityTags (PUT /universities/{urlId}) =====
    @Test
    void setUniversityTags_admin_replacesTagList() throws Exception {
        doNothing().when(universityTagService).setUniversityTags(eq("mit"), anyList());

        mockMvc.perform(put("/api/v1/admin/university-tags/universities/{urlId}", "mit")
                .contentType(APPLICATION_JSON)
                .content("[1, 2, 3]")
                .requestAttr("role", "ROLE_ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(universityTagService).setUniversityTags(eq("mit"), eq(List.of(1, 2, 3)));
    }

    // ===== 反模式: 非 admin 角色调用任何 endpoint 都 throw BusinessException =====
    @Test
    void anyEndpoint_nonAdmin_throwsBusinessException() {
        UniversityTagAdminController controller = new UniversityTagAdminController(universityTagService);
        MockHttpServletRequest req = new MockHttpServletRequest();
        // 不设 role attr (模拟未登录穿透)

        assertThrows(BusinessException.class, () -> controller.listTags(req));
        assertThrows(BusinessException.class,
            () -> controller.createTag(newTagRequest("x", "#fff", 1), req));
        assertThrows(BusinessException.class,
            () -> controller.updateTag(1, newTagRequest("x", "#fff", 1), req));
        assertThrows(BusinessException.class, () -> controller.deleteTag(1, req));
        assertThrows(BusinessException.class, () -> controller.listTagUniversities(1, req));
        assertThrows(BusinessException.class, () -> controller.setUniversityTags("mit", List.of(), req));

        verifyNoInteractions(universityTagService);
    }
}
