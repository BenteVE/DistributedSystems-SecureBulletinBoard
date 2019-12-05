import java.io.Serializable;

public class CommunicationPartner implements Serializable {

    String name;
    int index;
    byte[] tag;

    public CommunicationPartner(String name, int index, byte[] tag){
        this.name = name;
        this.index = index;
        this.tag = tag;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setTag(byte[] tag) {
        this.tag = tag;
    }

    public byte[] getTag() {
        return tag;
    }
}


