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

import foundation.icon.score.util.StringUtil;
import score.ObjectReader;
import score.ObjectWriter;

import java.util.Arrays;

public class EthAddress implements Comparable<EthAddress> {
    public static final int LENGTH = 20;

    private final byte[] data;

    public EthAddress(byte[] data) {
        this.data = data;
    }

    public static EthAddress of(String data) {
        if (data.substring(0, 2).compareTo("0x") == 0) {
            data = data.substring(2);
        }
        return EthAddress.of(StringUtil.hexToBytes(data));
    }

    public static EthAddress of(byte[] data) {
        if (data.length != LENGTH) throw BMVException.unknown("invalid Address data length");
        return new EthAddress(data);
    }

    public byte[] getEthAddress() {
        return data;
    }

    public static EthAddress readObject(ObjectReader r) {
        return new EthAddress(r.readByteArray());
    }

    public static void writeObject(ObjectWriter w, EthAddress o) {
        w.write(o.data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EthAddress that = (EthAddress) o;
        return Arrays.equals(data, that.data);
    }

    @Override
    public String toString() {
        return StringUtil.toString(data);
    }

    @Override
    public int compareTo(EthAddress o) {
        return StringUtil.bytesToHex(data).compareTo(StringUtil.bytesToHex(o.data));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
