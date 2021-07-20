/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.bmc;

import score.Address;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

public interface RelayerManager {
    /**
     * (Payable) Registers the Relayer for the network.
     * TODO regiser relayer with bond, desc
     *
     * @param _desc String (description of Relayer)
     */
    @Payable
    @External
    void registerRelayer(String _desc);

    /**
     * Unregisters the Relayer for the network.
     * TODO [TBD] May fail if it's referred by the BMR.
     * <p>
     * _addr Address (the address of Relayer)
     */
    @External
    void unregisterRelayer();

    @External
    void removeRelayer(Address _addr, Address _refund);

    /**
     * Get registered the Relayers.
     *
     * @return A dictionary with the address of the Relayer as key and information of the Relayer as value.
     *
     * <br>For Example::<br>
     * {
     * "hx..." : {
     * "description": "description of the Relayer...",
     * "bond": "0x10"
     * }
     * }
     */
    @External(readonly = true)
    Map<String, Relayer> getRelayers();

    /**
     * Optional External method
     */
    @External
    void distributeRelayerReward();

    /**
     * Claim reward of the Relayer
     * Called by relayer
     *
     * TODO [TBD] Does it need to use 'Address' parameter instead of Context.getCaller()?
     */
    @External
    void claimRelayerReward();

    /**
     * Set minimum bond of the Relayer
     *
     * @param _value Integer
     */
    @External
    void setRelayerMinBond(BigInteger _value);

    /**
     * Get minimum bond of the Relayer
     *
     * @return Integer minimum bond of the Relayer
     */
    @External(readonly = true)
    BigInteger getRelayerMinBond();

    /**
     * Set period of reward calculation of the Relayer
     *
     * @param _value Integer
     */
    @External
    void setRelayerTerm(long _value);

    /**
     * Get period of reward calculation of the Relayer
     *
     * @return Integer period of reward calculation of the Relayer
     */
    @External(readonly = true)
    long getRelayerTerm();

    @External
    void setRelayerRewardRank(int _value);

    @External(readonly = true)
    int getRelayerRewardRank();

    @External
    void setNextRewardDistribution(long _height);

    /**
     * //FIXME instead of getter pattern
     * @return
     */
    @External(readonly = true)
    RelayerManagerProperties getRelayerManagerProperties();
}