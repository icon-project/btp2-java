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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class BlockTree {

    private Hash root;
    private Map<Hash, List<Hash>> nodes;

    public BlockTree(Hash root) {
        this.root = root;
        this.nodes = new HashMap<>() {{
            put(root, new ArrayList<>());
        }};
    }

    private BlockTree(Hash root, Map<Hash, List<Hash>> nodes) {
        this.root = root;
        this.nodes = nodes;
    }

    private static class Item {
        private int nleaves;
        private Hash id;

        private Item(int nleaves, Hash id) {
            this.nleaves = nleaves;
            this.id = id;
        }
    }

    public static BlockTree readObject(ObjectReader r) {
        Map<Hash, List<Hash>> nodes = new HashMap<>();

        r.beginList();
        int nleaves = r.readInt();
        Hash root = Hash.of(r.readByteArray());
        List<Item> items = new ArrayList<>() {{
            add(new Item(nleaves, root));
        }};

        while(items.size() > 0) {
            Item item = items.remove(0);
            Hash id = item.id;
            List<Hash> children = new ArrayList<>();
            for (int i = 0; i < item.nleaves; i++) {
                Item c = new Item(r.readInt(), Hash.of(r.readByteArray()));
                children.add(c.id);
                items.add(c);
            }
            nodes.put(id, children);
        }
        r.end();
        BlockTree bt = new BlockTree(root, nodes);
        return bt;
    }

    public static void writeObject(ObjectWriter w, BlockTree o) {
        List<Hash> children = new ArrayList<>() {{
            add(o.root);
        }};

        w.beginList(o.nodes.size());
        while (children.size() > 0) {
            Hash node = children.remove(0);
            List<Hash> tmp = o.nodes.get(node);
            w.write(tmp.size());
            w.write(node);
            if (tmp.size() > 0) {
                children.addAll(tmp);
            }
        }
        w.end();
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLP");
        writeObject(w, this);
        return w.toByteArray();
    }

    public static BlockTree fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return BlockTree.readObject(r);
    }

    public Hash getRoot() {
        return root;
    }

    public void add(Header head) {
        if (nodes.containsKey(head.getHash())) {
            return;
        }

        if (!nodes.containsKey(head.getParentHash())) {
            throw new NoSuchElementException("No such parent node");
        }

        List<Hash> descendants = nodes.get(head.getParentHash());
        descendants.add(head.getHash());
        nodes.put(head.getHash(), new ArrayList<>());
    }

    public interface OnRemoveListener {
        void onRemove(Hash node);
    }

    public void prune(Hash until, Function<Hash, Void> lst) {
        List<Hash> removals = new ArrayList<>() {{ add(root); }};
        while (removals.size() > 0) {
            List<Hash> buf = new ArrayList<>();
            for (Hash removal : removals) {
                List<Hash> leaves = nodes.get(removal);
                for (Hash leaf : leaves) {
                    if (!leaf.equals(until)) {
                        buf.add(leaf);
                    }
                }
                nodes.remove(removal);
                if (lst != null) {
                    lst.apply(removal);
                }
            }
            removals = buf;
        }
        root = until;
    }

    @Override
    public String toString() {
        return "BlockTrie{" +
                "root=" + root +
                ", nodes=" + nodes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BlockTree)) {
            return false;
        }
        BlockTree other = (BlockTree) o;
        return root.equals(other.root) && nodes.equals(other.nodes);
    }

}
