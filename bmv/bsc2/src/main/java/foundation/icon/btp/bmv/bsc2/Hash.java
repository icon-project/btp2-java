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

public class Hash {
    public static final Hash EMPTY = new Hash(new byte[32]);
    private final byte[] data;

    public Hash(byte[] data) {
        this.data = data;
    }

    public static Hash of (String data) {
        return Hash.of(StringUtil.hexToBytes(data));
    }

    public static Hash of(byte[] data) {
        if (data.length != 32) {
            throw new IllegalArgumentException("wrong hash length");
        }
        return new Hash(data);
    }

    public static void writeObject(ObjectWriter w, Hash o) {
        w.write(o.data);
    }

    public static Hash readObject(ObjectReader r) {
        byte[] d = r.readByteArray();
        return new Hash(d);
    }

    public byte[] toBytes() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Hash)) {
            return false;
        }
        Hash other = (Hash)o;
        return Arrays.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return StringUtil.toString(data);
    }

}
