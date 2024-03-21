package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final HttpClient httpClient;
    private final Gson gson;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
        this.semaphore = new Semaphore(requestLimit);

        // Convert time unit to duration
        Duration duration = switch (timeUnit) {
            case SECONDS -> Duration.ofSeconds(1);
            case MINUTES -> Duration.ofMinutes(1);
            case HOURS -> Duration.ofHours(1);
            default -> throw new IllegalArgumentException("Unsupported time unit");
        };

        // Reset semaphore permits periodically
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(duration.toMillis());
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();
            String requestBody = gson.toJson(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (InterruptedException | java.io.IOException e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    // Inner class representing document structure
    static class Document {
        private String participantInn;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        // Define other fields and constructors, getters, setters as needed

        public Document(String participantInn, String docId, String docStatus, String docType, boolean importRequest,
                        String ownerInn, String producerInn, String productionDate,
                        String productionType) {
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
        }

        // getters and setters
    }

    // Example of usage
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);
        Document document = new Document("participantInnValue", "docIdValue", "docStatusValue", "docTypeValue", true,
                "ownerInnValue", "producerInnValue", "2020-01-23", "productionTypeValue");
        String signature = "test";
        crptApi.createDocument(document, signature);
    }
}
