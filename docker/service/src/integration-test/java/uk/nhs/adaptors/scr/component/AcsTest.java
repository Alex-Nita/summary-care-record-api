package uk.nhs.adaptors.scr.component;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.adaptors.scr.IntegrationTestsExtension;
import uk.nhs.adaptors.scr.utils.SpineRequest;
import uk.nhs.adaptors.scr.utils.spine.mock.SpineMockSetupEndpoint;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class, IntegrationTestsExtension.class})
@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
public class AcsTest {
    private static final String ACS_SET_RESOURCES_ENDPOINT = "/summary-care-record/consent";
    private static final String ACS_SPINE_ENDPOINT = "/acs";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpineMockSetupEndpoint spineMockSetupEndpoint;

    @Value("classpath:acs.consent.json")
    private Resource acsSetResourceRequest;

    @Value("${spine.url}")
    private String spineUrl;

    @Test
    public void whenPostingAcsSetResourceThenExpect200() throws Exception {
        spineMockSetupEndpoint
            .onMockServer(spineUrl)
            .forPath(ACS_SPINE_ENDPOINT)
            .forHttpMethod("POST")
            .withHttpStatusCode(OK.value())
            .withResponseContent("response");

        String requestBody = readString(acsSetResourceRequest.getFile().toPath(), UTF_8);
        mockMvc.perform(
            post(ACS_SET_RESOURCES_ENDPOINT)
                .contentType(APPLICATION_JSON_VALUE)
                .content(requestBody))
            .andExpect(status().isOk());

        SpineRequest latestRequest = spineMockSetupEndpoint.getLatestRequest();
        assertThat(latestRequest.getHttpMethod()).isEqualTo(POST.toString());
        assertThat(latestRequest.getUrl()).isEqualTo(ACS_SPINE_ENDPOINT);
    }
}