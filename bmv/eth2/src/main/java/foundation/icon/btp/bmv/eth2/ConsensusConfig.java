package foundation.icon.btp.bmv.eth2;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class ConsensusConfig {
    private BigInteger slotPerEpoch;
    private BigInteger epochsPerSyncCommitteePeriod;
    private Integer syncCommitteeSize;
    private BigInteger slotPerHistoricalRoot;

    public BigInteger getSlotPerEpoch() {
        return slotPerEpoch;
    }

    public void setSlotPerEpoch(BigInteger slotPerEpoch) {
        this.slotPerEpoch = slotPerEpoch;
    }

    public BigInteger getEpochsPerSyncCommitteePeriod() {
        return epochsPerSyncCommitteePeriod;
    }

    public void setEpochsPerSyncCommitteePeriod(BigInteger epochsPerSyncCommitteePeriod) {
        this.epochsPerSyncCommitteePeriod = epochsPerSyncCommitteePeriod;
    }

    public Integer getSyncCommitteeSize() {
        return syncCommitteeSize;
    }

    public void setSyncCommitteeSize(Integer syncCommitteeSize) {
        this.syncCommitteeSize = syncCommitteeSize;
    }

    public BigInteger getSlotPerHistoricalRoot() {
        return slotPerHistoricalRoot;
    }

    public void setSlotPerHistoricalRoot(BigInteger slotPerHistoricalRoot) {
        this.slotPerHistoricalRoot = slotPerHistoricalRoot;
    }

    public BigInteger computeEpoch(BigInteger slot) {
        return slot.divide(slotPerEpoch);
    }
    public BigInteger computeSyncCommitteePeriod(BigInteger slot) {
        return computeEpoch(slot).divide(epochsPerSyncCommitteePeriod);
    }

    public static void writeObject(ObjectWriter writer, ConsensusConfig obj) {
        obj.writeObject(writer);
    }

    public static ConsensusConfig readObject(ObjectReader reader) {
        ConsensusConfig obj = new ConsensusConfig();
        reader.beginList();
        obj.setSlotPerEpoch(reader.readNullable(BigInteger.class));
        obj.setEpochsPerSyncCommitteePeriod(reader.readNullable(BigInteger.class));
        obj.setSyncCommitteeSize(reader.readNullable(Integer.class));
        obj.setSlotPerHistoricalRoot(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(4);
        writer.writeNullable(this.getSlotPerEpoch());
        writer.writeNullable(this.getEpochsPerSyncCommitteePeriod());
        writer.writeNullable(this.getSyncCommitteeSize());
        writer.writeNullable(this.getSlotPerHistoricalRoot());
        writer.end();
    }

    public static ConsensusConfig fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ConsensusConfig.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ConsensusConfig.writeObject(writer, this);
        return writer.toByteArray();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConsensusConfig{");
        sb.append("slotPerEpoch=").append(slotPerEpoch);
        sb.append(", epochsPerSyncCommitteePeriod=").append(epochsPerSyncCommitteePeriod);
        sb.append(", syncCommitteeSize=").append(syncCommitteeSize);
        sb.append(", slotPerHistoricalRoot=").append(slotPerHistoricalRoot);
        sb.append('}');
        return sb.toString();
    }
}
