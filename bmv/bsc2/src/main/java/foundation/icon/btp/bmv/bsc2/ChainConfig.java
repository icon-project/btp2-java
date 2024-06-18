/*
 * Copyright 2023 ICON Foundation
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
package foundation.icon.btp.bmv.bsc2;

import score.Context;

import java.math.BigInteger;

public class ChainConfig {
    public final long ChainID;
    public final long Epoch;
    public final long Period;
    public final BigInteger Hertz;
    public final BigInteger Tycho;

    private static ChainConfig instance;

    public static ChainConfig setChainID(BigInteger cid) {
        if (instance == null || instance.ChainID != cid.longValueExact()) {
            instance = fromChainID(cid);
        }
        return instance;
    }

    private ChainConfig(long chainId, long epoch, long period, BigInteger hertz, BigInteger tycho) {
        this.ChainID = chainId;
        this.Epoch = epoch;
        this.Period = period;
        this.Hertz = hertz;
        this.Tycho = tycho;
    }

    public static ChainConfig getInstance() {
        return instance;
    }

    public static ChainConfig fromChainID(BigInteger cid) {
        if (cid.longValue() == 56L) {
            // BSC Mainnet
            return new ChainConfig(56L, 200L, 3L, BigInteger.valueOf(31302048L),
                    BigInteger.valueOf(1718863500L));
        } else if (cid.longValue() == 97L) {
            // BSC Testnet
            return new ChainConfig(97L, 200L, 3L, BigInteger.valueOf(31103030L),
                    BigInteger.valueOf(1713330442L));
        } else if (cid.longValue() == 99L) {
            // Private BSC Testnet
            return new ChainConfig(99L, 200L, 3L, BigInteger.valueOf(8), null);
        }

        Context.require(false, "No Chain Config - ChainID(" + cid.intValue() + ")");
        return null;
    }

    public boolean isEpoch(BigInteger number) {
        return number.longValue() % this.Epoch == 0;
    }

    public boolean isHertz(BigInteger number) {
        return Hertz != null && Hertz.compareTo(number) <= 0;
    }

    public boolean isTycho(long time) {
        return Tycho != null && Tycho.longValue() <= time;
    }

}
