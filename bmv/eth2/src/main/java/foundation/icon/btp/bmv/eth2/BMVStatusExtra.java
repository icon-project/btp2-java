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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class BMVStatusExtra {
    private BigInteger lastMessageSequence;

    private BigInteger lastMessageSlot;

    public BMVStatusExtra() {
    }

    public BigInteger getLastMessageSequence() {
        return lastMessageSequence;
    }

    public void setLastMessageSequence(BigInteger lastMessageSequence) {
        this.lastMessageSequence = lastMessageSequence;
    }

    public BigInteger getLastMessageSlot() {
        return lastMessageSlot;
    }

    public void setLastMessageSlot(BigInteger lastMessageSlot) {
        this.lastMessageSlot = lastMessageSlot;
    }

    public BMVStatusExtra(BigInteger lastMessageSequence, BigInteger lastMessageSlot) {
        this.lastMessageSequence = lastMessageSequence;
        this.lastMessageSlot = lastMessageSlot;
    }

    public static BMVStatusExtra readObject(ObjectReader r) {
        var extra = new BMVStatusExtra();
        r.beginList();
        extra.setLastMessageSequence(r.readBigInteger());
        extra.setLastMessageSlot(r.readBigInteger());
        r.end();
        return extra;
    }

    public static void writeObject(ObjectWriter writer, BMVStatusExtra extra) {
        writer.beginList(2);
        writer.write(extra.getLastMessageSequence());
        writer.write(extra.getLastMessageSlot());
        writer.end();
    }
    public static BMVStatusExtra fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMVStatusExtra.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BMVStatusExtra.writeObject(writer, this);
        return writer.toByteArray();
    }
}
