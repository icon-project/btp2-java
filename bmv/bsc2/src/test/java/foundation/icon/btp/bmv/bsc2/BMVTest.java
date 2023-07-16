package foundation.icon.btp.bmv.bsc2;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BMVTest extends TestBase {
    static final ServiceManager sm = getServiceManager();
    static final Account BMC = sm.createAccount(Integer.MAX_VALUE);
    static final BTPAddress BMC_BTP_ADDR = BTPAddress.parse("btp://0x1.icon/cx123");

    public static Score deployBmv(DataSource.Case.Deployment deployment) throws Exception {
        return sm.deploy(sm.createAccount(), BTPMessageVerifier.class,
                BMC.getAddress(), deployment.getChainId(), deployment.getHeader(),
                deployment.getValidators(), deployment.getCandidates(), deployment.getRecents()
        );
    }

    public static void handleRelayMessageTest(DataSource.Case c, Score bmv, String prev) {
        System.out.println("case: " + c.getDescription());
        for (DataSource.Case.Phase p : c.getPhases()) {
            System.out.println("phase: " + p.getDescription());
            byte[][] ret = (byte[][]) sm.call(BMC, BigInteger.ZERO, bmv.getAddress(), "handleRelayMessage",
                    BMC_BTP_ADDR.toString(), prev, BigInteger.valueOf(0), p.getMessage());

            if (p.getResult().size() > 0) {
                assertEquals(p.getResult().size(), ret.length);
                for (int i=0; i<p.getResult().size(); i++) {
                    assertEquals(p.getResult().get(i), new String(ret[i]));
                }
            }
            BMVStatus status = bmv.call(BMVStatus.class, "getStatus");
            if (p.getStatus().getHeight() != 0) {
                assertEquals(p.getStatus().getHeight(), status.getHeight());
            }
        }
    }

    public static class TestnetBMVTest {
        private static final List<DataSource.Case> cases = DataSource.loadCases("testnet.json");
        @TestFactory
        public Collection<DynamicTest> handleRelayMessageTests() {
            List<DynamicTest> t = new ArrayList<>();
            for (DataSource.Case c : cases) {
                t.add(DynamicTest.dynamicTest(c.getDescription(),
                        () -> {
                            Score bmv = deployBmv(c.getDeployment());
                            handleRelayMessageTest(c, bmv, "");
                        }
                ));
            }
            return t;
        }
    }
}
