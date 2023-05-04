package foundation.icon.btp.bmv.eth2;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import score.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BTPMessageVerifierUnitTest extends TestBase {
    static final ServiceManager sm = getServiceManager();

    public static Score deployBmv(DataSource.ConstructorParams params) throws Exception {
        return sm.deploy(sm.createAccount(), BTPMessageVerifier.class,
                params.getSrcNetworkID(),
                StringUtil.hexToBytes(params.getGenesisValidatorsHash()),
                StringUtil.hexToBytes(params.getSyncCommittee()),
                Address.fromString(params.getBmc()),
                StringUtil.hexToBytes(params.getFinalizedHeader()),
                BigInteger.ZERO
        );
    }

    public static void handleRelayMessageTest(DataSource.Case c, Score bmv, String net, String bmc, String prev) {
        BigInteger seq = BigInteger.ZERO;
        var bmcAccount = sm.getAccount(Address.fromString(bmc));
        BTPAddress bmcBtpAddress = new BTPAddress(net, bmc);
        for (DataSource.Case.Phase p : c.getPhases()) {
            byte[] relayMsg = StringUtil.hexToBytes(p.getInput());
            List<String> messages = p.getMessages();
            byte[][] ret = (byte[][]) sm.call(bmcAccount, BigInteger.ZERO, bmv.getAddress(), "handleRelayMessage",
                    bmcBtpAddress.toString(), prev, seq, relayMsg);

            if (messages.size() > 0) {
                assertEquals(messages.size(), ret.length);
                seq = seq.add(BigInteger.valueOf(ret.length));
            }
            BMVStatus status = bmv.call(BMVStatus.class, "getStatus");
            assertEquals(p.getStatus().getHeight(), status.getHeight());
        }
    }
    public static class Sepolia {
        private static final DataSource sepoliaData = DataSource.loadDataSource("sepolia.json");
        private static final DataSource historicalSummaryData = DataSource.loadDataSource("historicalRoot.json");
        private static final String PREV_BMC = "btp://aa36a7.eth/0xd2f04942ff92709ed9d41988d161710d18d7f1fe";
        private static final String NET = "0x42.icon";
        @TestFactory
        public Collection<DynamicTest> handleRelayMessageTests() {
            DataSource[] dataSources = {sepoliaData, historicalSummaryData};
            List<DynamicTest> t = new ArrayList<>();
            for (DataSource d : dataSources) {
                DataSource.ConstructorParams params = d.getParams();
                for (DataSource.Case c : d.getCases()) {
                    t.add(DynamicTest.dynamicTest(c.getDescription(),
                            () -> {
                                Score bmv = deployBmv(params);
                                handleRelayMessageTest(c, bmv, NET, params.getBmc(), PREV_BMC);
                            }
                    ));
                }
            }
            return t;
        }
    }
}
