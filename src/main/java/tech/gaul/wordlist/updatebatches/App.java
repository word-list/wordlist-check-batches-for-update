package tech.gaul.wordlist.updatebatches;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import tech.gaul.wordlist.queryword.models.QueryWordModel;
import tech.gaul.wordlist.queryword.models.Word;

public class App implements RequestHandler<SQSEvent, Object> {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object handleRequest(SQSEvent event, Context context) {

        // Get active queries.
        DynamoDbEnhancedClient dbClient = DependencyFactory.dynamoDbClient();        
    }
}