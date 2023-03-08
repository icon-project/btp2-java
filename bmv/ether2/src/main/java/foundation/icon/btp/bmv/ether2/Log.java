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

import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;

public class Log {
    private byte[] address;
    private byte[][] topics;
    private byte[] data;
    private BigInteger blockNumber;
    private byte[] txHash;
    private BigInteger txIndex;
    private byte[] blockHash;
    private BigInteger index;
    private boolean removed;

    public Log(byte[] address, byte[][] topics, byte[] data, BigInteger blockNumber, byte[] txHash, BigInteger txIndex, byte[] blockHash, BigInteger index, boolean removed) {
        this.address = address;
        this.topics = topics;
        this.data = data;
        this.blockNumber = blockNumber;
        this.txHash = txHash;
        this.txIndex = txIndex;
        this.blockHash = blockHash;
        this.index = index;
        this.removed = removed;
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

    public static Log readObject(ObjectReader r) {
        r.beginList();
        var address = r.readByteArray();
        var topicList = new ArrayList<byte[]>();
        while(r.hasNext())
            topicList.add(r.readByteArray());
        var topicLength = topicList.size();
        var topics = new byte[topicLength][];
        for (int i = 0; i < topicLength; i++)
            topics[i] = topicList.get(i);
        var data = r.readByteArray();
        var blockNum = r.readBigInteger();
        var txHash = r.readByteArray();
        var txIndex = r.readBigInteger();
        var blockHash = r.readByteArray();
        var index = r.readBigInteger();
        var removed = r.readBoolean();
        r.end();
        return new Log(address, topics, data, blockNum, txHash, txIndex, blockHash, index, removed);
    }
}
