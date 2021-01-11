package uk.nhs.adaptors.scr.uat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.nhs.adaptors.scr.WireMockInitializer;
import uk.nhs.adaptors.scr.consts.ScrHttpHeaders;
import uk.nhs.adaptors.scr.uat.common.CustomArgumentsProvider;
import uk.nhs.adaptors.scr.uat.common.TestData;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.adaptors.scr.consts.SpineHttpHeaders.SOAP_ACTION;
import static uk.nhs.adaptors.scr.controllers.FhirMediaTypes.APPLICATION_FHIR_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SpringExtension.class})
@DirtiesContext
@Slf4j
@ContextConfiguration(initializers = {WireMockInitializer.class})
public class SetAcsUAT {
    private static final String SPINE_ACS_ENDPOINT = "/sync-service";
    private static final String ACS_QUERY_HEADER = "urn:nhs:names:services:lrs/SET_RESOURCE_PERMISSIONS_INUK01";
    private static final String BEARER_TOKEN = "Bearer BvkLiUYgXoLD50aVuuq62swUG0ha";
    private static final String USER_INFO_ENDPOINT = "/oauth2/userinfo";
    private static final String ACS_ENDPOINT = "/$setPermission";
    private static final String NHSD_ASID = "546375434";
    private static final String CLIENT_IP = "192.168.0.24";
    private static final String NHSD_SESSION_URID = "555254240100";

    @Value("classpath:responses/acs/success.xml")
    private Resource acsSuccessResponse;

    @Value("classpath:responses/acs/invalidNhsNumber.xml")
    private Resource acsErrorResponse;

    @Value("classpath:responses/identity-service/userInfo.json")
    private Resource userInfoResponse;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WireMockServer wireMockServer;

    @ParameterizedTest(name = "[{index}] - {0}")
    @ArgumentsSource(CustomArgumentsProvider.SetAcsSuccess.class)
    public void testSetAcsPermission(String category, TestData testData) throws Exception {
        stubSpineAcsEndpoint(acsSuccessResponse);
        stubIdentityService(userInfoResponse);

        performRequest(testData)
            .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "[{index}] - {0}")
    @ArgumentsSource(CustomArgumentsProvider.SetAcsInvalidNhsNumber.class)
    public void testSetAcsPermissionInvalidNhs(String category, TestData testData) throws Exception {
        stubSpineAcsEndpoint(acsErrorResponse);
        stubIdentityService(userInfoResponse);

        performRequest(testData)
            .andExpect(status().isBadRequest())
            .andExpect(content().json(testData.getFhirResponse()));
    }

    private ResultActions performRequest(TestData testData) throws Exception {
        return mockMvc.perform(post(ACS_ENDPOINT)
            .contentType(APPLICATION_FHIR_JSON)
            .header(ScrHttpHeaders.NHSD_ASID, NHSD_ASID)
            .header(ScrHttpHeaders.CLIENT_IP, CLIENT_IP)
            .header(ScrHttpHeaders.NHSD_SESSION_URID, NHSD_SESSION_URID)
            .header(AUTHORIZATION, BEARER_TOKEN)
            .header(ScrHttpHeaders.CLIENT_REQUEST_URL, wireMockServer.baseUrl())
            .content(testData.getFhirRequest()));

    }

    private void stubSpineAcsEndpoint(Resource response) throws IOException {
        wireMockServer.stubFor(
            WireMock.post(SPINE_ACS_ENDPOINT)
                .withHeader(SOAP_ACTION, equalTo(ACS_QUERY_HEADER))
                .withHeader(CONTENT_TYPE, equalTo(TEXT_XML_VALUE))
                .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withBody(readString(response.getFile().toPath(), UTF_8))));
    }

    private void stubIdentityService(Resource response) throws IOException {
        wireMockServer.stubFor(
            WireMock.get(USER_INFO_ENDPOINT)
                .withHeader(AUTHORIZATION, equalTo(BEARER_TOKEN))
                .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withBody(readString(response.getFile().toPath(), UTF_8))));
    }
}
