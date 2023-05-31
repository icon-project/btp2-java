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
package foundation.icon.btp.bmv.bsc2;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;
import scorex.util.Collections;

import java.util.List;

public class BlockUpdate {
    private List<Header> headers;

    public BlockUpdate(List<Header> headers) {
        this.headers = Collections.unmodifiableList(headers);
    }

    public static BlockUpdate readObject(ObjectReader r) {
        List<Header> headers = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            headers.add(Header.readObject(r));
        }
        r.end();
        return new BlockUpdate(headers);
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return BlockUpdate.readObject(r);
    }

    public List<Header> getHeaders() {
        return headers;
    }

    @Override
    public String toString() {
        return "BlockUpdate{" +
                "headers=" + headers +
                '}';
    }
}
