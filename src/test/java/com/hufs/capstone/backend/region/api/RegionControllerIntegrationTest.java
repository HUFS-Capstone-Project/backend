package com.hufs.capstone.backend.region.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class RegionControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser
	void shouldReturnSidosWithVirtualAllOption() throws Exception {
		mockMvc.perform(get("/api/v1/regions/sidos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("ALL"))
				.andExpect(jsonPath("$.data[0].name").value("전체"))
				.andExpect(jsonPath("$.data[0].all").value(true))
				.andExpect(jsonPath("$.data[1].code").value("11"))
				.andExpect(jsonPath("$.data[1].name").value("서울특별시"));
	}

	@Test
	@WithMockUser
	void shouldReturnSigungusWithVirtualAllOption() throws Exception {
		mockMvc.perform(get("/api/v1/regions/sidos/11/sigungus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("ALL"))
				.andExpect(jsonPath("$.data[0].name").value("전체"))
				.andExpect(jsonPath("$.data[0].all").value(true))
				.andExpect(jsonPath("$.data[1].code").value("11680"))
				.andExpect(jsonPath("$.data[1].name").value("강남구"));
	}

	@Test
	@WithMockUser
	void shouldRejectInvalidSidoCode() throws Exception {
		mockMvc.perform(get("/api/v1/regions/sidos/99/sigungus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("E400_VALIDATION"))
				.andExpect(jsonPath("$.detail").value("입력값을 확인해 주세요."))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("sidoCode"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("유효하지 않은 시/도 코드입니다."));
	}
}
