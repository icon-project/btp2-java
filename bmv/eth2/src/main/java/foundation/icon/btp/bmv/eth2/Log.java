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
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;

public class Log {
    private byte[] address;
    private byte[][] topics;
    private byte[] data;
    public Log(byte[] address, byte[][] topics, byte[] data) {
        this.address = address;
        this.topics = topics;
        this.data = data;
    }

    public byte[] getAddress() {
        return address;
    }

    public byte[][] getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getMessage() {
        var data = getData();
        byte[] dsz = new byte[32];
        System.arraycopy(data, 32, dsz, 0, 32);
        int msz = new BigInteger(dsz).intValue();
        byte[] message = new byte[msz];
        System.arraycopy(data, 64, message, 0, msz);
        return message;
    }

    public static Log readObject(ObjectReader r) {
        r.beginList();
        var address = r.readByteArray();
        var topicList = new ArrayList<byte[]>();
        r.beginList();
        while(r.hasNext())
            topicList.add(r.readByteArray());
        r.end();
        var topicLength = topicList.size();
        var topics = new byte[topicLength][];
        for (int i = 0; i < topicLength; i++)
            topics[i] = topicList.get(i);
        var data = r.readByteArray();
        r.end();
        return new Log(address, topics, data);
    }

    @Override
    public String toString() {
        return "Log{" +
                "address=" + StringUtil.toString(address) +
                ", topics=" + StringUtil.toString(topics) +
                ", data=" + StringUtil.toString(data) +
                '}';
    }
}
