package foundation.icon.btp.bmv.bsc;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.StringUtil;
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

    public static Score deployBmv(DataSource.ConstructorParams params) throws Exception {
        return sm.deploy(sm.createAccount(), BTPMessageVerifier.class,
                BMC.getAddress(), params.getChainId(), StringUtil.hexToBytes(params.getHeader()),
                DataSource.ConstructorParams.toBytesArray(params.getRecents()),
                DataSource.ConstructorParams.toBytesArray(params.getValidators()));
    }

    public static void handleRelayMessageTest(DataSource.Case c, Score bmv, String prev) {
        System.out.println("case: " + c.getDescription());
        for (DataSource.Case.Phase p : c.getPhases()) {
            System.out.println("phase: " + p.getDescription());
            byte[] relayMsg = StringUtil.hexToBytes(p.getInput());
            List<String> messages = p.getMessages();
            byte[][] ret = (byte[][]) sm.call(BMC, BigInteger.ZERO, bmv.getAddress(), "handleRelayMessage",
                    BMC_BTP_ADDR.toString(), prev, BigInteger.valueOf(0), relayMsg);

            if (messages.size() > 0) {
                assertEquals(messages.size(), ret.length);
                for (int i=0; i<messages.size(); i++) {
                    assertEquals(messages.get(i), new String(ret[i]));
                }
            }
            BMVStatus status = bmv.call(BMVStatus.class, "getStatus");
            assertEquals(p.getStatus().getHeight(), status.getHeight());
        }
    }

    public static class MainNetBMVTest {
        private static final DataSource data = DataSource.loadDataSource("mainnet.json");
        // @TestFactory
        public Collection<DynamicTest> handleRelayMessageTests() {
            DataSource.ConstructorParams params = data.getParams();
            List<DynamicTest> t = new ArrayList<>();
            for (DataSource.Case c : data.getCases()) {
                t.add(DynamicTest.dynamicTest(c.getDescription(),
                        () -> {
                            Score bmv = deployBmv(params);
                            handleRelayMessageTest(c, bmv, "");
                        }
                ));
            }
            return t;
        }
    }

    public static class OneValidatorBMVTest {
        private static final DataSource data = DataSource.loadDataSource("privnet.json");;
        private static final BTPAddress PREV_BMC = BTPAddress.parse("btp://0x1.eth/0xD2F04942FF92709ED9d41988D161710D18d7f1FE");

        // @TestFactory
        public Collection<DynamicTest> handleRelayMessageTests() {
            DataSource.ConstructorParams params = data.getParams();
            List<DynamicTest> t = new ArrayList<>();
            for (DataSource.Case c : data.getCases()) {
                t.add(DynamicTest.dynamicTest(c.getDescription(),
                        () -> {
                            Score bmv = deployBmv(params);
                            handleRelayMessageTest(c, bmv, PREV_BMC.toString());
                        }
                ));
            }
            return t;
        }
    }
}
