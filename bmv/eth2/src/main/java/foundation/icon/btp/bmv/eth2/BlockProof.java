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
import score.Context;
import score.ObjectReader;

public class BlockProof {
    private byte[] header;
    private Proof proof;

    public BlockProof(byte[] header, Proof proof) {
        this.header = header;
        this.proof = proof;
    }

    LightClientHeader getLightClientHeader() {
        return LightClientHeader.deserialize(header);
    }

    Proof getProof() {
        return proof;
    }

    public static BlockProof readObject(ObjectReader r) {
        r.beginList();
        var blockProof = new BlockProof(r.readByteArray(), r.read(Proof.class));
        r.end();
        return blockProof;
    }

    public static BlockProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockProof.readObject(reader);
    }

    @Override
    public String toString() {
        return "BlockProof{" +
                "header=" + StringUtil.toString(header) +
                ", proof=" + proof +
                '}';
    }
}
