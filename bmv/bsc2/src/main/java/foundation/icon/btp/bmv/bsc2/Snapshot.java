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

import java.math.BigInteger;

public class Snapshot {
    private final Hash hash;
    private final BigInteger number;
    private final Validators validators;
    private final Validators candidates;
    private final Validators voters;
    private final EthAddresses recents;
    private final VoteAttestation attestation;

    public Snapshot(Hash hash, BigInteger number, Validators validators,
            Validators candidates, Validators voters, EthAddresses recents, VoteAttestation attestation) {
        this.hash = hash;
        this.number = number;
        // ensure the list of validators in ascending order
        this.validators = validators;
        // ensure the list of validators in ascending order
        this.candidates = candidates;
        this.voters = voters;
        this.recents = recents;
        this.attestation = attestation;
    }

    public static void writeObject(ObjectWriter w, Snapshot o) {
        w.beginList(6);
        w.write(o.hash);
        w.write(o.number);
        w.write(o.validators);
        w.write(o.candidates);
        w.write(o.voters);
        w.write(o.recents);
        w.write(o.attestation);
        w.end();
    }

    public static Snapshot readObject(ObjectReader r) {
        r.beginList();
        Hash hash = r.read(Hash.class);
        BigInteger number = r.readBigInteger();
        Validators validators = r.read(Validators.class);
        Validators candidates = r.read(Validators.class);
        Validators voters = r.read(Validators.class);
        EthAddresses recents = r.read(EthAddresses.class);
        VoteAttestation attestation = r.read(VoteAttestation.class);
        r.end();
        return new Snapshot(hash, number, validators, candidates, voters, recents, attestation);
    }

    public boolean inturn(EthAddress validator) {
        BigInteger offset = number.add(BigInteger.ONE).mod(BigInteger.valueOf(validators.size()));
        EthAddress[] vals = validators.getAddresses().toArray();
        return vals[offset.intValue()].equals(validator);
    }

    public Snapshot apply(ChainConfig config, Header head) {
        BigInteger newNumber = head.getNumber();
        Context.require(number.longValue() + 1L == newNumber.longValue()
                && hash.equals(head.getParentHash()), "Inconsistent block number");
        Context.require(hash.equals(head.getParentHash()), "Inconsistent block hash");


        Validators newVoters = newNumber.longValue() % config.Epoch == (voters.size() / 2) + 1
            ? validators
            : voters;

        Validators newValidators = newNumber.longValue() % config.Epoch == validators.size() / 2
                ? candidates
                : validators;

        // ensure the coinbase is sealer
        EthAddress sealer = head.getCoinbase();
        Context.require(validators.contains(sealer), "UnauthorizedValidator");

        Validators newCandidates = config.isEpoch(newNumber) ? head.getValidators(config) : candidates;
        EthAddresses newRecents = new EthAddresses(recents);
        int size = newRecents.size();
        int limit = newValidators.size() / 2;
        for (int i = 0; i < size - limit; i++) {
            newRecents.remove(i);
        }

        Context.require(!newRecents.contains(sealer), "RecentlySigned");
        newRecents.add(head.getCoinbase());
        if (newNumber.compareTo(BigInteger.valueOf(limit + 1)) < 0) {
            Context.require(newRecents.size() == newNumber.intValue(), "Invalid recents size");
        } else {
            Context.require(newRecents.size() == limit + 1, "Invalid recents size");
        }

        VoteAttestation newAttestation = head.getVoteAttestation(config);
        if (newAttestation != null) {
            Hash target = newAttestation.getVoteRange().getTargetHash();
            Context.require(target.equals(head.getParentHash()), "Invalid attestation, target mismatch");
        } else {
            newAttestation = attestation;
        }
        return new Snapshot(head.getHash(), newNumber, newValidators, newCandidates, newVoters, newRecents, newAttestation);
    }

    public Hash getHash() {
        return hash;
    }

    public BigInteger getNumber() {
        return number;
    }

    public Validators getValidators() {
        return validators;
    }

    public Validators getCandidates() {
        return candidates;
    }

    public Validators getVoters() {
        return voters;
    }

    public EthAddresses getRecents() {
        return recents;
    }

    public VoteAttestation getVoteAttestation() {
        return attestation;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "hash=" + hash +
                ", number=" + number +
                ", validators=" + validators +
                ", candidates=" + candidates +
                ", voters=" + voters +
                ", recents=" + recents +
                '}';
    }

}
