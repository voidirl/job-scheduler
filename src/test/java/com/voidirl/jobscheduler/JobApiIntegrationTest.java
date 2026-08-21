package com.voidirl.jobscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private long createJob() throws Exception {
        String body = mockMvc.perform(post("/api/jobs")
                        .param("jobName", "test-job")
                        .param("scheduledTime", "2030-01-01T10:00:00"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(com.jayway.jsonpath.JsonPath.read(body, "$.id").toString());
    }

    @Test
    void getJobReturnsCreatedJob() throws Exception {
        long id = createJob();
        mockMvc.perform(get("/api/jobs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.jobName").value("test-job"));
    }

    @Test
    void getMissingJobReturns404() throws Exception {
        mockMvc.perform(get("/api/jobs/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateJobChangesFields() throws Exception {
        long id = createJob();
        mockMvc.perform(put("/api/jobs/" + id)
                        .param("jobName", "renamed-job")
                        .param("scheduledTime", "2031-06-15T08:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("renamed-job"))
                .andExpect(jsonPath("$.scheduledTime").value("2031-06-15T08:30:00"));
    }

    @Test
    void updateMissingJobReturns404() throws Exception {
        mockMvc.perform(put("/api/jobs/999999")
                        .param("jobName", "x")
                        .param("scheduledTime", "2030-01-01T10:00:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJobRemovesIt() throws Exception {
        long id = createJob();
        mockMvc.perform(delete("/api/jobs/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/jobs/" + id))
                .andExpect(status().isNotFound());
    }
}
