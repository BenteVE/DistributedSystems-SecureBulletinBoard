import java.io.Serializable;
import java.util.ArrayList;

public class PartnerData implements Serializable {

    boolean awaitingInitialization;

    int sendingIndex;
    byte[] sendingTag;
    int receivingIndex;
    byte[] receivingTag;
    byte[] hashSendingIndex;
    byte[] hashSendingTag;
    byte[] hashReceivingIndex;
    byte[] hashReceivingTag;
    byte[] encryptedKeysetSend;
    byte[] encryptedKeysetReceive;

    ArrayList<String> chatHistory;

    public PartnerData(){
        awaitingInitialization = true;
        this.sendingIndex = -1;
        this.sendingTag = null;
        this.receivingIndex = -1;
        this.receivingTag = null;
        this.chatHistory = new ArrayList<>();
    }

    public PartnerData(int receivingIndex, byte[] receivingTag){
        awaitingInitialization = true;
        this.sendingIndex = -1;
        this.sendingTag = null;
        this.receivingIndex = receivingIndex;
        this.receivingTag = receivingTag;
        this.chatHistory = new ArrayList<>();
    }

    public void addToChathistory(String value){
        this.chatHistory.add(value);
    }

    public ArrayList<String> getChathistory(){
        return chatHistory;
    }

    public void setChatHistory(ArrayList<String> chatHistory) {
        this.chatHistory = chatHistory;
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

    public byte[] getHashSendingIndex() {
        return hashSendingIndex;
    }

    public void setHashSendingIndex(byte[] hashSendingIndex) {
        this.hashSendingIndex = hashSendingIndex;
    }

    public byte[] getHashSendingTag() {
        return hashSendingTag;
    }

    public void setHashSendingTag(byte[] hashSendingTag) {
        this.hashSendingTag = hashSendingTag;
    }

    public byte[] getHashReceivingIndex() {
        return hashReceivingIndex;
    }

    public void setHashReceivingIndex(byte[] hashReceivingIndex) {
        this.hashReceivingIndex = hashReceivingIndex;
    }

    public byte[] getHashReceivingTag() {
        return hashReceivingTag;
    }

    public void setHashReceivingTag(byte[] hashReceivingTag) {
        this.hashReceivingTag = hashReceivingTag;
    }

    public byte[] getEncryptedKeysetSend() {
        return encryptedKeysetSend;
    }

    public void setEncryptedKeysetSend(byte[] encryptedKeysetSend) {
        this.encryptedKeysetSend = encryptedKeysetSend;
    }

    public byte[] getEncryptedKeysetReceive() {
        return encryptedKeysetReceive;
    }

    public void setEncryptedKeysetReceive(byte[] encryptedKeysetReceive) {
        this.encryptedKeysetReceive = encryptedKeysetReceive;
    }
}


