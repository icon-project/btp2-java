/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.lib;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.Arrays;
import java.util.List;

public class MerkleTreeAccumulator {
    private static final int HASH_LEN = 32;

    private long height;
    private byte[][] roots;
    private long offset;
    private Integer rootSize;

    /**
     * Constructor for decoding
     */
    public MerkleTreeAccumulator() {
    }

    public MerkleTreeAccumulator(long height) {
        this.height = height;
        this.offset = height;
    }

    public long getOffset() {
        return offset;
    }

    public long getHeight() {
        return height;
    }

    private static byte[] concatAndHash(byte[] b1, byte[] b2) {
        byte[] data = new byte[HASH_LEN * 2];
        System.arraycopy(b1, 0, data, 0, HASH_LEN);
        System.arraycopy(b2, 0, data, HASH_LEN, HASH_LEN);
        return Context.hash("sha3-256", data);
    }

    private static void verify(byte[][] witness, int witnessLen, byte[] root, byte[] hash, long idx) {
        for (int i = 0; i < witnessLen; i++) {
            if (idx % 2 == 0) {
                hash = concatAndHash(hash, witness[i]);
            } else {
                hash = concatAndHash(witness[i], hash);
            }
            idx = idx / 2;
        }
        if (!Arrays.equals(root, hash)) {
            throw new MTAException("invalid witness"+
                    ", root: "+StringUtil.toString(root) + ", hash: "+StringUtil.toString(hash));
        }
    }

    public void verify(byte[][] witness, byte[] hash, long height, long at) {
        if (this.height == at) {
            byte[] root = getRoot(witness.length);
            verify(witness, witness.length, root, hash, height - offset);
        } else if (this.height < at) {
            if (this.height <= height) {
                throw new MTAException("given witness for newer node");
            }
            if (this.offset > height) {
                throw new MTAException("not allowed old witness");
            }
            int rootIdx = getRootIdxByHeight(height);
            byte[] root = getRoot(rootIdx);
            verify(witness, rootIdx, root, hash, height - offset);
        } else {
            throw new MTAException.InvalidWitnessOldException("not allowed old witness");
        }
    }

    private int getRootIdxByHeight(long height) {
        if (height < offset) {
            throw new MTAException("given height is out of range");
        }
        long idx = height -  offset;
        int rootIdx = (roots == null ? 0 : roots.length) - 1;
        while (roots != null && rootIdx >= 0) {
            if (roots[rootIdx] != null) {
                long bitFlag = 1L << rootIdx;
                if (idx < bitFlag) {
                    break;
                }
                idx -= bitFlag;
            }
            rootIdx--;
        }
        if (rootIdx < 0) {
            throw new MTAException("given height is out of range");
        }
        return rootIdx;
    }

    private byte[] getRoot(int idx) {
        if (idx < 0 || roots == null || idx >= roots.length) {
            throw new MTAException("root idx is out of range");
        } else {
            return roots[idx];
        }
    }

    private void appendRoot(byte[] hash) {
        int len = roots == null ? 0 : roots.length;
        byte[][] roots = new byte[len + 1][];
        roots[len] = hash;
        this.roots = roots;
    }

    public boolean isRootSizeLimitEnabled() {
        return rootSize != null && rootSize > 0;
    }

    public void setRootSizeLimit(Integer size) {
        this.setRootSizeLimit(size, false);
    }

    /**
     * Set root size limit
     * @param size Root size limit(null for no limits). It limits size of roots by pruning old data.
     * @param unsafe Set it as true for ignoring remaining tracked elements. Otherwise,
     *               It ensures that it tracks 2^(size-1) added elements even though it prunes old data.
     * @throws IllegalArgumentException
     */
    public void setRootSizeLimit(Integer size, boolean unsafe) {
        if (size == null) {
            this.rootSize = null;
        } else {
            if (size<1) {
                throw new IllegalArgumentException("Invalid size value");
            }
            this.updateRootsBySize(size, unsafe);
            this.rootSize = size;
        }
    }

    /**
     * Get root size limit
     */
    public Integer getRootSizeLimit() {
        return rootSize;
    }

    private void updateRootsBySize(int rootSize, boolean unsafe) {
        if (this.roots!= null && rootSize < this.roots.length) {
            int size = this.roots.length;
            long offset = this.offset;
            while (size>rootSize) {
                if (this.roots[size-1] != null) {
                    offset += (long)StrictMath.pow(2, size-1);
                }
                size -= 1;
            }
            if (!unsafe && roots[size-1] == null) {
                throw new IllegalArgumentException("No way to keep required elements");
            }
            while (size>0 && roots[size-1] == null) size--;
            byte[][] roots = new byte[size][];
            System.arraycopy(this.roots, 0, roots, 0, size);
            this.roots = roots;
            this.offset = offset;
        }
    }

    public void add(byte[] hash) {
        if (height == offset) {
            appendRoot(hash);
        } else {
            boolean isAdded = false;
            int len = roots == null ? 0 : roots.length;
            int pruningIdx = (isRootSizeLimitEnabled() ? rootSize : 0) - 1;
            for (int i = 0; i < len; i++) {
                if (roots[i] == null) {
                    roots[i] = hash;
                    isAdded = true;
                    break;
                } else {
                    if (i == pruningIdx) {
                        roots[i] = hash;
                        addOffset(i);
                        isAdded = true;
                        break;
                    } else {
                        hash = concatAndHash(roots[i], hash);
                        roots[i] = null;
                    }
                }
            }
            if (!isAdded) {
                appendRoot(hash);
            }
        }
        height++;
    }

    private void addOffset(int rootIdx) {
        long offset = (long) StrictMath.pow(2, rootIdx);
        this.offset += offset;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MerkleTreeAccumulator{");
        sb.append("height=").append(height);
        sb.append(", roots=").append(StringUtil.toString(roots));
        sb.append(", offset=").append(offset);
        sb.append(", rootSize=").append(rootSize);
        sb.append('}');
        return sb.toString();
    }


    public static void writeObject(ObjectWriter writer, MerkleTreeAccumulator obj) {
        obj.writeObject(writer);
    }

    public static MerkleTreeAccumulator readObject(ObjectReader reader) {
        MerkleTreeAccumulator obj = new MerkleTreeAccumulator();
        reader.beginList();
        obj.height = reader.readLong();
        if (reader.beginNullableList()) {
            List<byte[]> rootsList = new ArrayList<>();
            while(reader.hasNext()) {
                rootsList.add(reader.readNullable(byte[].class));
            }
            var roots = new byte[rootsList.size()][];
            for (int i=0 ; i<roots.length ;i++) {
                roots[i] = rootsList.get(i);
            }
            obj.roots = roots;
            reader.end();
        }
        obj.offset = reader.readLong();
        obj.rootSize = reader.readNullable(Integer.class);
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(4);
        writer.write(this.height);
        byte[][] roots = this.roots;
        if (roots != null) {
            writer.beginNullableList(roots.length);
            for(byte[] v : roots) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.write(this.offset);
        writer.writeNullable(this.rootSize);
        writer.end();
    }

    private static final String RLPn = "RLPn";

    public static MerkleTreeAccumulator fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, bytes);
        return MerkleTreeAccumulator.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        MerkleTreeAccumulator.writeObject(writer, this);
        return writer.toByteArray();
    }

}
