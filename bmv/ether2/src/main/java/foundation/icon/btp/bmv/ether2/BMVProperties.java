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

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class BMVProperties {
    public static final BMVProperties DEFAULT;

    static {
        DEFAULT = new BMVProperties();
    }

    private byte[] srcNetworkID;
    private byte[] genesisValidatorsHash;

    private SyncCommittee currentSyncCommittee;

    private SyncCommittee nextSyncCommittee;
    private Address bmc;
    private BeaconBlockHeader finalizedHeader;
    private byte[] etherBmc;

    private BigInteger lastMsgSeq;
    private BigInteger lastMsgSlot;

    public byte[] getSrcNetworkID() {
        return srcNetworkID;
    }

    public void setSrcNetworkID(byte[] srcNetworkID) {
        this.srcNetworkID = srcNetworkID;
    }

    Address getBmc() {
        return bmc;
    }

    void setBmc(Address bmc) {
        this.bmc = bmc;
    }

    SyncCommittee getCurrentSyncCommittee() {
        return currentSyncCommittee;
    }

    void setCurrentSyncCommittee(SyncCommittee syncCommittee) {
        this.currentSyncCommittee = syncCommittee;
    }

    public SyncCommittee getNextSyncCommittee() {
        return nextSyncCommittee;
    }

    public void setNextSyncCommittee(SyncCommittee nextSyncCommittee) {
        this.nextSyncCommittee = nextSyncCommittee;
    }

    byte[] getGenesisValidatorsHash() {
        return genesisValidatorsHash;
    }

    void setGenesisValidatorsHash(byte[] genesisValidatorsHash) {
        this.genesisValidatorsHash = genesisValidatorsHash;
    }

    BeaconBlockHeader getFinalizedHeader() {
        return finalizedHeader;
    }

    void setFinalizedHeader(BeaconBlockHeader finalizedHeader) {
        this.finalizedHeader = finalizedHeader;
    }

    public byte[] getEtherBmc() {
        return etherBmc;
    }

    public void setEtherBmc(byte[] etherBmc) {
        this.etherBmc = etherBmc;
    }

    public BigInteger getLastMsgSlot() {
        return lastMsgSlot;
    }

    public void setLastMsgSlot(BigInteger lastMsgSlot) {
        this.lastMsgSlot = lastMsgSlot;
    }

    public BigInteger getLastMsgSeq() {
        return lastMsgSeq;
    }

    public void setLastMsgSeq(BigInteger lastMsgSeq) {
        this.lastMsgSeq = lastMsgSeq;
    }

    public String getNetwork() {
        var stringSrc = new String(srcNetworkID);
        var delimIndex = stringSrc.lastIndexOf("/");
        return stringSrc.substring(delimIndex + 1);
    }

    public static BMVProperties readObject(ObjectReader r) {
        r.beginList();
        var object = new BMVProperties();
        object.setSrcNetworkID(r.readByteArray());
        object.setGenesisValidatorsHash(r.readByteArray());
        object.setCurrentSyncCommittee(r.read(SyncCommittee.class));
        object.setNextSyncCommittee(r.readNullable(SyncCommittee.class));
        object.setBmc(r.readAddress());
        object.setFinalizedHeader(r.read(BeaconBlockHeader.class));
        object.setEtherBmc(r.readByteArray());
        object.setLastMsgSlot(r.readBigInteger());
        object.setLastMsgSeq(r.readBigInteger());
        r.end();
        return object;
    }

    public static void writeObject(ObjectWriter w, BMVProperties obj) {
        w.beginList(9);
        w.write(obj.srcNetworkID);
        w.write(obj.genesisValidatorsHash);
        w.write(obj.currentSyncCommittee);
        w.writeNullable(obj.nextSyncCommittee);
        w.write(obj.bmc);
        w.write(obj.finalizedHeader);
        w.write(obj.etherBmc);
        w.write(obj.lastMsgSlot);
        w.write(obj.lastMsgSeq);
        w.end();
    }
}