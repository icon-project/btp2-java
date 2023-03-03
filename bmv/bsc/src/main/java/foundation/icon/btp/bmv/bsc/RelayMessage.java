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
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.Collections;

import java.util.Arrays;
import java.util.List;

public class RelayMessage {
    private List<TypePrefixedMessage> tpms;

    public RelayMessage(List<TypePrefixedMessage> tpms) {
        this.tpms = Collections.unmodifiableList(tpms);
    }

    public List<TypePrefixedMessage> getMessages() {
        return tpms;
    }

    public static RelayMessage readObject(ObjectReader r) {
        r.beginList();
        List<TypePrefixedMessage> tpms = new ArrayList<>();
        r.beginList();
        while(r.hasNext()) {
            tpms.add(r.read(TypePrefixedMessage.class));
        }
        r.end();
        return new RelayMessage(tpms);
    }

    public static void writeObject(ObjectWriter w, RelayMessage o) {
        w.beginList(1);
        w.beginList(o.tpms.size());
        for (TypePrefixedMessage typedMessage : o.tpms)
            w.write(typedMessage);
        w.end();
        w.end();
    }

    public static RelayMessage fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return readObject(r);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        writeObject(w, this);
        return w.toByteArray();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (TypePrefixedMessage tpm : tpms) {
            s.append(tpm.getMessage());
        }

        return "RelayMessage{" +
                "tpms=" + s +
                '}';
    }

    public static class TypePrefixedMessage {
        public static final int BLOCK_UPDATE = 1;
        public static final int BLOCK_PROOF = 2;
        public static final int MESSAGE_PROOF = 3;
        private final int type;
        private final byte[] payload;

        public TypePrefixedMessage(int type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }

        public Object getMessage() {
            try {
                if (type == BLOCK_UPDATE) {
                    return BlockUpdate.fromBytes(payload);
                } else if (type == BLOCK_PROOF) {
                    return BlockProof.fromBytes(payload);
                } else if (type == MESSAGE_PROOF) {
                    return MessageProof.fromBytes(payload);
                }
            } catch (Exception e) {
                throw BMVException.unknown("invalid relay message payload");
            }
            throw BMVException.unknown("invalid type : " + type);
        }

        public static TypePrefixedMessage readObject(ObjectReader r) {
            r.beginList();
            int type = r.readInt();
            byte[] payload = r.readByteArray();
            TypePrefixedMessage typePrefixedMessage = new TypePrefixedMessage(type, payload);
            r.end();
            return typePrefixedMessage;
        }

        public static TypePrefixedMessage fromBytes(byte[] bytes) {
            ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
            return readObject(r);
        }

        public static void writeObject(ObjectWriter w, TypePrefixedMessage o) {
            w.beginList(2);
            w.write(o.type);
            w.write(o.payload);
            w.end();
        }

        public byte[] toBytes() {
            ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
            writeObject(w, this);
            return w.toByteArray();
        }

        @Override
        public String toString() {
            return "TypePrefixedMessage{" +
                    "type=" + type +
                    ", payload=" + StringUtil.bytesToHex(payload) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypePrefixedMessage that = (TypePrefixedMessage) o;
            return type == that.type && Arrays.equals(payload, that.payload);
        }
    }
}
