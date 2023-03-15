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


import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

public class SyncCommittee {
    private byte[][] blsPublicKeys;
    private byte[] aggregatePubKey;
    private static final int BLS_PUBLIC_KEY_LENGTH = 48;

    public SyncCommittee(byte[][] blsPublicKey, byte[] aggregatePubKey) {
        this.blsPublicKeys = blsPublicKey;
        this.aggregatePubKey = aggregatePubKey;
    }

    byte[][] getBlsPublicKeys() {
        return blsPublicKeys;
    }

    static SyncCommittee deserialize(byte[] data) {
        byte[][] publicKeys = new byte[Constants.SYNC_COMMITTEE_COUNT][BLS_PUBLIC_KEY_LENGTH];
        byte[] aggregatedPubkey = new byte[BLS_PUBLIC_KEY_LENGTH];
        var pos = 0;
        for (int i = 0; i < Constants.SYNC_COMMITTEE_COUNT; i++) {
            System.arraycopy(data, pos, publicKeys[i], 0, BLS_PUBLIC_KEY_LENGTH);
            pos += BLS_PUBLIC_KEY_LENGTH;
        }
        System.arraycopy(data, pos, aggregatedPubkey, 0, BLS_PUBLIC_KEY_LENGTH);
        return new SyncCommittee(publicKeys, aggregatedPubkey);
    }

    public byte[] getHashTreeRoot() {
        var packed = pack();
        return SszUtils.merkleize(packed);
    }

    byte[] hashPublicKeys() {
        byte[][] packed = new byte[Constants.SYNC_COMMITTEE_COUNT][Constants.BYTES_PER_CHUNK];
        for (int i = 0; i < blsPublicKeys.length; i++)
            packed[i] = SszUtils.concatAndHash(blsPublicKeys[i], new byte[Constants.BYTES_PER_CHUNK / 2]);
        return SszUtils.merkleize(packed);
    }

    private byte[][] pack() {
        var packed = new byte[2][Constants.BYTES_PER_CHUNK];
        var publicKeysRoot = hashPublicKeys();
        packed[0] = publicKeysRoot;
        packed[1] = SszUtils.concatAndHash(aggregatePubKey, new byte[Constants.BYTES_PER_CHUNK / 2]);
        return packed;
    }

    public static SyncCommittee readObject(ObjectReader r) {
        r.beginList();
        var blsPubKeyList = new ArrayList<byte[]>();
        while(r.hasNext())
            blsPubKeyList.add(r.readByteArray());
        var pubKeyLen = blsPubKeyList.size();
        var pubKeys = new byte[pubKeyLen][];
        for (int i = 0; i < pubKeyLen; i++)
            pubKeys[i] = blsPubKeyList.get(i);
        var aggregatePubKey = r.readByteArray();
        r.end();
        return new SyncCommittee(pubKeys, aggregatePubKey);
    }

    public static void writeObject(ObjectWriter w, SyncCommittee syncCommittee) {
        w.beginList(2);
        w.beginList(syncCommittee.blsPublicKeys.length);
        for (byte[] publicKey: syncCommittee.blsPublicKeys)
            w.write(publicKey);
        w.end();
        w.write(syncCommittee.aggregatePubKey);
        w.end();
    }
}
