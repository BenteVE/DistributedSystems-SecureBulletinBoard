import java.io.Serializable;

public class PartnerData implements Serializable {

    boolean awaitingInitialization;

    int sendingIndex;
    byte[] sendingTag;
    int receivingIndex;
    byte[] receivingTag;

    public PartnerData(int sendingIndex, byte[] sendingTag, int receivingIndex, byte[] receivingTag){
        awaitingInitialization = true;
        this.sendingIndex = sendingIndex;
        this.sendingTag = sendingTag;
        this.receivingIndex = receivingIndex;
        this.receivingTag = receivingTag;
    }

    public void setAwaitingInitialization(boolean awaitingInitialization) {
        this.awaitingInitialization = awaitingInitialization;
    }

    public boolean isAwaitingInitialization() { return awaitingInitialization; }

    public void setSendingIndex(int sendingIndex) {
        this.sendingIndex = sendingIndex;
    }

    public int getSendingIndex() {
        return sendingIndex;
    }

    public void setSendingTag(byte[] sendingTag) {
        this.sendingTag = sendingTag;
    }

    public byte[] getSendingTag() {
        return sendingTag;
    }

    public void setReceivingIndex(int receivingIndex) {
        this.receivingIndex = receivingIndex;
    }

    public int getReceivingIndex() {
        return receivingIndex;
    }

    public void setReceivingTag(byte[] receivingTag) {
        this.receivingTag = receivingTag;
    }

    public byte[] getReceivingTag() {
        return receivingTag;
    }
}


