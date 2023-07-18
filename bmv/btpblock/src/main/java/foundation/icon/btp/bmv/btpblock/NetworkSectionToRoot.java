/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.bmv.btpblock;

import score.ObjectReader;


public class NetworkSectionToRoot {
    static final int LEFT = 0;
    static final int RIGHT = 1;
    private final int dir;
    private final byte[] value;

    public NetworkSectionToRoot(int dir, byte[] value) {
        this.dir = dir;
        this.value = value;
    }

    public int getDir() {
        return dir;
    }

    public byte[] getValue() {
        return value;
    }

    public static NetworkSectionToRoot readObject(ObjectReader r) {
        r.beginList();
        NetworkSectionToRoot obj = new NetworkSectionToRoot(r.readInt(), r.readNullable(byte[].class));
        r.end();
        return obj;
    }
}
