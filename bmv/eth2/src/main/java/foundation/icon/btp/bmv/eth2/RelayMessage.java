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

import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.Arrays;
import java.util.List;

public class RelayMessage {
    private static final Logger logger = Logger.getLogger(RelayMessage.class);
    private TypePrefixedMessage[] messages;

    public RelayMessage() {}

    public TypePrefixedMessage[] getMessages() {
        return messages;
    }

    public void setMessages(TypePrefixedMessage[] messages) {
        this.messages = messages;
    }

    public static RelayMessage readObject(ObjectReader reader) {
        reader.beginList();
        RelayMessage relayMessage = new RelayMessage();
        List<TypePrefixedMessage> typePrefixedMessages = new ArrayList<>();
        reader.beginList();
        while(reader.hasNext())
            typePrefixedMessages.add(reader.read(TypePrefixedMessage.class));
        reader.end();
        int msgLength = typePrefixedMessages.size();
        TypePrefixedMessage[] messageArray = new TypePrefixedMessage[msgLength];
        for (int i = 0; i < msgLength; i++)
            messageArray[i] = typePrefixedMessages.get(i);
        relayMessage.setMessages(messageArray);
        reader.end();
        return relayMessage;
    }

    public static void writeObject(ObjectWriter writer, RelayMessage message) {
        writer.beginList(1);
        writer.beginList(message.messages.length);
        for (TypePrefixedMessage typedMessage : message.messages)
            writer.write(typedMessage);
        writer.end();
        writer.end();
    }

    public static RelayMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w, this);
        return w.toByteArray();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < messages.length; i++) {
            s.append(messages[i].toString());
        }

        return "RelayMessage{" +
                "messages=" + s +
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
                logger.println("getMessage, type = " + type);
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

        public static TypePrefixedMessage readObject(ObjectReader reader) {
            reader.beginList();
            TypePrefixedMessage typePrefixedMessage = new TypePrefixedMessage(reader.readInt(), reader.readByteArray());
            reader.end();
            return typePrefixedMessage;
        }

        public static TypePrefixedMessage fromBytes(byte[] bytes) {
            ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
            return readObject(reader);
        }

        public static void writeObject(ObjectWriter writer, TypePrefixedMessage message) {
            writer.beginList(2);
            writer.write(message.type);
            writer.write(message.payload);
            writer.end();
        }

        public byte[] toBytes() {
            ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
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