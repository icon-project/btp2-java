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

package foundation.icon.btp.bmv.eth2.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.test.Log;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;


public class BMVScore extends Score {

    private static final Log LOG = Log.getGlobal();

    public static BMVScore mustDeploy(
            TransactionHandler txHandler,
            Wallet wallet
    )
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "bmv");
        RpcObject params = new RpcObject.Builder()
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("bmv-ether2"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new BMVScore(score);
    }

    public BMVScore(Score other) {
        super(other);
    }
}
