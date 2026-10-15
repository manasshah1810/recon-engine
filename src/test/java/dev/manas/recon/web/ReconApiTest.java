package dev.manas.recon.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReconApiTest {
    @Autowired
    MockMvc mvc;

    private static MockMultipartFile csv(String name, String body) {
        return new MockMultipartFile(name, name + ".csv", "text/csv", body.getBytes());
    }

    private static final String H = "reference,amount,currency,date\n";

    private long createRun() throws Exception {
        var a = csv("fileA", H + "T1,100.00,USD,2026-01-01\nT2,50.00,USD,2026-01-01\nT3,70.00,USD,2026-01-01\n");
        var b = csv("fileB", H + "T1,100.00,USD,2026-01-01\nT2,50.02,USD,2026-01-02\nT9,5.00,USD,2026-01-01\n");
        var res = mvc.perform(multipart("/api/runs").file(a).file(b)
                        .param("amountTolerance", "0.05").param("dateToleranceDays", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exactMatches").value(1))
                .andExpect(jsonPath("$.toleranceMatches").value(1))
                .andExpect(jsonPath("$.unmatchedA").value(1))
                .andExpect(jsonPath("$.unmatchedB").value(1))
                .andReturn();
        return ((Number) JsonPath.read(res.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void runProducesSummaryAndBreaks() throws Exception {
        long id = createRun();
        mvc.perform(get("/api/runs/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.totalA").value(3));
        mvc.perform(get("/api/runs/" + id + "/breaks")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/runs/" + id + "/breaks").param("type", "UNMATCHED_A"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reference").value("T3"));
    }

    @Test
    void breakCanBeResolvedAndFiltered() throws Exception {
        long id = createRun();
        var list = mvc.perform(get("/api/runs/" + id + "/breaks").param("type", "UNMATCHED_B")).andReturn();
        long breakId = ((Number) JsonPath.read(list.getResponse().getContentAsString(), "$[0].id")).longValue();
        mvc.perform(patch("/api/breaks/" + breakId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"note\":\"booked late\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.note").value("booked late"));
        mvc.perform(get("/api/runs/" + id + "/breaks").param("status", "OPEN")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/runs/" + id + "/breaks").param("status", "RESOLVED")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unknownIdsReturn404() throws Exception {
        mvc.perform(get("/api/runs/999999")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/breaks/999999").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void badCsvReturns400WithMessage() throws Exception {
        var a = csv("fileA", "wrong,header\n");
        var b = csv("fileB", H);
        mvc.perform(multipart("/api/runs").file(a).file(b))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void invalidPatchBodyReturns400() throws Exception {
        long id = createRun();
        var list = mvc.perform(get("/api/runs/" + id + "/breaks")).andReturn();
        long breakId = ((Number) JsonPath.read(list.getResponse().getContentAsString(), "$[0].id")).longValue();
        mvc.perform(patch("/api/breaks/" + breakId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
