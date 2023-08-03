/*
 * Copyright 2022 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.bmv.btpblock.cases;

import foundation.icon.btp.bmv.btpblock.score.BMCScore;
import foundation.icon.btp.bmv.btpblock.score.BMVScore;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.score.util.StringUtil;
import foundation.icon.test.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BTPMessageVerifierScoreTest extends TestBase {
    private static final int UNKNOWN = 25;
    private static final int NOT_VERIFIABLE = 26;
    private static TransactionHandler txHandler;
    private static KeyWallet ownerWallet;
    private static BMVScore bmvScore;
    private static BMCScore bmcScore;
    private static BMCScore prevBmCScore;
    private static BMCScore otherNetworkBMC;
    private static final String srcNetworkID = "0x1.icon";
    private static final BigInteger networkTypeID = BigInteger.TWO;
    private static final byte[] FIRST_BLOCK_HEADER1 = StringUtil.hexToBytes("f8a40a00a0b59054f1aa87fde02bf970f13e6f32eca3ccaecbf8504c4fdd7d44e9764d7009c00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7ab858f856f85494b2b9e51c766cc3cbceadcf1fb1ff915475d28fd5945e9843a7ab917006c228186d2539ee0a49bd027f94550177c85e84af7e5637afa8e8c53526b892a15794ad3d0afe910c0aa74d0e0f64835d6633756c03ef");
    private static final byte[] FIRST_BLOCK_HEADER2 = StringUtil.hexToBytes("f8a81400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e200a0335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa93864001520401f80000f800b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91");
    private static final byte[] FAIL_CASE_FIRST_BLOCK_HEADER = StringUtil.hexToBytes("f8a40a00a0b59054f1aa87fde02bf970f13e6f32eca3ccaecbf8504c4fdd7d44e9764d7009c00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7ab858f856f85494b2b9e51c766cc3cbceadcf1fb1ff915475d28fd5945e9843a7ab917006c228186d2539ee0a49bd027f94550177c85e84af7e5637afa8e8c53526b892a15794ad3d0afe910c0aa74d0e0f64835d6633756c03ef");
    static final List<String> SUCCESS_RELAY_MESSAGE1 = List.of(
            "cecdcc028ac9f800c483646f67f800",
            "f90227f90224f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700da0298d7f800d28363617488656c657068616e748462697264f800",
            "f901b4f901b1f901ae01b901aaf901a7b890f88e1e00a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0dd7d7cd1229649a054dbfd03969abe0ce4e439afc09f36e07165b95f866f565c0208a0335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa938640015203a086c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9f800b90112f9010ff9010cb84106c7a0ddd22d7400e2194f3605e3e85091f86a7f527f0c434c3c4c38dcdfc57d43848068744496fafd3d36327d94f569682119c733db5f28fd264174d252cec500b8419b3430b7a6c4be13719a8a65db32e9c102c4955393c5af1ff113a3a8e8497cf277b4f8de3a9253aa6d4a64dcc20d994a032d29728af71c792489ed9991b07dae01b8412fbb67e55dafc56a2233b898f8e7cf8798cd7405b21eda48c3375bf5041fb3d761f4dee890a3e4cf4224326f84bd94b009f707d63274b5cf7485bf189b241e0e01b84185108e8e77ee5a35bcec1261adf775ac505968c69fe0cde79c74315ca45e5d4105855a1f14e628a45bcab8e6554c4864e54007b30a81f3a7cdfc6b58004221df00",
            "f868f866f502b3f2c0cc866d6f6e6b6579846c696f6ee3e201a038f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07ef02adece3e202a09dc633d90e96b8744b27aaa5bb6bb2cca28f187c196cf7af1c82d8d1e8cd5f6fc6857469676572c0",
            "f903d7f903d4f901e701b901e3f901e0b8c9f8c72800a04e72027cbde3d568aa3427e6ce1af27c6f2dacefac31b589e79dc30a5aebf00fe3e201a08c46a84b0bad8d2ee375d1fb666256ef0abb7bfcf208b0e6e9a83718e2dda4c8020fa049da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e700f800b858f856f8549437360294b1fc085535d88b56fd49ca4bc53d4495940f0ff8981a4d7a5f858231a247ff8399c2f3c08a94394b6cc344b1f802f8659a4c45e2435c92d8133d949497581179ca01aab2903848be71d8d3a2dce615b90112f9010ff9010cb84187752998c9c0bb8b434983f2081f9634aff88a7c0ca1d44f8b627ad3f2dd631001f9f64fcd56da6f2fcb73ccc15c921f7984d7753382d4c7af53a15760cb553101b8412434c7320084711082dbbc47391edb54c38599cd08cfd9e305420880e99796426eb74812499318b46a7fc96bf35be2a2d017ec05e8c76b821ddf85cd03bb388701b841b5e48e3cb022b75275d93b1f693f379ebb326a48160357ab6c995b274a5a1f5a17e7f51a2b1ec4f477a9ce5402e8fb9f10b9f37d95484990af0a459c22fee52f00b8417f42277e1e0491c408c90a779a098439ba97792b027a792fe1b0e11b28af601b2a51a889785d3d2e78863696f63258bf4d5567cdc707e65f18da974dd5e95d0401f901e701b901e3f901e0b8c9f8c73200a00e24b5d5a9df68de9d34588a4b2c602fc26130291372ddd8f51ef9808a1270bde3e201a0d1cdfff9f24d50ba88a34f19cbdf5c214169df63629e299d9baffb242a1bb428020fa0efe5785c35214f54e0ee595ebbe96842cd158d9329e3f3f6ed7e30f8cfa835f900f800b858f856f85494cbe9f33d08a1afceb9a70a88cd83106ea46f73f5944390474702c3ea92b489452084980e8423452ca694d1f7a2d7fa13b72c868fda587d791d830d99cc639495e8c2643f5ca148cfd2c24d67e37cfc1f0e3cefb90112f9010ff9010cb8414458298bfc325cc44d8aecf680edf1763dc165c9ce05171a930d5640bab9cd666439f6bdf4fa917b56be81b44a61aeeeed7834040c415ace942e944cdb21932f01b84114fbb6c0c54103d2d22a55566debb065106708c5a0afad0e7e3201de62e7594b5aaab3e694460814e344507395b9a7ab6346563a00e1ee818659a1934d0e86e800b8416deb9288aed42c9962afc3fe8714c30d212b47f90a175ec6f86de83d26c38fbb3075eb68f57dddbe8ea6496d272aeae961c09759dbf1779e20a24716c711b1dc01b8411f189c84c47b291556722541d42eacfc5c7b51f0a0e0e36f4eafa7774848c6ad4e9ddab16395ed967cc20374f7451fda40e3a21b81b8f5f16ab5baac0658e82200"
    );
    static final List<String> SUCCESS_RELAY_MESSAGE2 = List.of(
            "f901eaf901e7f901ae01b901aaf901a7b890f88e1e00a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e200a049da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e70400a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac04a087b6db234476ea7fc1b573795cec8442233298ecc2d23b548dd619f92d306a2df800b90112f9010ff9010cb841addbf04245977a99500c94e4cc99f6e05a0c697ee092108c8be735c6f95c46653163b2f123769cafbbf5c9408db465106381758eb47002e9a10788a74dd47fec00b8418d45452ae3460b6c4fac99c632725c3dad070f0a6f74e1e028b8f24dd4a893063f0706f69917764ccd3bff4e51a4a459c6347a49c5f7f097fd0a00ac6c7d952b00b8417209209a417686b40baf73cf7ef89ea1800d11b16e593ecb57f97cc5fbd375c52da33b874f91c708b16d7e5f57d458c8edbab4bc2d81d72a39bb1029cf40b00e00b8412da450059654ee32a6019436c7f5036edc3c63c374bda0e9b9f38fda532bfc35652200875d910b52b68045cdcdcefcdc087b2d12da6ff481990cfcb432c8dcbf00f502b3f2c0cc866d6f6e6b6579846c696f6ee3e202a0d3f9be5d7f0324950c636665ce754816c87f5bd9cd64aee0ce1c0bd0fa8a2242",
            "f90223f90220f502b3f2e3e202a09dc633d90e96b8744b27aaa5bb6bb2cca28f187c196cf7af1c82d8d1e8cd5f6fcc8462697264866d6f6e6b6579c0f901e701b901e3f901e0b8c9f8c72800a04e72027cbde3d568aa3427e6ce1af27c6f2dacefac31b589e79dc30a5aebf00fe3e200a0efe5785c35214f54e0ee595ebbe96842cd158d9329e3f3f6ed7e30f8cfa835f90409a0dd7d7cd1229649a054dbfd03969abe0ce4e439afc09f36e07165b95f866f565c00f800b858f856f8549437360294b1fc085535d88b56fd49ca4bc53d4495940f0ff8981a4d7a5f858231a247ff8399c2f3c08a94394b6cc344b1f802f8659a4c45e2435c92d8133d949497581179ca01aab2903848be71d8d3a2dce615b90112f9010ff9010cb84187752998c9c0bb8b434983f2081f9634aff88a7c0ca1d44f8b627ad3f2dd631001f9f64fcd56da6f2fcb73ccc15c921f7984d7753382d4c7af53a15760cb553101b8412434c7320084711082dbbc47391edb54c38599cd08cfd9e305420880e99796426eb74812499318b46a7fc96bf35be2a2d017ec05e8c76b821ddf85cd03bb388701b841b5e48e3cb022b75275d93b1f693f379ebb326a48160357ab6c995b274a5a1f5a17e7f51a2b1ec4f477a9ce5402e8fb9f10b9f37d95484990af0a459c22fee52f00b8417f42277e1e0491c408c90a779a098439ba97792b027a792fe1b0e11b28af601b2a51a889785d3d2e78863696f63258bf4d5567cdc707e65f18da974dd5e95d0401",
            "f901edf901eaf901e701b901e3f901e0b8c9f8c73200a00e24b5d5a9df68de9d34588a4b2c602fc26130291372ddd8f51ef9808a1270bde3e200a0d33456b7b455a6381b4e523716b33f9593d3dfe00c7219a3656c983ed99ab8a90409a08c46a84b0bad8d2ee375d1fb666256ef0abb7bfcf208b0e6e9a83718e2dda4c800f800b858f856f85494cbe9f33d08a1afceb9a70a88cd83106ea46f73f5944390474702c3ea92b489452084980e8423452ca694d1f7a2d7fa13b72c868fda587d791d830d99cc639495e8c2643f5ca148cfd2c24d67e37cfc1f0e3cefb90112f9010ff9010cb8414458298bfc325cc44d8aecf680edf1763dc165c9ce05171a930d5640bab9cd666439f6bdf4fa917b56be81b44a61aeeeed7834040c415ace942e944cdb21932f01b84114fbb6c0c54103d2d22a55566debb065106708c5a0afad0e7e3201de62e7594b5aaab3e694460814e344507395b9a7ab6346563a00e1ee818659a1934d0e86e800b8416deb9288aed42c9962afc3fe8714c30d212b47f90a175ec6f86de83d26c38fbb3075eb68f57dddbe8ea6496d272aeae961c09759dbf1779e20a24716c711b1dc01b8411f189c84c47b291556722541d42eacfc5c7b51f0a0e0e36f4eafa7774848c6ad4e9ddab16395ed967cc20374f7451fda40e3a21b81b8f5f16ab5baac0658e82200"
    );

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        BigInteger amount = ICX.multiply(BigInteger.valueOf(3000));
        ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        // Deploy BMCs
        bmcScore = BMCScore.mustDeploy(txHandler, ownerWallet, srcNetworkID);
        prevBmCScore = BMCScore.mustDeploy(txHandler, ownerWallet, srcNetworkID);
        otherNetworkBMC = BMCScore.mustDeploy(txHandler, ownerWallet, "0x2.icon");
    }

    @Order(1)
    @Test
    public void positiveCases() throws TransactionFailureException, IOException, ResultTimeoutException {
        positiveCase(SUCCESS_RELAY_MESSAGE1, FIRST_BLOCK_HEADER1, new long[]{0, 1, 4, 4, 7});
        positiveCase(SUCCESS_RELAY_MESSAGE2, FIRST_BLOCK_HEADER2, new long[]{0, 2, 4});
    }

    @Order(2)
    @Test
    public void scenario2() throws TransactionFailureException, IOException, ResultTimeoutException {
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), FAIL_CASE_FIRST_BLOCK_HEADER, BigInteger.ZERO);
        var validMsg = "cecdcc028ac9f800c483646f67f800";
        String otherBMC = makeBTPAddress(otherNetworkBMC.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), otherBMC, BigInteger.ZERO, StringUtil.hexToBytes(validMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    @Order(3)
    @Test
    public void scenario3() throws IOException, ResultTimeoutException {
        var blockUpdateMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, StringUtil.hexToBytes(blockUpdateMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var remainMessage = "cecdcc028ac9f800c483646f67f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ZERO, StringUtil.hexToBytes(remainMessage));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var invalidNidMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0303a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidNidMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var invalidFirstSNMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0205a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidFirstSNMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + NOT_VERIFIABLE + ")");

        var invalidPrevHashMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0000000000000000000000000000000000000000000000000000000000000000003a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidPrevHashMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    @Order(4)
    @Test
    public void scenario4() throws IOException, ResultTimeoutException {
        var duplicatedSignatureMsg = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(duplicatedSignatureMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var proofNullMsg = "f8f5f8f3f8f101b8eef8ecb8e8f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83af800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(proofNullMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "UnknownFailure");
    }

    @Order(5)
    @Test
    public void scenario5() throws IOException, ResultTimeoutException {
        var hashMismatchMsg = "f901b5f901b2f901af01b901abf901a8b891f88f1400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd2682c1c0b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(hashMismatchMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    @Order(6)
    @Test
    public void scenario6() throws IOException, ResultTimeoutException {
        var proofMessageMsg = "dcdbda0298d7f800d28363617488656c657068616e748462697264f800";
        String prevBmc = makeBTPAddress(prevBmCScore.getAddress());
        var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(proofMessageMsg));
        var txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        // make remain count 2
        var validBlockUpdate = "f9020cf90209f9020601b90202f901ffb8e8f8e61400a0653028ae079ea11215676f155ec1732433affde292291941d39d82a2f676f877e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494ee9e945fd36670fe91fbdd1032e52c59cb5e2721949383f305cee0871031aaaf0a54770d9750a3d0d6942bbfe92f6e51b70604ef9b2a826589df1b1638f194a4b9be8024e0e3c9282e601998af9927a0fefc91b90112f9010ff9010cb8410e5f232f533fdc9f8e7df493d62b5ecdc0e7987190922561baed8de00bcf4be677265592dfea4068a86f97126b18abf8b94b973b13c9a52bb439d6bb6920bc5901b841dc4771d2a9ce2d59f0db8ccbc94b39d4dacb8d69ce987e4dfb6b99fae4c9feab3b0225ce311585c8e425b3681a70e20658fd7921420832fc82ffc99f05dc9eae01b84139a5c4164529099369bf00e28851f9cd127856f3ac7d5b513f23e7e19adbdc10643c80b38d5306b8e5c9688486759acd75cca2e019451cc5f51511ca6d1e37e000b841ded736c290086f0c5e655fd0782ad419bd6f0aa609aee21c01bb172d6af07a461e52b301f5106f4d9db2c72ceb1f818b96df7201afbbee8b1bae7bea48fa2a5700";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(validBlockUpdate));
        txResult = txHandler.getResult(txHash);
        assertSuccess(txResult);

        var mismatchLeftNumMsg = "f83cf83af83802b6f5e3e201a052763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478ace88656c657068616e748462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchLeftNumMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var invalidNumOfLeafMsg = "f842f840f83e02b83bf839e3e203a0468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e9d28363617488656c657068616e748462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidNumOfLeafMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var invalidLevelMsg = "f0efee02acebf800c483636174e3e202a0468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e9";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(invalidLevelMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var mismatchCountMsg = "f856f854f85202b84ff84df800f847b84052763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478a468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e98462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchCountMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");

        var mismatchRootMsg = "f85ff85df85b02b858f856f800f850b84452763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478a468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e97465737484626972648462697264f800";
        txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), prevBmc, BigInteger.ONE, StringUtil.hexToBytes(mismatchRootMsg));
        txResult = txHandler.getResult(txHash);
        assertEquals(txResult.getFailure().getMessage(), "Reverted(" + UNKNOWN + ")");
    }

    private void positiveCase(List<String> msgList, byte[] blockHeader, long[] seqs) throws TransactionFailureException, IOException, ResultTimeoutException {
        // Deploy BMV
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet, srcNetworkID, networkTypeID, bmcScore.getAddress(), blockHeader, BigInteger.ZERO);

        var msgLength = msgList.size();
        var hashes = new Bytes[msgLength];
        for (int i = 0; i < msgLength; i++) {
            hashes[i] = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), makeBTPAddress(prevBmCScore.getAddress()), BigInteger.valueOf(seqs[i]), StringUtil.hexToBytes(msgList.get(i)));
        }
        for (Bytes h : hashes) {
            assertSuccess(txHandler.getResult(h));
        }
    }

    private String makeBTPAddress(Address address) {
        var net = BMCScore.bmcNetWork.get(address);
        return "btp://" + net + "/" + address.toString();
    }
}
