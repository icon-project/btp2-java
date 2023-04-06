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

import score.Context;

import java.math.BigInteger;
import java.util.Arrays;

public class SszUtils {

    public static byte[] getZeroHash(int index) {
        if (index == 0) return new byte[32];
        byte[][] zeroHashes = new byte[index + 1][32];
        for (int i = 1; i <= index; i++)
            zeroHashes[i] = concatAndHash(zeroHashes[index - 1], zeroHashes[index -1]);
        return zeroHashes[index];
    }

    public static byte[] serialize(BigInteger integer, int size) {
        var serialized = new byte[size];
        var src = integer.toByteArray();
        reverse(src);
        System.arraycopy(src, 0, serialized, 0, src.length);
        return serialized;
    }

    public static BigInteger deserializeInteger(byte[] data) {
        reverse(data);
        return new BigInteger(data, 0, data.length);
    }

    private static void reverse(byte[] src) {
        for (int i = 0; i < src.length / 2; i++) {
            var tmp = src[i];
            src[i] = src[src.length - 1 - i];
            src[src.length - 1 - i] = tmp;
        }
    }

    public static void verify(byte[] root, Proof proof) {
        var proofIndex = proof.getIndex();
        var depth = floorLog2(proofIndex).intValue();
        var index = proofIndex.intValue() % (1 << depth);
        validateMerkleBranch(proof.getLeaf(), proof.getHashes(), depth, index, root);
    }

    public static BigInteger floorLog2(BigInteger value) {
        var val = value.bitLength() - 1;
        return BigInteger.valueOf(val);
    }

    public static void validateMerkleBranch(
            byte[] leaf, byte[][] branch, int depth, int index, byte[] root) {
        var value = leaf;
        for (int i = 0; i < depth; i++) {
            if ((index / (1 << i)) % 2 == 1)
                value = concatAndHash(branch[i], value);
            else
                value = concatAndHash(value, branch[i]);
        }
        if (!Arrays.equals(value, root))
            throw BMVException.unknown("Invalid MerkleBranch");
    }

    public static byte[] concatAndHash(byte[] b1, byte[] b2) {
        var data = concat(b1, b2);
        return Context.hash("sha-256", data);
    }

    public static byte[] concat(byte[]... bytesArgs) {
        int len = 0, accum = 0;
        for (byte[] bytes : bytesArgs)
            len += bytes.length;

        byte[] data = new byte[len];
        for (byte[] bytes : bytesArgs) {
            System.arraycopy(bytes, 0, data, accum, bytes.length);
            accum += bytes.length;
        }
        return data;
    }

    public static int nextPow2(int x) {
        var highestOneBit = Integer.highestOneBit(x);
        if (highestOneBit == x) return highestOneBit;
        return highestOneBit * 2;
    }

    public static byte[] merkleize(byte[][] chunk) {
        var chunkCount = chunk.length;
        var size = SszUtils.nextPow2(chunkCount);
        byte[][] tree = new byte[size][Constants.BYTES_PER_CHUNK];
        System.arraycopy(chunk, 0, tree, 0, chunkCount);

        var depth = Integer.bitCount(size - 1);
        for (int i = 0; i < depth; i++) {
            int padCount = chunkCount % 2;
            int paddedChunkCount = chunkCount + padCount;

            for (int j = 0; j < padCount; j++)
                tree[chunkCount + j] = SszUtils.getZeroHash(i);

            for (int j = 0; j < paddedChunkCount; j += 2)
                tree[j / 2] = SszUtils.concatAndHash(tree[j], tree[j + 1]);

            chunkCount = paddedChunkCount / 2;
        }
        return tree[0];
    }
}
