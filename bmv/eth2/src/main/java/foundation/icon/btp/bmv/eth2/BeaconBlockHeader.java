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
import score.ObjectWriter;

import java.math.BigInteger;

public class BeaconBlockHeader {
    private BigInteger slot;
    private BigInteger validatorIndex;
    private byte[] parentRoot;
    private byte[] stateRoot;
    private byte[] bodyRoot;
    private static final int SLOT_LENGTH = 8;
    private static final int VALIDATOR_INDEX_LENGTH = 8;

    public BeaconBlockHeader(BigInteger slot, BigInteger validatorIndex, byte[] parentRoot, byte[] stateRoot, byte[] bodyRoot) {
        this.slot = slot;
        this.validatorIndex = validatorIndex;
        this.parentRoot = parentRoot;
        this.stateRoot = stateRoot;
        this.bodyRoot = bodyRoot;
    }

    public BigInteger getSlot() {
        return slot;
    }

    public void setSlot(BigInteger slot) {
        this.slot = slot;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public BigInteger getValidatorIndex() {
        return validatorIndex;
    }

    public void setValidatorIndex(BigInteger validatorIndex) {
        this.validatorIndex = validatorIndex;
    }

    public byte[] getParentRoot() {
        return parentRoot;
    }

    public void setParentRoot(byte[] parentRoot) {
        this.parentRoot = parentRoot;
    }

    public byte[] getBodyRoot() {
        return bodyRoot;
    }

    public void setBodyRoot(byte[] bodyRoot) {
        this.bodyRoot = bodyRoot;
    }

    byte[] getHashTreeRoot() {
        return SszUtils.merkleize(pack());
    }

    private byte[][] pack() {
        byte[][] data = new byte[5][Constants.BYTES_PER_CHUNK];
        var slotBytes = SszUtils.serialize(slot, SLOT_LENGTH);
        var indexBytes = SszUtils.serialize(validatorIndex, VALIDATOR_INDEX_LENGTH);
        System.arraycopy(slotBytes, 0, data[0], 0, slotBytes.length);
        System.arraycopy(indexBytes, 0, data[1], 0,  indexBytes.length);
        System.arraycopy(parentRoot, 0, data[2], 0,  Constants.BYTES_PER_CHUNK);
        System.arraycopy(stateRoot, 0, data[3], 0,  Constants.BYTES_PER_CHUNK);
        System.arraycopy(bodyRoot, 0, data[4], 0,  Constants.BYTES_PER_CHUNK);
        return data;
    }

    static BeaconBlockHeader deserialize(byte[] data) {
        var pos = 0;
        var slotBytes = new byte[SLOT_LENGTH];
        var validatorIndexBytes = new byte[VALIDATOR_INDEX_LENGTH];
        var parentRoot = new byte[Constants.HASH_LENGTH];
        var stateRoot = new byte[Constants.HASH_LENGTH];
        var bodyRoot = new byte[Constants.HASH_LENGTH];
        System.arraycopy(data, pos, slotBytes, 0, SLOT_LENGTH);
        pos += SLOT_LENGTH;
        System.arraycopy(data, pos, validatorIndexBytes, 0, VALIDATOR_INDEX_LENGTH);
        pos += VALIDATOR_INDEX_LENGTH;
        System.arraycopy(data, pos, parentRoot, 0, Constants.HASH_LENGTH);
        pos += Constants.HASH_LENGTH;
        System.arraycopy(data, pos, stateRoot, 0, Constants.HASH_LENGTH);
        pos += Constants.HASH_LENGTH;
        System.arraycopy(data, pos, bodyRoot, 0, Constants.HASH_LENGTH);
        var slot = SszUtils.deserializeInteger(slotBytes);
        var validatorIndex = SszUtils.deserializeInteger(validatorIndexBytes);
        return new BeaconBlockHeader(slot, validatorIndex, parentRoot, stateRoot, bodyRoot);
    }

    public static BeaconBlockHeader readObject(ObjectReader r) {
        r.beginList();
        var slot = r.readBigInteger();
        var index = r.readBigInteger();
        var parentRoot = r.readByteArray();
        var stateRoot = r.readByteArray();
        var bodyRoot = r.readByteArray();
        r.end();
        return new BeaconBlockHeader(slot, index, parentRoot, stateRoot, bodyRoot);
    }

    public static void writeObject(ObjectWriter w, BeaconBlockHeader b) {
        w.beginList(5);
        w.write(b.slot);
        w.write(b.validatorIndex);
        w.write(b.parentRoot);
        w.write(b.stateRoot);
        w.write(b.bodyRoot);
        w.end();
    }

    @Override
    public String toString() {
        return "BeaconBlockHeader{" +
                "slot=" + slot +
                ", validatorIndex=" + validatorIndex +
                ", parentRoot=" + StringUtil.toString(parentRoot) +
                ", stateRoot=" + StringUtil.toString(stateRoot) +
                ", bodyRoot=" + StringUtil.toString(bodyRoot) +
                '}';
    }
}
