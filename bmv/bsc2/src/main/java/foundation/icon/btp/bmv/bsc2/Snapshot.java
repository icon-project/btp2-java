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
    private final int currTurnLength;
    private final int nextTurnLength;

    public Snapshot(Hash hash, BigInteger number, Validators validators,
            Validators candidates, Validators voters, EthAddresses recents,
            VoteAttestation attestation, int currTurnLength, int nextTurnLength) {

        this.hash = hash;
        this.number = number;
        // ensure the list of validators in ascending order
        this.validators = validators;
        // ensure the list of validators in ascending order
        this.candidates = candidates;
        this.voters = voters;
        this.recents = recents;
        this.attestation = attestation;
        this.currTurnLength = currTurnLength;
        this.nextTurnLength = nextTurnLength;
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
        w.write(o.currTurnLength);
        w.write(o.nextTurnLength);
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
        int currTurnLength = r.readOrDefault(Integer.class, Header.DEFAULT_TURN_LENGTH);
        int nextTurnLength = r.readOrDefault(Integer.class, Header.DEFAULT_TURN_LENGTH);
        r.end();
        return new Snapshot(hash, number, validators, candidates, voters, recents, attestation,
                currTurnLength, nextTurnLength);
    }

    public boolean inturn(EthAddress validator) {
        BigInteger offset = number.add(BigInteger.ONE).divide(BigInteger.valueOf(currTurnLength))
            .mod(BigInteger.valueOf(validators.size()));
        return validators.getAddresses().get(offset.intValue()).equals(validator);
    }

    public Snapshot apply(ChainConfig config, Header head) {
        BigInteger newNumber = head.getNumber();
        Context.require(number.longValue() + 1L == newNumber.longValue()
                && hash.equals(head.getParentHash()), "Inconsistent block number");
        Context.require(hash.equals(head.getParentHash()), "Inconsistent block hash");

        // ensure the coinbase is sealer
        EthAddress sealer = head.getCoinbase();
        Context.require(validators.contains(sealer), "UnauthorizedValidator");

        EthAddresses newRecents = new EthAddresses(recents);
        if (newRecents.size() >= getMinerHistoryLength() + 1) {
            newRecents.remove(0);
        }

        Context.require(newRecents.count(sealer) < currTurnLength, "RecentlySigned");
        newRecents.add(sealer);

        VoteAttestation newAttestation = head.getVoteAttestation(config);
        if (newAttestation != null) {
            Hash target = newAttestation.getVoteRange().getTargetHash();
            Context.require(target.equals(head.getParentHash()), "Invalid attestation, target mismatch");
        } else {
            newAttestation = attestation;
        }

        int newCurrTurnLength = currTurnLength;
        int newNextTurnLength = nextTurnLength;
        Validators newValidators = validators;
        Validators newCandidates = candidates;
        Validators newVoters = voters;
        if (newNumber.longValue() % config.Epoch == 0) {
            newNextTurnLength = head.getTurnLength(config);
            newCandidates = head.getValidators(config);
        }
        if (newNumber.longValue() % config.Epoch == getMinerHistoryLength()) {
            newCurrTurnLength = nextTurnLength;
            newValidators = candidates;

            if (config.isBohr(head.getTime())) {
                // BEP-404: Clear Miner History when switching validators set
                newRecents.clear();
            } else {
                // If the number of current validators is less than the number of previous validators,
                // the capacity of the recent signers should be adjusted
                int limit = Utils.calcMinerHistoryLength(newValidators.size(), newCurrTurnLength) + 1;
                for (int i = 0; i < newRecents.size() - limit; i++) {
                    newRecents.remove(i);
                }
            }
        }
        if (newNumber.longValue() % config.Epoch == (long) (voters.size() / 2 + 1) * currTurnLength) {
            newVoters = validators;
        }

        return new Snapshot(head.getHash(), newNumber, newValidators, newCandidates, newVoters,
                newRecents, newAttestation, newCurrTurnLength, newNextTurnLength);
    }

    public int getMinerHistoryLength() {
        return Utils.calcMinerHistoryLength(validators.size(), currTurnLength);
    }

    public boolean isRecentlySigned(EthAddress signer) {
        return recents.count(signer) >= currTurnLength;
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
                ", attestation=" + attestation +
                ", currTurnLength=" + currTurnLength +
                ", nextTurnLength=" + nextTurnLength +
                '}';
    }

}
