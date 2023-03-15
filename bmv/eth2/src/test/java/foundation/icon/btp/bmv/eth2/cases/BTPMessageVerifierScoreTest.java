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

import foundation.icon.btp.bmv.eth2.score.BMVScore;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.transport.http.HttpProvider;
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
    }

    @Test
    void hashTree() throws Exception {
        bmvScore = BMVScore.mustDeploy(txHandler, ownerWallet);
    }
}
