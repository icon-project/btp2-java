package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModeTest implements BMCIntegrationTest {
    static BTPAddress link = Faker.btpLink();
    static String svc = MockBSHIntegrationTest.SERVICE;
    static Address relay = bmc._wallet().getAddress();

    static void setMode(BigInteger mode) {
        System.out.printf("ModeTest:setMode _mode:%s\n", mode);
        iconSpecific.setMode(mode);
        assertEquals(mode, iconSpecific.getMode());
    }

    @AfterAll
    static void afterAll() {
        System.out.println("ModeTest:afterAll start");
        setMode(BTPMessageCenter.MODE_NORMAL);
        System.out.println("ModeTest:afterAll end");
    }

    @Test
    void setModeShouldSuccess() {
        setMode(BTPMessageCenter.MODE_MAINTENANCE);
        sendMessageShouldRevert();
        handleRelayMessageShouldRevert();
        handleFragmentShouldRevert();
        claimRewardShouldRevert();
    }

    void sendMessageShouldRevert() {
        System.out.println("ModeTest:sendMessageShouldRevert");
        AssertBMCException.assertUnknown(() -> bmc.sendMessage(txr -> {},
                link.net(), svc, BigInteger.ZERO, Faker.btpLink().toBytes()));
    }

    void handleRelayMessageShouldRevert() {
        System.out.println("ModeTest:handleRelayMessageShouldRevert");
        AssertBMCException.assertUnknown(() ->
                bmc.handleRelayMessage(link.toString(),
                        MessageTest.mockRelayMessage(MessageTest.btpMessageForSuccess(link)).toBase64String()));
    }

    void handleFragmentShouldRevert() {
        System.out.println("ModeTest:handleFragmentShouldRevert");
        AssertBMCException.assertUnknown(() ->
                iconSpecific.handleFragment(link.toString(),
                        MessageTest.mockRelayMessage(MessageTest.btpMessageForSuccess(link)).toBase64String(), 0));
    }

    void claimRewardShouldRevert() {
        System.out.println("ModeTest:claimRewardShouldRevert");
        AssertBMCException.assertUnknown(() ->
                bmc.claimReward(link.net(), relay.toString()));
    }
}
