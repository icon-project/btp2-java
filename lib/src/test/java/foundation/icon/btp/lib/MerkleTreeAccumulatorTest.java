package foundation.icon.btp.lib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.Context;
import scorex.util.ArrayList;

import java.util.Arrays;

public class MerkleTreeAccumulatorTest {
    static final int HASH_DEPTH = 4;
    static final int HASH_COUNT = 1<<HASH_DEPTH;
    static private byte[][] hashes = null;

    private static final int HASH_LEN = 32;
    private static final String SHA3_256 = "sha3-256";
    private static byte[] concatAndHash(byte[] b1, byte[] b2) {
        byte[] data = new byte[HASH_LEN * 2];
        System.arraycopy(b1, 0, data, 0, HASH_LEN);
        System.arraycopy(b2, 0, data, HASH_LEN, HASH_LEN);
        return Context.hash(SHA3_256, data);
    }

    @BeforeAll
    public static void initHashes() {
        hashes = new byte[HASH_COUNT*2-1][];
        int wIndex = 0;
        for (int i=0 ; i<HASH_COUNT ; i++, wIndex++) {
            var msg = "test_hash "+i;
            hashes[wIndex] = Context.hash(SHA3_256, msg.getBytes());
        }
        var cnt = HASH_COUNT/2;
        int rIndex = 0;
        while (cnt > 0) {
            for (int i=0 ; i<cnt ; i++, wIndex++, rIndex += 2) {
                hashes[wIndex] = concatAndHash(hashes[rIndex], hashes[rIndex+1]);
            }
            cnt = cnt/2;
        }
    }

    private static byte[] getHash(int height, int offset) {
        int idx = 0;
        int cnt = HASH_COUNT;
        for (int i=0 ; i<height ; i++) {
            idx += cnt;
            cnt /= 2;
        }
        if (offset<0 || offset>=cnt) {
            throw new IllegalArgumentException("OffsetOutOfRange");
        }
        return hashes[idx+offset];
    }

    private static byte[][] getProofAt(int height, int index) {
        if (height > HASH_COUNT || index <0 || index >= height ) {
            throw new IllegalArgumentException("InvalidHeightOrIndex");
        }
        var proofs = new ArrayList<byte[]>();
        for (int i=0 ; (index|1)+1<=height ; i++, height/=2, index/=2) {
            if (index%2 == 0) {
                proofs.add(getHash(i, index+1));
            } else {
                proofs.add(getHash(i, index-1));
            }
        }
        var ret = new byte[proofs.size()][];
        for (int i=0 ; i<ret.length ; i++) {
            ret[i] = proofs.get(i);
        }
        return ret;
    }

    @Test
    public void testBasic() {
        var mta = new MerkleTreeAccumulator();
        for (int i=0 ; i<HASH_COUNT ; i++) {
            mta.add(hashes[i]);
        }
        for (int i=0 ; i<HASH_COUNT ; i++) {
            var proof = getProofAt(HASH_COUNT, i);
            mta.verify(proof, hashes[i], i, HASH_COUNT);
        }
    }

    @Test
    public void testForAllSize() {
        var mta = new MerkleTreeAccumulator();
        for (int i=0 ; i<HASH_COUNT ; i++) {
            mta.add(hashes[i]);
            for (int j=0 ; j<i+1 ; j++) {
                var proof = getProofAt((int)mta.getHeight(), j);
                try {
                    mta.verify(proof, hashes[j], j, mta.getHeight());
                } catch (MTAException ex) {
                    throw new MTAException("Invalid("+j+"/"+(i+1)+")", ex);
                }
            }
            int k = i+1;
            if (k<HASH_COUNT) {
                Assertions.assertThrows(MTAException.class, () -> {
                    var proof = getProofAt(k+1, k);
                    mta.verify(proof, hashes[k], k, k+1);
                });
            }
        }
    }

    @Test
    public void testBasicOverflow() {
        var mta = new MerkleTreeAccumulator();
        for (int i=0 ; i<HASH_COUNT ; i++) {
            mta.add(hashes[i]);
            for (int j=0 ; j<=i ; j++) {
                for (int h=i+1 ; h<=HASH_COUNT ; h++) {
                    var proof = getProofAt(h, j);
                    mta.verify(proof, hashes[j], j, h);
                }
            }
        }
    }

