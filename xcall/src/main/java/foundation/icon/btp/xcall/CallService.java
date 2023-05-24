/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.xcall;

import foundation.icon.score.client.ScoreClient;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

@ScoreClient
public interface CallService {
    /**
     * The name of CallService.
     */
    String NAME = "xcall";

    /**
     * Sends a call message to the contract on the destination chain.
     *
     * @param _to The BTP address of the callee on the destination chain
     * @param _data The calldata specific to the target contract
     * @param _rollback (Optional) The data for restoring the caller state when an error occurred
     * @return The serial number of the request
     */
    @Payable
    @External
    BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback);

    /**
     * Rollbacks the caller state of the request '_sn'.
     *
     * @param _sn The serial number of the previous request
     */
    @External
    void executeRollback(BigInteger _sn);

    /**
     * Executes the requested call message.
     *
     * @param _reqId The request id
     */
    @External
    void executeCall(BigInteger _reqId);
}
