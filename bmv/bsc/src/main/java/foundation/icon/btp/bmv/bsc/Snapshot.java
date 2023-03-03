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

import foundation.icon.score.util.ArrayUtil;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.Collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class Snapshot {
    private Hash hash;
    private BigInteger number;
    private List<EthAddress> validators;
    private List<EthAddress> candidates;
    private List<EthAddress> recents;

    public Snapshot(Hash hash, BigInteger number, List<EthAddress> validators,
            List<EthAddress> candidates, List<EthAddress> recents) {
        this.hash = hash;
        this.number = number;
        this.validators = Collections.unmodifiableList(validators);
        this.candidates = Collections.unmodifiableList(candidates);
        this.recents = Collections.unmodifiableList(recents);
    }

    public static void writeObject(ObjectWriter w, Snapshot o) {
        w.beginList(5);
        w.writeNullable(o.hash);
        w.writeNullable(o.number);
        w.beginList(o.validators.size());
        for (EthAddress validator : o.validators) {
            w.writeNullable(validator);
        }
        w.end();
        w.beginList(o.candidates.size());
        for (EthAddress candidate : o.candidates) {
            w.writeNullable(candidate);
        }
        w.end();
        w.beginList(o.recents.size());
        for (EthAddress recent : o.recents) {
            w.writeNullable(recent);
        }
        w.end();
        w.end();
    }

    public static Snapshot readObject(ObjectReader r) {
        r.beginList();
        Hash hash = r.read(Hash.class);
        BigInteger number = r.readBigInteger();

        r.beginList();
        List<EthAddress> validators = new ArrayList<>();
        while(r.hasNext()) {
            validators.add(r.read(EthAddress.class));
        }
        r.end();

        r.beginList();
        List<EthAddress> candidates = new ArrayList<>();
        while(r.hasNext()) {
            candidates.add(r.read(EthAddress.class));
        }
        r.end();

        r.beginList();
        List<EthAddress> recents = new ArrayList<>();
        while(r.hasNext()) {
            recents.add(r.read(EthAddress.class));
        }
        r.end();
        r.end();
        return new Snapshot(hash, number, validators, candidates, recents);
    }

    public boolean inturn(EthAddress validator) {
        BigInteger offset = number.add(BigInteger.ONE).mod(BigInteger.valueOf(validators.size()));
        EthAddress[] vals = Arrays.copyOf(validators.toArray(), validators.size(), EthAddress[].class);
        ArrayUtil.sort(vals);
        return vals[offset.intValue()].equals(validator);
    }

    public Snapshot apply(Header head) {
        Context.require(head != null && number.add(BigInteger.ONE).equals(head.getNumber()) &&
                hash.equals(head.getParentHash()), "Inconsistent header");

        Hash newHash = head.getHash();
        BigInteger newNumber = head.getNumber();
        List<EthAddress> newValidators;
        List<EthAddress> newCandidates;
        List<EthAddress> newRecents = new ArrayList<>(recents);
        BigInteger epoch = Config.getAsBigInteger(Config.EPOCH);

        newValidators = newNumber.mod(epoch).equals(BigInteger.valueOf(validators.size() / 2))
            ? candidates
            : validators;

        newCandidates = newNumber.mod(epoch).equals(BigInteger.ZERO)
            ? head.getValidators()
            : candidates;

        newRecents.add(head.getSigner());
        if (newRecents.size() > newValidators.size() / 2) {
            newRecents = newRecents.subList(newRecents.size() - newValidators.size() / 2, newRecents.size());
        }

        return new Snapshot(newHash, newNumber, newValidators, newCandidates, newRecents);
    }

    public Hash getHash() {
        return hash;
    }

    public BigInteger getNumber() {
        return number;
    }

    public List<EthAddress> getValidators() {
        return validators;
    }

    public List<EthAddress> getCandidates() {
        return candidates;
    }

    public List<EthAddress> getRecents() {
        return recents;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "hash=" + hash +
                ", number=" + number +
                ", validators=" + validators +
                ", candidates=" + candidates +
                ", recents=" + recents +
                '}';
    }

}
