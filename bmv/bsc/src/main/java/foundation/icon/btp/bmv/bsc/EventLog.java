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

import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.Collections;

import java.util.List;

public class EventLog {
    private EthAddress address;
    private List<byte[]> topics;
    private byte[] data;

    public EventLog(EthAddress address, List<byte[]> topics, byte[] data) {
        this.address = address;
        this.topics = Collections.unmodifiableList(topics);
        this.data = data;
    }

    public static EventLog readObject(ObjectReader r) {
        r.beginList();
        EthAddress address = r.read(EthAddress.class);
        r.beginList();
        List<byte[]> topics = new ArrayList<>();
        while(r.hasNext()) {
            topics.add(r.readByteArray());
        }
        r.end();
        byte[] data = r.readByteArray();
        r.end();
        return new EventLog(address, topics, data);
    }

    public EthAddress getAddress() {
        return address;
    }

    public List<byte[]> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    public Hash getSignature() {
        return Hash.of(topics.get(0));
    }
}