    @Test
    public void testVerifyWithInvalid() {
        final long OFFSET = 10;
        var mta = new MerkleTreeAccumulator(OFFSET);
        for (int i=0 ; i<HASH_COUNT ; i++) {
            mta.add(hashes[i]);
        }

        Assertions.assertDoesNotThrow(() -> {
            var proof = getProofAt(HASH_COUNT, 1);
            mta.verify(proof, hashes[1], 1+OFFSET, OFFSET+HASH_COUNT);
        });

        Assertions.assertThrows(MTAException.class, () -> {
            var proof = getProofAt(HASH_COUNT, 1);
            mta.verify(proof, hashes[0], OFFSET, OFFSET+HASH_COUNT);
        });

        Assertions.assertThrows(MTAException.class, () -> {
            var proof = getProofAt(HASH_COUNT, 1);
            mta.verify(proof, hashes[0], OFFSET-1, OFFSET+HASH_COUNT+1);
        });

        Assertions.assertThrows(MTAException.class, () -> {
            var proof = getProofAt(HASH_COUNT, 1);
            mta.verify(proof, hashes[0], OFFSET-1, OFFSET);
        });
    }

    @Test
    public void testRootSizeLimit() {
        var mta = new MerkleTreeAccumulator();
        mta.setRootSizeLimit(HASH_DEPTH);
        for (int i=0 ; i<HASH_COUNT ; i++) {
            mta.add(hashes[i]);
        }
        for (int i=0 ; i<HASH_COUNT ; i++) {
            var k = i;
            var proof = getProofAt(HASH_COUNT, k);
            if (i<HASH_COUNT/2) {
                Assertions.assertThrows(MTAException.class, () -> {
                    mta.verify(proof, hashes[k], k, HASH_COUNT);
                });
            } else {
                Assertions.assertDoesNotThrow(() -> {
                    var proof2 = Arrays.copyOf(proof, proof.length-1);
                    mta.verify(proof2, hashes[k], k, HASH_COUNT);
                });
            }
        }
    }

    private void testValuesWithReducedProofs(MerkleTreeAccumulator mta, int reduce) {
        int mod = 1<<reduce;
        for (int i=0 ; i<HASH_COUNT ; i++) {
            var k = i;
            var proof = getProofAt(HASH_COUNT, k);
            if (i<((mod-1)*HASH_COUNT)/mod) {
                Assertions.assertThrows(MTAException.class, () -> {
                    mta.verify(proof, hashes[k], k, HASH_COUNT);
                });
                Assertions.assertThrows(MTAException.class, () -> {
                    var proof2 = Arrays.copyOf(proof, proof.length-reduce);
                    mta.verify(proof2, hashes[k], k, HASH_COUNT);
                });
            } else {
                Assertions.assertDoesNotThrow(() -> {
                    var proof2 = Arrays.copyOf(proof, proof.length-reduce);
                    mta.verify(proof2, hashes[k], k, HASH_COUNT);
                });
            }
        }
    }

    private static MerkleTreeAccumulator testSerializeDeserialize(MerkleTreeAccumulator mta) {
        var bs = mta.toBytes();
        var mta2 = MerkleTreeAccumulator.fromBytes(bs);
        Assertions.assertEquals(mta.toString(), mta2.toString());
        Assertions.assertArrayEquals(bs, mta2.toBytes());
        return mta2;
    }

    @Test
    public void testSetRootSizeOnTheFly() {
        for (int l=0 ; l<HASH_COUNT ; l++) {
            var mta = new MerkleTreeAccumulator();
            for (int i=0 ; i<HASH_COUNT ; i++) {
                if (i==l) {
                    // expect Exception on setRootSizeLimit(HASH_DEPTH-1)
                    // tracking less items than 2^(HASH_DEPTH-2)
                    // after adding more than 2^(HASH_DEPTH-2) items.
                    // 2^(HASH_DEPTH-2) == 2^HASH_DEPTH / 4 == HASH_COUNT / 4
                    if (i>=(HASH_COUNT/4) && i%(HASH_COUNT/2) < (HASH_COUNT/4)) {
                        var mta2 = testSerializeDeserialize(mta);
                        Assertions.assertThrows(IllegalArgumentException.class, () -> {
                            mta2.setRootSizeLimit(HASH_DEPTH - 1);
                        });
                        Assertions.assertArrayEquals(mta2.toBytes(), mta.toBytes());
                        mta.setRootSizeLimit(HASH_DEPTH-1, true);
                    } else {
                        mta.setRootSizeLimit(HASH_DEPTH-1);
                    }
                }
                testSerializeDeserialize(mta);
                mta.add(hashes[i]);
            }
            var mta2 = testSerializeDeserialize(mta);
            testValuesWithReducedProofs(mta2, 2);
        }
    }

    @Test
    public void testSetGetRootSizeLimit() {
        final int OFFSET = 10;
        var mta = new MerkleTreeAccumulator(OFFSET);
        Assertions.assertEquals(OFFSET, mta.getOffset());
        Assertions.assertNull(mta.getRootSizeLimit());

        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            mta.setRootSizeLimit(0);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            mta.setRootSizeLimit(-1);
        });

        mta.setRootSizeLimit(4);
        Assertions.assertEquals(Integer.valueOf(4), mta.getRootSizeLimit());

        mta.setRootSizeLimit(null);
        Assertions.assertNull(mta.getRootSizeLimit());
    }
}
