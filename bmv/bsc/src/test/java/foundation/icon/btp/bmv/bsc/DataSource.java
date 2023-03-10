package foundation.icon.btp.bmv.bsc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.icon.btp.lib.BMVStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.IOException;
import java.math.BigInteger;
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
        private String header;
        private BigInteger epoch;
        private BigInteger chainId;
        private List<EthAddress> recents;
        private List<EthAddress> validators;

        public static byte[][] toBytesArray(List<EthAddress> o) {
            byte[][] ret = new byte[o.size()][];
            for (int i = 0; i < o.size(); i++) {
                ret[i] = o.get(i).getEthAddress();
            }
            return ret;
        }
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
