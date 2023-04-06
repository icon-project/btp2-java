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

import foundation.icon.btp.lib.BTPAddress;
import score.Context;

import java.math.BigInteger;
import java.util.Arrays;

public class MessageEvent {
    // pre-calculated btp message event signature
    // keccak256("Message(string,uint256,bytes)")
    public static final Hash SIGNATURE = Hash.of("37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b");

    private BTPAddress next;
    private BigInteger sequence;
    private byte[] message;

    public MessageEvent(BTPAddress next, BigInteger sequence, byte[] message) {
        this.next = next;
        this.sequence = sequence;
        this.message = message;
    }

    public static MessageEvent of(BTPAddress next, EventLog log) {
        Context.require(SIGNATURE.equals(log.getSignature()), "Invalid Message event signature");
        Context.require(Arrays.equals(Context.hash("keccak-256", next.toString().getBytes()), log.getTopics().get(1)), "Mismatch next bmc");

        // TODO smell...
        byte[] dsz = new byte[32];
        System.arraycopy(log.getData(), 32, dsz, 0, 32);
        int msz = new BigInteger(dsz).intValue();
        byte[] message = new byte[msz];
        System.arraycopy(log.getData(), 64, message, 0, msz);

        return new MessageEvent(next, new BigInteger(log.getTopics().get(2)), message);
    }

    public BTPAddress getNext() {
        return next;
    }

    public BigInteger getSequence() {
        return sequence;
    }

    public byte[] getMessage() {
        return message;
    }
}
