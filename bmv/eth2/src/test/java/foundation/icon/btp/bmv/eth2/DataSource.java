package foundation.icon.btp.bmv.eth2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.icon.btp.lib.BMVStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.IOException;
import java.util.List;

@ToString
@Getter
@NoArgsConstructor
public class DataSource {
    public static DataSource loadDataSource(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(DataSource.class.getClassLoader().getResourceAsStream(filename), DataSource.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private ConstructorParams params;
    private List<Case> cases;

    @ToString
    @Getter
    @NoArgsConstructor
    public static class ConstructorParams {
        private String srcNetworkID;
        private String genesisValidatorsHash;
        private String syncCommittee;
        private String bmc;
        private String ethBmc;
        private String finalizedHeader;
    }

    @ToString
    @Getter
    @NoArgsConstructor
    public static class Case {
        private String description;
        private List<Phase> phases;

        @ToString
        @Getter
        @NoArgsConstructor
        public static class Phase {
            private String description;
            private String input;
            private List<String> messages;
            private BMVStatus status;
        }
    }
}
