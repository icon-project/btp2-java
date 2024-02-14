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

import foundation.icon.score.util.StringUtil;

import java.math.BigInteger;

public class Constants {
    public static final int HASH_LENGTH = 32;
    public static final int SYNC_COMMITTEE_COUNT = 512;
    public static final int BYTES_PER_CHUNK = 32;
    public static final int MIN_SYNC_COMMITTEE_PARTICIPANTS = 1;
    public static final byte[] MAINNET_GENESIS_VALIDATORS_ROOT = StringUtil.hexToBytes("4b363db94e286120d76eb905340fdd4e54bfe9f06bf33ff6cf5ad27f511bfe95");
    public static final byte[] SEPOLIA_GENESIS_VALIDATORS_ROOT = StringUtil.hexToBytes("d8ea171f3c94aea21ebc42a1ed61052acf3f9209c00e4efbaaddac09ed9b8078");

    // https://github.com/ethereum/execution-specs/tree/master/network-upgrades/mainnet-upgrades
    // read via /eth/v1/config/fork_schedule
    public static final BigInteger MAINNET_ALTAIR_EPOCH = BigInteger.valueOf(74240);
    public static final BigInteger MAINNET_BELLATRIX_EPOCH = BigInteger.valueOf(144896);
    public static final BigInteger MAINNET_CAPELLA_EPOCH = BigInteger.valueOf(194048);
    public static final BigInteger MAINNET_DENEB_EPOCH = BigInteger.valueOf(269568);
    public static final byte[] MAINNET_GENESIS_VERSION = StringUtil.hexToBytes("00000000");
    public static final byte[] MAINNET_ALTAIR_VERSION = StringUtil.hexToBytes("01000000");
    public static final byte[] MAINNET_BELLATRIX_VERSION = StringUtil.hexToBytes("02000000");
    public static final byte[] MAINNET_CAPELLA_VERSION = StringUtil.hexToBytes("03000000");
    public static final byte[] MAINNET_DENEB_VERSION = StringUtil.hexToBytes("04000000");

    public static final BigInteger SEPOLIA_ALTAIR_EPOCH = BigInteger.valueOf(50);
    public static final BigInteger SEPOLIA_BELLATRIX_EPOCH = BigInteger.valueOf(100);
    public static final BigInteger SEPOLIA_CAPELLA_EPOCH = BigInteger.valueOf(56832);
    public static final BigInteger SEPOLIA_DENEB_EPOCH = BigInteger.valueOf(132608);
    public static final byte[] SEPOLIA_GENESIS_VERSION = StringUtil.hexToBytes("90000069");
    public static final byte[] SEPOLIA_ALTAIR_VERSION = StringUtil.hexToBytes("90000070");
    public static final byte[] SEPOLIA_BELLATRIX_VERSION = StringUtil.hexToBytes("90000071");
    public static final byte[] SEPOLIA_CAPELLA_VERSION = StringUtil.hexToBytes("90000072");
    public static final byte[] SEPOLIA_DENEB_VERSION = StringUtil.hexToBytes("90000073");
}
