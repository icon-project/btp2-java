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

import foundation.icon.score.util.StringUtil;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class EthAddresses {
    private List<EthAddress> addresses;

    public EthAddresses(EthAddress[] addresses) {
        this.addresses = new ArrayList<>(List.of(addresses));
    }
    public EthAddresses(List<EthAddress> addresses) {
        this.addresses = addresses;
    }

    public EthAddresses(byte[][] addresses) {
        this.addresses = new ArrayList<>();
        for (int i = 0; i < addresses.length; i++) {
            this.addresses.add(new EthAddress(StringUtil.bytesToHex(addresses[i])));
        }
    }

    public EthAddresses(EthAddresses o) {
        this.addresses = new ArrayList<>(o.addresses);
    }

    public EthAddress[] toArray() {
        EthAddress[] addresses = new EthAddress[this.addresses.size()];
        for (int i = 0; i < this.addresses.size(); i++) {
            addresses[i] = this.addresses.get(i);
        }
        return addresses;
    }

    public static EthAddresses fromString(String o) {
        List<String> tokens = StringUtil.tokenize(o, ',');
        List<EthAddress> addresses = new ArrayList<>();
        for (String token : tokens) {
            addresses.add(EthAddress.of(token));
        }
        return new EthAddresses(addresses);
    }

    public EthAddress get(int i) {
        return addresses.get(i);
    }

    public boolean contains(EthAddress address) {
        return addresses.contains(address);
    }

    public void add(EthAddress newAddress) {
        addresses.add(newAddress);
    }

    public EthAddresses subList(int from, int to) {
        return new EthAddresses(addresses.subList(from, to));
    }

    public int size() {
        return addresses.size();
    }

    public static EthAddresses readObject(ObjectReader r) {
        r.beginList();

        List<EthAddress> addresses = new ArrayList<>();
        while(r.hasNext()) {
            addresses.add(r.read(EthAddress.class));
        }
        r.end();
        return new EthAddresses(addresses);
    }

    public static void writeObject(ObjectWriter w, EthAddresses o) {
        w.beginList(o.addresses.size());
        for (EthAddress address : o.addresses) {
            w.write(address);
        }
        w.end();
    }

    public static void sort(EthAddress[] a) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            EthAddress v = a[i];
            for (int j = i+1; j < len; j++) {
                if (v.compareTo(a[j]) > 0) {
                    EthAddress t = v;
                    v = a[j];
                    a[j] = t;
                }
            }
            a[i] = v;
        }
    }
}
