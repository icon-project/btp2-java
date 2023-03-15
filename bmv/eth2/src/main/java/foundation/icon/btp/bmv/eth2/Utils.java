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
package foundation.icon.btp.bmv.eth2;

import java.math.BigInteger;

public class Utils {
    public static BigInteger SLOTS_PER_EPOCH = BigInteger.valueOf(32);
    public static BigInteger EPOCHS_PER_SYNC_COMMITTEE_PERIOD = BigInteger.valueOf(256);

    static BigInteger computeEpoch(BigInteger slot) {
        return slot.divide(SLOTS_PER_EPOCH);
    }

    static BigInteger computeSyncCommitteePeriod(BigInteger epoch) {
        return epoch.divide(EPOCHS_PER_SYNC_COMMITTEE_PERIOD);
    }
}
