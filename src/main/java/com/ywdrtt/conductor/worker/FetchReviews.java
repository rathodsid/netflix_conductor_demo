package com.ywdrtt.conductor.worker;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.Review;
import com.google.api.services.androidpublisher.model.ReviewsListResponse;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FetchReviews implements Worker {

    private static final Logger logger = LoggerFactory.getLogger(FetchReviews.class);
    private final String taskDefName;
    private final String serviceAccountFile;
    private final List<String> scopes;
    private final int maxResults;

    public FetchReviews(String taskDefName, String serviceAccountFile, List<String> scopes, int maxResults) {
        this.taskDefName = taskDefName;
        this.serviceAccountFile = serviceAccountFile;
        this.scopes = scopes;
        this.maxResults = maxResults;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        
        try {
            AndroidPublisher service = getService();
            String packageName = (String) task.getInputData().get("packageName");
            List<Review> reviews = fetchReviews(service, packageName);
            
            result.addOutputData("reviews", reviews);
            result.setStatus(TaskResult.Status.COMPLETED);
        } catch (Exception e) {
            logger.error("An error occurred while fetching reviews: ", e);
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion(e.getMessage());
        }
        
        return result;
    }

    private AndroidPublisher getService() throws Exception {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(serviceAccountFile))
                .createScoped(scopes);

        return new AndroidPublisher.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("FetchReviews")
                .build();
    }

    private List<Review> fetchReviews(AndroidPublisher service, String packageName) throws Exception {
        List<Review> reviews = new ArrayList<>();

        try {
            logger.info("Fetching reviews for package {}", packageName);
            AndroidPublisher.Reviews.List request = service.reviews().list(packageName)
                    .setMaxResults((long) maxResults);

            ReviewsListResponse response = request.execute();
            logger.info("Reviews fetched successfully");

            if (response.getReviews() != null) {
                reviews.addAll(response.getReviews());
            }
            logger.info("Reviews extracted successfully");
        } catch (Exception e) {
            logger.error("An error occurred while fetching reviews from list reviews api: ", e);
            throw e;
        }

        return reviews;
    }
}