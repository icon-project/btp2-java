package foundation.icon.btp.bmv.bsc2;

import com.iconloop.score.test.ManualRevertException;
import com.iconloop.score.test.TestBase;
import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnitTest extends TestBase {
    public static class VoteAttestationTest {
        public static Validators validators = new Validators(new ArrayList<>() {{
            add(new Validator(EthAddress.of("1284214b9b9c85549ab3d2b972df0deef66ac2c9"),
                    BLSPublicKey.of("8e82934ca974fdcd97f3309de967d3c9c43fa711a8d673af5d75465844bf8969c8d1948d903748ac7b8b1720fa64e50c")));
            add(new Validator(EthAddress.of("35552c16704d214347f29fa77f77da6d75d7c752"),
                    BLSPublicKey.of("b742ad4855bae330426b823e742da31f816cc83bc16d69a9134be0cfb4a1d17ec34f1b5b32d5c20440b8536b1e88f0f2")));
            add(new Validator(EthAddress.of("47788386d0ed6c748e03a53160b4b30ed3748cc5"),
                    BLSPublicKey.of("000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")));
            add(new Validator(EthAddress.of("980a75ecd1309ea12fa2ed87a8744fbfc9b863d5"),
                    BLSPublicKey.of("89037a9ace3b590165ea1c0c5ac72bf600b7c88c1e435f41932c1132aae1bfa0bb68e46b96ccb12c3415e4d82af717d8")));
            add(new Validator(EthAddress.of("a2959d3f95eae5dc7d70144ce1b73b403b7eb6e0"),
                    BLSPublicKey.of("b973c2d38487e58fd6e145491b110080fb14ac915a0411fc78f19e09a399ddee0d20c63a75d8f930f1694544ad2dc01b")));
            add(new Validator(EthAddress.of("b71b214cb885500844365e95cd9942c7276e7fd8"),
                    BLSPublicKey.of("a2750ec6dded3dcdc2f351782310b0eadc077db59abca0f0cd26776e2e7acb9f3bce40b1fa5221fd1561226c6263cc5f")));
            add(new Validator(EthAddress.of("f474cf03cceff28abc65c9cbae594f725c80e12d"),
                    BLSPublicKey.of("96c9b86c3400e529bfe184056e257c07940bb664636f689e8d2027c834681f8f878b73445261034e946bb2d901b4b878")));
        }});

        @Test
        void verifyValidVoteSignature() {
            VoteAttestation attestation = VoteAttestation.fromBytes(StringUtil.hexToBytes("f8b27bb860b52b3719228d17a7d7dc98d69adcc43d6e663ee9d265d1ca4fb3a9cd0d7eff6434b40b1135190ee1dc683684cc48f2290495bf14bc187d068a98a396bca4fa585bfa9c355198fa543a62db80cf4f1d18d913f353407f2aa35336a2cdd53e85daf84c8401e53abea0826b43a928a31836f6025f9ba8d70691c0c32fe5b48b219792365203e8e37f348401e53abfa0dc77f6bcfb22b3ea2de6cb5f2dbe9fd2b8441dbb9785ce2d0d73af81f4709f3880"));
            assertDoesNotThrow(() -> attestation.verify(validators));
        }

        @Test
        void verifyValidVoteSignatureWithWrongMessage() {
            VoteAttestation attestation = VoteAttestation.fromBytes(StringUtil.hexToBytes("f8b27bb860b52b3719228d17a7d7dc98d69adcc43d6e663ee9d265d1ca4fb3a9cd0d7eff6434b40b1135190ee1dc683684cc48f2290495bf14bc187d068a98a396bca4fa585bfa9c355198fa543a62db80cf4f1d18d913f353407f2aa35336a2cdd53e85daf84c8401e53abea0826b43a928a31836f6025f9ba8d70691c0c32fe5b48b219792365203e8e37f348401e53abfa0dc77f6bcfb22b3ea2de6cb5f2dbe9fd2b8441dbb9785ce2d0d73af71f4709f3880"));
            assertThrows(ManualRevertException.class, () -> attestation.verify(validators));
        }

        @Test
        void verifyInvalidVoteSignature() {
            VoteAttestation attestation = VoteAttestation.fromBytes(StringUtil.hexToBytes("f8b27bb860b53b3719228d17a7d7dc98d69adcc43d6e663ee9d265d1ca4fb3a9cd0d7eff6434b40b1135190ee1dc683684cc48f2290495bf14bc187d068a98a396bca4fa585bfa9c355198fa543a62db80cf4f1d18d913f353407f2aa35336a2cdd53e85daf84c8401e53abea0826b43a928a31836f6025f9ba8d70691c0c32fe5b48b219792365203e8e37f348401e53abfa0dc77f6bcfb22b3ea2de6cb5f2dbe9fd2b8441dbb9785ce2d0d73af81f4709f3880"));
            assertThrows(IllegalArgumentException.class, () -> attestation.verify(validators));
        }
    }
}


