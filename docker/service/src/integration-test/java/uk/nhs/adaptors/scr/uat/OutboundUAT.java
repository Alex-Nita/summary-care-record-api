package uk.nhs.adaptors.scr.uat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.scr.IntegrationTestsExtension;
import uk.nhs.adaptors.scr.uat.common.CustomArgumentsProvider;
import uk.nhs.adaptors.scr.uat.common.TestData;
import uk.nhs.adaptors.scr.utils.spine.mock.SpineMockSetupEndpoint;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SpringExtension.class, IntegrationTestsExtension.class})
@DirtiesContext
@Slf4j
public class OutboundUAT {

    private static final String FHIR_ENDPOINT = "/fhir";
    private static final String SPINE_ENDPOINT = "/";

    @Value("${spine.url}")
    private String spineUrl;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpineMockSetupEndpoint spineMockSetupEndpoint;

    @ParameterizedTest(name = "[{index}] - {0}")
    @ArgumentsSource(CustomArgumentsProvider.Outbound.class)
    void testTranslatingFromFhirToHL7v3(String category, TestData testData) throws Exception {
        spineMockSetupEndpoint
            .onMockServer(spineUrl)
            .forPath(SPINE_ENDPOINT)
            .forHttpMethod("POST")
            .withHttpStatusCode(OK.value())
            .withResponseContent("response");

        mockMvc.perform(
            post(FHIR_ENDPOINT)
                .contentType(getContentType(testData.getFhirFormat()))
                .content(testData.getFhir()))
            .andExpect(status().isOk());

        var lastRequest = spineMockSetupEndpoint.getLatestRequest();

        assertThat(lastRequest.getHttpMethod()).isEqualTo(POST.name());
        assertThat(lastRequest.getBody()).isEqualTo(testData.getHl7v3());
    }

    private String getContentType(TestData.FhirFormat fhirFormat) {
        switch (fhirFormat) {
            case JSON:
                return "application/fhir+json";
            case XML:
                return "application/fhir+xml";
            default:
                throw new NotImplementedException();
        }
    }
}
