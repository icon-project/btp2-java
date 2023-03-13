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

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.Collections;

import java.util.List;

public class MessageProof {
    // block hash
    private Hash id;
    private List<ReceiptProof> proofs;

    public MessageProof(Hash id, List<ReceiptProof> proofs) {
        this.id = id;
        this.proofs = Collections.unmodifiableList(proofs);
    }

    public static MessageProof readObject(ObjectReader r) {
        Hash id;
        List<ReceiptProof> proofs = new ArrayList<>();

        r.beginList();
        id = r.read(Hash.class);
        r.beginList();
        while (r.hasNext()) {
            proofs.add(r.read(ReceiptProof.class));
        }
        r.end();
        r.end();
        return new MessageProof(id, proofs);
    }

    public static MessageProof fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return MessageProof.readObject(r);
    }

    public Hash getId() {
        return this.id;
    }

    public List<ReceiptProof> getReceiptProofs() {
        return proofs;
    }

    @Override
    public String toString() {
        return "MessageProof{" +
                "id=" + id +
                ", proofs=" + proofs +
                '}';
    }

}
