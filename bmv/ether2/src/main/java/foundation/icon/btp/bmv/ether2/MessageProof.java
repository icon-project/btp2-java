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
package foundation.icon.btp.bmv.ether2;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;

public class MessageProof {
    private BigInteger slot;
    private Proof receiptRootProof;
    private ReceiptProof[] receiptProofs;

    public BigInteger getSlot() {
        return slot;
    }

    public Proof getReceiptRootProof() {
        return receiptRootProof;
    }

    public ReceiptProof[] getReceiptProofs() {
        return receiptProofs;
    }

    public MessageProof(BigInteger slot, Proof sszProof, ReceiptProof[] receiptProofs) {
        this.slot = slot;
        this.receiptRootProof = sszProof;
        this.receiptProofs = receiptProofs;
    }

    public static MessageProof readObject(ObjectReader r) {
        r.beginList();
        var slot = r.readBigInteger();
        var proof = r.read(Proof.class);
        var receiptsProofList = new ArrayList<ReceiptProof>();

        while(r.hasNext())
            receiptsProofList.add(r.read(ReceiptProof.class));
        var receiptsProofLen = receiptsProofList.size();
        var receiptsProofs = new ReceiptProof[receiptsProofLen];
        for (int i = 0; i < receiptsProofLen; i++)
            receiptsProofs[i] = receiptsProofList.get(i);
        r.end();
        return new MessageProof(slot, proof, receiptsProofs);
    }

    public static MessageProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MessageProof.readObject(reader);
    }
}