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

public class LightClientHeader {
    private BeaconBlockHeader beacon;

    public LightClientHeader(BeaconBlockHeader beaconBlockHeader) {
        this.beacon = beaconBlockHeader;
    }

    public BeaconBlockHeader getBeacon() {
        return beacon;
    }

    public void setBeacon(BeaconBlockHeader beacon) {
        this.beacon = beacon;
    }

    public static LightClientHeader deserialize(byte[] data) {
        var beaconBytesLength = 112;
        var beaconBytes = new byte[beaconBytesLength];
        var offset = 4;
        System.arraycopy(data, offset, beaconBytes, 0, beaconBytesLength);
        var beacon = BeaconBlockHeader.deserialize(beaconBytes);
        return new LightClientHeader(beacon);
    }

    public static LightClientHeader readObject(ObjectReader r) {
        r.beginList();
        var beaconBlockHeader = r.read(BeaconBlockHeader.class);
        r.end();
        return new LightClientHeader(beaconBlockHeader);
    }

    public static void writeObject(ObjectWriter w, LightClientHeader header) {
        w.beginList(1);
        w.write(header.beacon);
        w.end();
    }

    @Override
    public String toString() {
        return "LightClientHeader{" +
                "beacon=" + beacon +
                '}';
    }
}
