package com.ywdrtt.conductor;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.ywdrtt.conductor.worker.FetchReviews;
import com.ywdrtt.conductor.worker.StoreReviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Collection;

@SpringBootApplication
public class ConductorApplication {

    public static void main(String[] args) {
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI("http://localhost:8080/api/"); // Point this to the server API

        int threadCount = 1; // number of threads used to execute workers.  To avoid starvation, should be
        // same or more than number of workers

        Worker worker1 = new FetchReviews("fetch_reviews");
        Worker worker2 = new StoreReviews("store_reviews");
        
        /*// Create TaskRunnerConfigurer
        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, Collections.singletonList(worker1))
                        .withThreadCount(threadCount)
                        .build();*/

        Collection workerArrayList = new ArrayList<Worker>();
        workerArrayList.add(worker1);
        workerArrayList.add(worker2);
        // Create TaskRunnerConfigurer
        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workerArrayList)
                        .withThreadCount(threadCount)
                        .build();
        // Start the polling and execution of tasks
        configurer.init();
        SpringApplication.run(ConductorApplication.class, args);
    }
}
