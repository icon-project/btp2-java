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

import score.Context;
import score.ObjectReader;

public class BlockUpdate {
    private final BlockHeader blockHeader;
    private final byte[] blockProof;

    public BlockUpdate(BlockHeader blockHeader, byte[] blockProof) {
        this.blockHeader = blockHeader;
        this.blockProof = blockProof;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public byte[] getBlockProof() {
        return blockProof;
    }

    public static BlockUpdate readObject(ObjectReader r) {
        r.beginList();
        var blockHeaderBytes = r.readByteArray();
        var blockHeader = BlockHeader.fromBytes(blockHeaderBytes);
        var blockProof = r.readNullable(byte[].class);
        r.end();
        return new BlockUpdate(
                blockHeader,
                blockProof
        );
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockUpdate.readObject(reader);
    }
}
