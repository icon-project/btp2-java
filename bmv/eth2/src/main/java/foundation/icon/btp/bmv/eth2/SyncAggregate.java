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


public class SyncAggregate {
    private byte[] syncCommitteeBits;
    private byte[] syncCommitteeSignature;
    private static final int BLS_SIGNATURE_LENGTH = 96;

    public SyncAggregate(byte[] syncCommitteeBits, byte[] syncCommitteeSignature) {
        this.syncCommitteeBits = syncCommitteeBits;
        this.syncCommitteeSignature = syncCommitteeSignature;
    }

    boolean[] getSyncCommitteeBits() {
        boolean[] bits = new boolean[Constants.SYNC_COMMITTEE_COUNT];
        for (int i = 0; i < syncCommitteeBits.length; i++)
            for (int j = 0; j < 8; j++)
                bits[i * 8 + j] = (syncCommitteeBits[i] & (1 << j)) != 0;
        return bits;
    }

    byte[] getSyncCommitteeSignature() {
        return syncCommitteeSignature;
    }

    static SyncAggregate deserialize(byte[] data) {
        byte[] syncCommitteeBytes = new byte[Constants.SYNC_COMMITTEE_COUNT / 8];
        byte[] syncCommitteeSignature = new byte[BLS_SIGNATURE_LENGTH];
        System.arraycopy(data, 0, syncCommitteeBytes, 0, Constants.SYNC_COMMITTEE_COUNT / 8);
        System.arraycopy(data, Constants.SYNC_COMMITTEE_COUNT / 8, syncCommitteeSignature, 0, BLS_SIGNATURE_LENGTH);
        return new SyncAggregate(syncCommitteeBytes, syncCommitteeSignature);
    }
}
