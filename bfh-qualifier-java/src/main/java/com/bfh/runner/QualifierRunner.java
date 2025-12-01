package com.bfh.runner;

import com.bfh.dto.FinalQueryRequest;
import com.bfh.dto.GenerateWebhookRequest;
import com.bfh.dto.GenerateWebhookResponse;
import com.bfh.entity.SqlSolution;
import com.bfh.repository.SqlSolutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class QualifierRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QualifierRunner.class);

    private final RestTemplate restTemplate;
    private final SqlSolutionRepository sqlSolutionRepository;

    @Value("${candidate.name}")
    private String candidateName;

    @Value("${candidate.regNo}")
    private String candidateRegNo;

    @Value("${candidate.email}")
    private String candidateEmail;

    @Value("${sql.solution.q1}")
    private String solutionQ1;

    @Value("${sql.solution.q2}")
    private String solutionQ2;

    public QualifierRunner(RestTemplate restTemplate,
                           SqlSolutionRepository sqlSolutionRepository) {
        this.restTemplate = restTemplate;
        this.sqlSolutionRepository = sqlSolutionRepository;
    }

    @Override
    public void run(String... args) {
        try {
            // 1. Generate webhook
            GenerateWebhookRequest requestBody = new GenerateWebhookRequest(
                    candidateName,
                    candidateRegNo,
                    candidateEmail
            );

            String generateWebhookUrl =
                    "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            log.info("Calling generateWebhook API...");
            ResponseEntity<GenerateWebhookResponse> responseEntity =
                    restTemplate.postForEntity(
                            generateWebhookUrl,
                            requestBody,
                            GenerateWebhookResponse.class
                    );

            if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                log.error("Failed to generate webhook. Status: {}", responseEntity.getStatusCode());
                return;
            }

            GenerateWebhookResponse webhookResponse = responseEntity.getBody();
            String webhookUrl = webhookResponse.getWebhook();
            String accessToken = webhookResponse.getAccessToken();

            log.info("Received webhookUrl: {}", webhookUrl);
            log.info("Received accessToken: {}", accessToken);

            // 2. Determine question based on last two digits of regNo
            String lastTwoDigitsStr = candidateRegNo.substring(candidateRegNo.length() - 2);
            int lastTwoDigits = Integer.parseInt(lastTwoDigitsStr);

            boolean isOdd = (lastTwoDigits % 2 != 0);
            String questionType = isOdd ? "Q1_ODD" : "Q2_EVEN";
            String finalQuery = isOdd ? solutionQ1 : solutionQ2;

            log.info("RegNo: {}, lastTwoDigits: {}, questionType: {}",
                    candidateRegNo, lastTwoDigits, questionType);
            log.info("Final SQL Query: {}", finalQuery);

            // 3. Store the solution in DB
            SqlSolution solution = new SqlSolution();
            solution.setRegNo(candidateRegNo);
            solution.setQuestionType(questionType);
            solution.setFinalQuery(finalQuery);

            sqlSolutionRepository.save(solution);
            log.info("Solution stored in DB with ID: {}", solution.getId());

            // 4. Submit final query to webhook using JWT token in Authorization header
            FinalQueryRequest finalQueryRequest = new FinalQueryRequest(finalQuery);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            HttpEntity<FinalQueryRequest> httpEntity =
                    new HttpEntity<>(finalQueryRequest, headers);

            log.info("Submitting final query to webhook...");
            ResponseEntity<String> submitResponse = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            log.info("Webhook submission status: {}", submitResponse.getStatusCode());
            log.info("Webhook response body: {}", submitResponse.getBody());

        } catch (Exception e) {
            log.error("Error during qualifier flow", e);
        }
    }
}
