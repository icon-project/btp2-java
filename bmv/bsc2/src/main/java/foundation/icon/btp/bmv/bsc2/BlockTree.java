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
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class BlockTree {

    private Hash root;
    private final Map<Hash, List<Hash>> nodes;

    public BlockTree(Hash root) {
        this.root = root;
        this.nodes = new HashMap<>();
        this.nodes.put(root, new ArrayList<>());
    }

    private BlockTree(Hash root, Map<Hash, List<Hash>> nodes) {
        this.root = root;
        this.nodes = nodes;
    }

    private static class Item {
        private final int nleaves;
        private final Hash id;

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
        List<Item> items = new ArrayList<>();
        items.add(new Item(nleaves, root));

        while(!items.isEmpty()) {
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
        return new BlockTree(root, nodes);
    }

    public static void writeObject(ObjectWriter w, BlockTree o) {
        List<Hash> children = new ArrayList<>();
        children.add(o.root);
        w.beginList(o.nodes.size());
        while (!children.isEmpty()) {
            Hash node = children.remove(0);
            List<Hash> tmp = o.nodes.get(node);
            w.write(tmp.size());
            w.write(node);
            if (!tmp.isEmpty()) {
                children.addAll(tmp);
            }
        }
        w.end();
    }

    public static BlockTree fromBytes(byte[] bytes) {
        ObjectReader r = Context.newByteArrayObjectReader("RLP", bytes);
        return BlockTree.readObject(r);
    }

    public Hash getRoot() {
        return root;
    }

    public List<Hash> getStem(Hash id) {
        List<Hash> ret = new ArrayList<>();
        if (!this.nodes.containsKey(id)) {
            return ret;
        }

        Hash target = id;
        while (!target.equals(this.root)) {
            for (Hash key : this.nodes.keySet()) {
                List<Hash> children = this.nodes.get(key);
                if (children.contains(target)) {
                    ret.add(target);
                    target = key;
                    break;
                }
            }
        }
        ret.add(this.root);

        // sorted by root to leaf
        for (int i = 0; i < ret.size()/2; i++) {
            Hash tmp = ret.get(i);
            ret.set(i, ret.get(ret.size()-1-i));
            ret.set(ret.size()-1-i, tmp);
        }
        return ret;
    }

    public void add(Hash parent, Hash node) {
        Context.require(!this.nodes.containsKey(node), "already exist node");
        Context.require(this.nodes.containsKey(parent), "no such parent node");
        List<Hash> descendants = this.nodes.get(parent);
        descendants.add(node);
        this.nodes.put(node, new ArrayList<>());
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

    public void prune(Hash until, OnRemoveListener lst) {
        if (until.equals(root)) {
            return;
        }

        List<Hash> removals = new ArrayList<>();
        removals.add(root);
        while (!removals.isEmpty()) {
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
                    lst.onRemove(removal);
                }
            }
            removals = buf;
        }
        root = until;
    }

    @Override
    public String toString() {
        return "BlockTree{" +
                "root=" + root +
                ", nodes=" + nodes +
                '}';
    }

}
