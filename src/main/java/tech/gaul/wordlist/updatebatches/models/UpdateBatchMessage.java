package tech.gaul.wordlist.updatebatches.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UpdateBatchMessage {
    
    private String batchId;

}
