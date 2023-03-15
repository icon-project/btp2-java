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

import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;

public class Proof {
    private BigInteger index;
    private byte[] leaf;
    private byte[][] hashes;

    public Proof(BigInteger index, byte[] leaf, byte[][] hashes) {
        this.index = index;
        this.leaf = leaf;
        this.hashes = hashes;
    }

    BigInteger getIndex() {
        return index;
    }

    byte[] getLeaf() {
        return leaf;
    }

    byte[][] getHashes() {
        return hashes;
    }

    public static Proof readObject(ObjectReader r) {
        r.beginList();
        var index = r.readBigInteger();
        var leaf = r.readByteArray();
        var hashList = new ArrayList<byte[]>();
        while(r.hasNext())
            hashList.add(r.readByteArray());
        var hashesLength = hashList.size();
        var hashes = new byte[hashesLength][];
        for (int i = 0; i < hashesLength; i++)
            hashes[i] = hashList.get(i);
        r.end();
        return new Proof(index, leaf, hashes);
    }

}
