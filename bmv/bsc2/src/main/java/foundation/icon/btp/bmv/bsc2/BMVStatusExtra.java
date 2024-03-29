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

import score.ByteArrayObjectWriter;
import score.Context;

public class BMVStatusExtra {
    // mta offset
    private final long offset;
    private final BlockTree tree;

    public BMVStatusExtra(long offset, BlockTree tree) {
        this.offset = offset;
        this.tree = tree;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        w.beginList(2);
        w.write(offset);
        w.write(tree);
        w.end();
        return w.toByteArray();
    }
}
