package dev.andrylat.testci;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class TestCiApplicationTests {

    @Test
    void contextLoads() throws JsonProcessingException {
        String githubUsername = System.getenv("GITHUB_USERNAME");
        String githubToken = System.getenv("GITHUB_TOKEN");
        String currentRepository = System.getenv("REPOSITORY");
        String commitSha = System.getenv("COMMIT_SHA");
        String prSha = System.getenv("PR_COMMIT_SHA");
        //String externalRepository = System.getenv("EXTERNAL_REPOSITORY");
        //String externalSha = System.getenv("EXTERNAL_REPOSITORY_SHA");

        String sha = commitSha;
        if(prSha != null && !prSha.isEmpty()) {
            sha = prSha;
        }

        String repository = currentRepository;

        System.out.println("LOGGGG: sha = " + sha + " | repository = " + repository);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders(githubUsername, githubToken);
        System.out.println("LOGGGG: headers = " + headers);

        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(prepareRequest(sha));
        HttpEntity<String> request =
                new HttpEntity<String>(body, headers);

        System.out.println("LOGGGG: request = " + request);

        try {
            String personResultAsJsonStr =
                    restTemplate.postForObject("https://api.github.com/repos/" + repository + "/check-runs", request, String.class);
            System.out.println("LOGGGG: personResultAsJsonStr = " + personResultAsJsonStr);
        } catch (Exception ex) {
            System.err.println("LOGGG: exception = " + ex);
        }

        if(new Random().nextInt(100) > 90) {
            fail();
        }
    }

    static HttpHeaders createHeaders(String username, String password){
        return new HttpHeaders() {{

            set( "Authorization", "Bearer " + password );
            set("accept", "application/vnd.github.v3+json");
            setContentType(MediaType.APPLICATION_JSON);
        }};
    }

    private static GithubCheckRunRequest prepareRequest(String sha) {
        String commitHash = sha;
        GithubCheckRunRequest requestData = new GithubCheckRunRequest("Test Reoirt", commitHash, "PUBLIC_REPORT_TITLE");

        StringBuilder output = new StringBuilder();
        output.append("CHECK_REPORT_HEADER_1").append("REPORT_END_LINE");


        requestData.getOutput().setSummary(output.toString());

        return requestData;
    }

    static class GithubCheckRunRequest {
        private static final String SUCCESS_CONCLUSION = "success";
        private static final String FAILURE_CONCLUSION = "failure";
        private static final String NEUTRAL_CONCLUSION = "neutral";

        private final String name;
        private final String commitSha;
        private String conclusion;
        private final String completedAt;
        private final CheckRunOutput output;

        public GithubCheckRunRequest(String name, String commitSha, String title) {
            this.name = name;
            this.commitSha = commitSha;
            this.conclusion = SUCCESS_CONCLUSION;
            this.completedAt = getNow();
            this.output = new CheckRunOutput(title);
        }

        public String getName() {
            return name;
        }

        @JsonProperty("head_sha")
        public String getCommitSha() {
            return commitSha;
        }

        public String getConclusion() {
            return conclusion;
        }

        public void setFailureConclusion() {
            this.conclusion = FAILURE_CONCLUSION;
        }

        @JsonProperty("completed")
        public String getCompletedAt() {

            return completedAt;
        }

        public CheckRunOutput getOutput() {
            return output;
        }

        public void updateConclusionByPercentage(int successfulPercentage) {
            this.conclusion = calculateCheckConclusion(successfulPercentage);
        }

        private String getNow() {
            TimeZone timeZone = TimeZone.getTimeZone("UTC");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            dateFormat.setTimeZone(timeZone);

            return dateFormat.format(new Date());
        }

        private static String calculateCheckConclusion(int successfulPercentage) {
            if (successfulPercentage >= 90) {
                return GithubCheckRunRequest.SUCCESS_CONCLUSION;
            }
            if (successfulPercentage >= 50) {
                return GithubCheckRunRequest.NEUTRAL_CONCLUSION;
            }

            return GithubCheckRunRequest.FAILURE_CONCLUSION;
        }

        public static class CheckRunOutput {
            private final String title;
            private String summary;

            public CheckRunOutput(String title) {
                this.title = title;
            }

            public String getTitle() {
                return title;
            }

            public String getSummary() {
                return summary;
            }

            public void setSummary(String summary) {
                this.summary = summary;
            }
        }
    }

}
