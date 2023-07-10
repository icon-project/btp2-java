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
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class BlockProof {
    private final Header header;
    private final BigInteger height;
    private final byte[][] witness;

    public BlockProof(Header header, BigInteger height, byte[][] witness) {
        this.header = header;
        this.height = height;
        this.witness = witness;
    }

    public static BlockProof readObject(ObjectReader r) {
        r.beginList();
        Header header = r.read(Header.class);
        BigInteger height = r.readBigInteger();
        r.beginList();
        List<byte[]> w = new ArrayList<>();
        while (r.hasNext()) {
            w.add(r.readByteArray());
        }
        r.end();
        r.end();

        byte[][] witness = new byte[w.size()][];
        for (int i = 0; i < w.size(); i++) {
            witness[i] = w.get(i);
        }
        return new BlockProof(header, height, witness);
    }

    public static BlockProof fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return BlockProof.readObject(r);
    }

    public Header getHeader() {
        return this.header;
    }

    public BigInteger getHeight() {
        return this.height;
    }

    public byte[][] getWitness() {
        return this.witness;
    }

    @Override
    public String toString() {
        return "BlockProof{" +
                "header=" + header +
                ", witness=" + witness +
                '}';
    }
}
