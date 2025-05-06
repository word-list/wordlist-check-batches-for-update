package tech.gaul.wordlist.updatebatches;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.batches.Batch;
import com.openai.models.batches.BatchRetrieveParams;
import com.openai.models.batches.Batch.Status;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import tech.gaul.wordlist.updatebatches.models.ActiveBatchRequest;
import tech.gaul.wordlist.updatebatches.models.ActiveWordQuery;
import tech.gaul.wordlist.updatebatches.models.UpdateBatchMessage;

public class App implements RequestHandler<SQSEvent, Object> {

    ObjectMapper objectMapper = new ObjectMapper();
    OpenAIClient openAIClient = DependencyFactory.getOpenAIClient();
    DynamoDbEnhancedClient dbClient = DependencyFactory.dynamoDbClient();

    TableSchema<ActiveBatchRequest> activeBatchRequestSchema = TableSchema.fromBean(ActiveBatchRequest.class);
    TableSchema<ActiveWordQuery> activeWordQuerySchema = TableSchema.fromBean(ActiveWordQuery.class);

    SqsClient sqsClient = DependencyFactory.sqsClient();

    @Override
    public Object handleRequest(SQSEvent event, Context context) {

        List<ActiveBatchRequest> deleteList = Collections.synchronizedList(new LinkedList<ActiveBatchRequest>());

        // Get active batch requests.
        dbClient.table("active-batch-requests", activeBatchRequestSchema)
                .scan(b -> b.filterExpression(Expression.builder()
                        .expression("status = :status")
                        .expressionValues(Map.of(":status", AttributeValue.fromS("Awaiting Response"))).build()))
                .items()
                .stream()
                .map(request -> CompletableFuture.runAsync(() -> {
                    try {
                        BatchRetrieveParams batchRetrieveParams = BatchRetrieveParams.builder()
                                .batchId(request.getBatchRequestId())
                                .build();

                        Batch batch = openAIClient.batches().retrieve(batchRetrieveParams);
                        if (batch == null) {
                            // Batch was not found
                            System.out.println("Batch was not found: " + request.getBatchRequestId());
                            deleteList.add(request);
                            return;
                        }

                        if (batch.status().equals(Status.COMPLETED)) {
                            // Batch is completed
                            System.out.println("Batch is completed: " + request.getBatchRequestId());

                            // Send a message to process this batch.
                            SendMessageRequest.Builder messageBuilder = SendMessageRequest.builder()
                                    .queueUrl(System.getenv("UPDATE_BATCH_QUEUE_URL"))
                                    .messageBody(objectMapper.writeValueAsString(UpdateBatchMessage.builder()
                                            .batchId(request.getBatchRequestId())
                                            .build()));

                            sqsClient.sendMessage(messageBuilder.build());
                        } else if (batch.status().equals(Status.FAILED) || batch.status().equals(Status.CANCELLED)) {
                            System.out.println("Batch failed or cancelled: " + request.getBatchRequestId());
                            deleteList.add(request);
                            return;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })).forEach(task -> {
                    // Wait for all tasks to complete
                    try {
                        task.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        // Delete the active batch requests that are no longer needed.
        for (ActiveBatchRequest request : deleteList) {
            dbClient.table("active-batch-requests", activeBatchRequestSchema).deleteItem(request);
        }

        return null;
    }
}