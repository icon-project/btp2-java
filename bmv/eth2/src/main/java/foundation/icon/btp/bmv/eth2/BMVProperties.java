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
    private Address bmc;

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

    byte[] getGenesisValidatorsHash() {
        return genesisValidatorsHash;
    }

    void setGenesisValidatorsHash(byte[] genesisValidatorsHash) {
        this.genesisValidatorsHash = genesisValidatorsHash;
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
        object.setBmc(r.readAddress());
        r.end();
        return object;
    }

    public static void writeObject(ObjectWriter w, BMVProperties obj) {
        w.beginList(3);
        w.write(obj.srcNetworkID);
        w.write(obj.genesisValidatorsHash);
        w.write(obj.bmc);
        w.end();
    }
}
