/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.bmv.btpblock;

import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class MessageProof {
    private final List<ProofNode> leftProofNodes;
    private final byte[][] messages;
    private final List<ProofNode> rightProofNodes;

    public MessageProof(List<ProofNode> leftProofNodes, byte[][] messages, List<ProofNode> rightProofNodes) {
        this.leftProofNodes = leftProofNodes;
        this.messages = messages;
        this.rightProofNodes = rightProofNodes;
    }

    public byte[][] getMessages() {
        return messages;
    }

    public static MessageProof readObject(ObjectReader r) {
        r.beginList();
        List<ProofNode> lNodes = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            lNodes.add(r.read(ProofNode.class));
        }
        r.end();
        List<byte[]> messageList = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            messageList.add(r.readByteArray());
        }
        byte[][] messages = new byte[messageList.size()][];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = messageList.get(i);
        }
        r.end();
        List<ProofNode> rNodes = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            rNodes.add(r.read(ProofNode.class));
        }
        r.end();
        r.end();
        return new MessageProof(lNodes, messages, rNodes);
    }

    public static MessageProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MessageProof.readObject(reader);
    }

    public ProveResult proveMessage() {
        Node node = new Node();
        int left = 0, total = 0;
        for (ProofNode pn : leftProofNodes) {
            var num = pn.getNumOfLeaf();
            node = node.add(num, pn.getValue());
            left += num;
        }

        for (byte[] message : messages) {
            node = node.add(1, BTPMessageVerifier.hash(message));
            total++;
        }

        for (ProofNode pn : rightProofNodes) {
            var num = pn.getNumOfLeaf();
            node = node.add(num, pn.getValue());
            total += num;
        }
        node.ensureHash(false);

        total += left;
        var rootNumOfLeaf = node.getNumOfLeaf();
        if (total != rootNumOfLeaf)
            throw BMVException.unknown("total doesn't match total : " + total + ", node : " + rootNumOfLeaf);
        node.verify();
        return new ProveResult(node.getValue(), left, total);
    }

    public void printRightNodes() {
        int i = 0;
        for (ProofNode pn : rightProofNodes) {
            Context.println("ProofInRight[" + i + "]"
                    + ": numOfLeaf=" + pn.getNumOfLeaf()
                    + ", value=" + StringUtil.bytesToHex(pn.getValue()));
            i++;
        }
    }

    public static class ProveResult {
        final byte[] hash;
        final int offset;
        final int total;

        public ProveResult(byte[] hash, int left, int total) {
            this.hash = hash;
            this.offset = left;
            this.total = total;
        }
    }
}
