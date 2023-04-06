package foundation.icon.btp.bmv.eth2;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BTPMessageVerifierUnitTest extends TestBase {
    static final ServiceManager sm = getServiceManager();
    static final Account BMC = sm.createAccount(Integer.MAX_VALUE);
    static final BTPAddress BMC_BTP_ADDRESS = new BTPAddress(BTPIntegrationTest.Faker.btpNetwork(), BMC.getAddress().toString());

    public static Score deployBmv(DataSource.ConstructorParams params) throws Exception {
        return sm.deploy(sm.createAccount(), BTPMessageVerifier.class,
                params.getSrcNetworkID(),
                StringUtil.hexToBytes(params.getGenesisValidatorsHash()),
                StringUtil.hexToBytes(params.getSyncCommittee()),
                BMC.getAddress(),
                StringUtil.hexToBytes(params.getFinalizedHeader()));
    }

    public static void handleRelayMessageTest(DataSource.Case c, Score bmv, String prev) {
        BigInteger seq = BigInteger.ZERO;
        for (DataSource.Case.Phase p : c.getPhases()) {
            byte[] relayMsg = StringUtil.hexToBytes(p.getInput());
            List<String> messages = p.getMessages();
            byte[][] ret = (byte[][]) sm.call(BMC, BigInteger.ZERO, bmv.getAddress(), "handleRelayMessage",
                    BMC_BTP_ADDRESS.toString(), prev, seq, relayMsg);

            if (messages.size() > 0) {
                assertEquals(messages.size(), ret.length);
                for (int i = 0; i < messages.size(); i++)
                    assertEquals(messages.get(i), new String(ret[i]));
                seq = seq.add(BigInteger.valueOf(ret.length));
            }
            BMVStatus status = bmv.call(BMVStatus.class, "getStatus");
            assertEquals(p.getStatus().getHeight(), status.getHeight());
        }
    }
    public static class Sepolia {
        private static final DataSource data = DataSource.loadDataSource("sepolia.json");
        private static final BTPAddress PREV_BMC = BTPAddress.parse("btp://0x1.eth/0xD2F04942FF92709ED9d41988D161710D18d7f1FE");
        @TestFactory
        public Collection<DynamicTest> handleRelayMessageTests() {
            DataSource.ConstructorParams params = data.getParams();
            List<DynamicTest> t = new ArrayList<>();
            for (DataSource.Case c : data.getCases()) {
                t.add(DynamicTest.dynamicTest(c.getDescription(),
                        () -> {
                            Score bmv = deployBmv(params);
//                            handleRelayMessageTest(c, bmv, PREV_BMC.toString());
                        }
                ));
            }
            return t;
        }
    }
}
