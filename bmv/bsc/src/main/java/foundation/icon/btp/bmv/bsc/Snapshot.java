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

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Snapshot {
    private Hash hash;
    private BigInteger number;
    private EthAddresses validators;
    private EthAddresses candidates;
    private EthAddresses recents;

    public Snapshot(Hash hash, BigInteger number, EthAddresses validators,
            EthAddresses candidates, EthAddresses recents) {
        this.hash = hash;
        this.number = number;
        this.validators = validators;
        this.candidates = candidates;
        this.recents = recents;
    }

    public static void writeObject(ObjectWriter w, Snapshot o) {
        w.beginList(5);
        w.write(o.hash);
        w.write(o.number);
        w.write(o.validators);
        w.write(o.candidates);
        w.write(o.recents);
        w.end();
    }

    public static Snapshot readObject(ObjectReader r) {
        r.beginList();
        Hash hash = r.read(Hash.class);
        BigInteger number = r.readBigInteger();
        EthAddresses validators = r.read(EthAddresses.class);
        EthAddresses candidates = r.read(EthAddresses.class);
        EthAddresses recents = r.read(EthAddresses.class);
        r.end();
        return new Snapshot(hash, number, validators, candidates, recents);
    }

    public boolean inturn(EthAddress validator) {
        BigInteger offset = number.add(BigInteger.ONE).mod(BigInteger.valueOf(validators.size()));
        EthAddress[] vals = validators.toArray();
        EthAddresses.sort(vals);
        return vals[offset.intValue()].equals(validator);
    }

    public Snapshot apply(Header head) {
        Context.require(head != null && number.add(BigInteger.ONE).equals(head.getNumber()) &&
                hash.equals(head.getParentHash()), "Inconsistent header");

        Hash newHash = head.getHash();
        BigInteger newNumber = head.getNumber();
        EthAddresses newValidators;
        EthAddresses newCandidates;
        EthAddresses newRecents = new EthAddresses(recents);
        BigInteger epoch = Config.getAsBigInteger(Config.EPOCH);

        newValidators = newNumber.mod(epoch).equals(BigInteger.valueOf(validators.size() / 2))
            ? candidates
            : validators;

        newCandidates = newNumber.mod(epoch).equals(BigInteger.ZERO)
            ? new EthAddresses(head.getValidators())
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

    public EthAddresses getValidators() {
        return validators;
    }

    public EthAddresses getCandidates() {
        return candidates;
    }

    public EthAddresses getRecents() {
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
