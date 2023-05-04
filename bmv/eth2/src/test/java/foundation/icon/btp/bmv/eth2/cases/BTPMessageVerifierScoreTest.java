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

package foundation.icon.btp.bmv.eth2.cases;

import foundation.icon.btp.bmv.eth2.DataSource;
import foundation.icon.btp.bmv.eth2.score.BMCScore;
import foundation.icon.btp.bmv.eth2.score.BMVScore;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.score.util.StringUtil;
import foundation.icon.test.*;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.*;

import java.math.BigInteger;


@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BTPMessageVerifierScoreTest extends TestBase {
    private static TransactionHandler txHandler;
    private static KeyWallet ownerWallet;
    private static BMVScore bmvScore;
    private static BMCScore bmcScore;
    private static final String srcNetworkID = "0xaa36a7.eth";
    private static final DataSource data = DataSource.loadDataSource("sepolia.json");
    private static final String PREV_BMC = "btp://0xaa36a7.eth/0xd2f04942ff92709ed9d41988d161710d18d7f1fe";

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        BigInteger amount = ICX.multiply(BigInteger.valueOf(3000));
        ownerWallet = KeyWallet.create();
        txHandler.transfer(ownerWallet.getAddress(), amount);
        ensureIcxBalance(txHandler, ownerWallet.getAddress(), BigInteger.ZERO, amount);

        // Deploy BMCs
        bmcScore = BMCScore.mustDeploy(txHandler, ownerWallet, "0x1.icon");
    }

    @Test
    void successCase() throws Exception {
        var params = data.getParams();
        var cases = data.getCases();
        bmvScore = BMVScore.mustDeploy(
                txHandler,
                ownerWallet,
                srcNetworkID,
                StringUtil.hexToBytes(params.getGenesisValidatorsHash()),
                StringUtil.hexToBytes(params.getSyncCommittee()),
                bmcScore.getAddress(),
                StringUtil.hexToBytes(params.getFinalizedHeader()),
                BigInteger.ZERO
        );

        BigInteger seq = BigInteger.ZERO;
        for (DataSource.Case c: cases) {
            for (DataSource.Case.Phase p : c.getPhases()) {
                var txHash = bmcScore.handleRelayMessage(ownerWallet, bmvScore.getAddress(), PREV_BMC, seq, StringUtil.hexToBytes(p.getInput()));
                assertSuccess(txHandler.getResult(txHash));
            }
        }
    }
}
