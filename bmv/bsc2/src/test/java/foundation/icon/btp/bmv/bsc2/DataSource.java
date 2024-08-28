package foundation.icon.btp.bmv.bsc2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.score.util.StringUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
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
            return mapper.readValue(
                    DataSource.class.getClassLoader().getResourceAsStream(filename),
                    mapper.getTypeFactory().constructCollectionType(List.class, DataSource.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Case> loadCases(String filename) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            InputStream is = DataSource.class.getClassLoader().getResourceAsStream(filename);
            return mapper.readValue(is, new TypeReference<>() {});

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Case> cases;

    @ToString
    @Getter
    @NoArgsConstructor
    public static class Case {
        private String description;
        private Deployment deployment;
        private List<Phase> phases;

        @ToString
        @Getter
        @NoArgsConstructor
        public static class Deployment {
            private String header;
            private BigInteger chainId;
            private String validators;
            private String candidates;
            private String recents;
            private Integer currTurnLength;
            private Integer nextTurnLength;

            public byte[] getHeader() {
                return StringUtil.hexToBytes(header);
            }
            public byte[] getValidators() {
                return StringUtil.hexToBytes(validators);
            }
            public byte[] getCandidates() {
                return StringUtil.hexToBytes(candidates);
            }
            public byte[] getRecents() {
                return StringUtil.hexToBytes(recents);
            }

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
        public static class Phase {
            private String description;
            private String message;
            private List<String> result;
            private BMVStatus status;

            public byte[] getMessage() {
                return StringUtil.hexToBytes(message);
            }
        }
    }
}
