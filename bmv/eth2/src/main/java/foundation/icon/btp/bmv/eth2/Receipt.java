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
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;

public class Receipt {
    private BigInteger type;
    private byte[] postState;
    private BigInteger status;
    private BigInteger cumulativeGasUsed;
    private byte[] bloom;
    private Log[] logs;
    private byte[] txHash;
    private byte[] contractAddress;
    private BigInteger gasUsed;
    private byte[] blockHash;
    private BigInteger txIndex;

    public Receipt(BigInteger type, byte[] postState, BigInteger status, BigInteger cumulativeGasUsed, byte[] bloom, Log[] logs, byte[] txHash, byte[] contractAddress, BigInteger gasUsed, byte[] blockHash, BigInteger txIndex) {
        this.type = type;
        this.postState = postState;
        this.status = status;
        this.cumulativeGasUsed = cumulativeGasUsed;
        this.bloom = bloom;
        this.logs = logs;
        this.txHash = txHash;
        this.contractAddress = contractAddress;
        this.gasUsed = gasUsed;
        this.blockHash = blockHash;
        this.txIndex = txIndex;
    }

    Log[] getLogs() {
        return logs;
    }

    public static Receipt readObject(ObjectReader r) {
        r.beginList();
        var type = r.readBigInteger();
        var postState = r.readByteArray();
        var status = r.readBigInteger();
        var cumulativeGasUsed = r.readBigInteger();
        var bloom = r.readByteArray();
        var logList = new ArrayList<Log>();
        while(r.hasNext())
            logList.add(r.read(Log.class));
        var logsLength = logList.size();
        var logs = new Log[logsLength];
        for (int i = 0; i < logsLength; i++)
            logs[i] = logList.get(i);
        var txHash = r.readByteArray();
        var contractAddress = r.readByteArray();
        var gasUsed = r.readBigInteger();
        var blockHash = r.readByteArray();
        var txIndex = r.readBigInteger();
        r.end();
        return new Receipt(
          type, postState, status, cumulativeGasUsed, bloom, logs, txHash, contractAddress, gasUsed, blockHash, txIndex);
    }

    static Receipt fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Receipt.readObject(reader);
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "type=" + type +
                ", postState=" + StringUtil.toString(postState) +
                ", status=" + status +
                ", cumulativeGasUsed=" + cumulativeGasUsed +
                ", bloom=" + StringUtil.toString(bloom) +
                ", logs=" + StringUtil.toString(logs) +
                ", txHash=" + StringUtil.toString(txHash) +
                ", contractAddress=" + StringUtil.toString(contractAddress) +
                ", gasUsed=" + gasUsed +
                ", blockHash=" + StringUtil.toString(blockHash) +
                ", txIndex=" + txIndex +
                '}';
    }
}
