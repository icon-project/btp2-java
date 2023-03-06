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
package foundation.icon.btp.bmv.bsc;

import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class ReceiptProof {
    private byte[] key; // transaction index encoded with rlp
    private byte[][] proof; // merkle proof

    private ReceiptProof(byte[] key, byte[][] proof) {
        this.key = key;
        this.proof = proof;
    }

    public static ReceiptProof readObject(ObjectReader r) {
        r.beginList();
        byte[] key = r.readByteArray();
        r.beginList();
        List<byte[]> proof = new ArrayList<>();
        while (r.hasNext()) {
            proof.add(r.readByteArray());
        }
        r.end();
        r.end();
        int i = 0;
        byte[][] _proof = new byte[proof.size()][];
        for (byte[] part : proof) {
            _proof[i++] = part;
        }
        return new ReceiptProof(key, _proof);
    }

    public static ReceiptProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLP", bytes);
        return ReceiptProof.readObject(reader);
    }

    public byte[] getKey() {
        return key;
    }

    public byte[][] getProof() {
        return proof;
    }
}
