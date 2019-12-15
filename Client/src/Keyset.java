import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;

public class Keyset implements Serializable{
    int index;
    byte[] tag;
    byte[] eKey;

    public Keyset(int index, byte[] tag, SecretKey eKey){
        this.index = index;
        this.tag = tag;
        this.eKey = eKey.getEncoded();
    }



    //Method to turn object into bytearray
    public byte[] getBytes(){
        System.out.println("in keyset.getBytes()");
        System.out.println("this: "+ this);
        System.out.println(this.index);
        System.out.println(this.tag);
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.out.println("stap 1 baos gelukt");
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            System.out.println("stap 2 baos gelukt");
            oos.writeObject(this);
            System.out.println("step 3 okay");
            System.out.println("baos" + baos);
            System.out.println("baos.toByteARray" + baos.toByteArray());
            return baos.toByteArray();
        }catch (IOException ioe){
            System.err.println(ioe.getLocalizedMessage());
            return null;
        }
    }


    //Method to turn bytearray into object
    public static Keyset getInstance(byte[] sKeyset){
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(sKeyset);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            if(o instanceof Keyset)
                return (Keyset) o;
            else return null;
        }catch  (IOException ioe){
            System.err.println(ioe.getLocalizedMessage());
            return null;
        }catch (ClassNotFoundException nfe){
            System.err.println(nfe.getLocalizedMessage());
            return null;
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public byte[] getTag() {
        return tag;
    }

    public void setTag(byte[] tag) {
        this.tag = tag;
    }

    public SecretKey getEKey() {
        return new SecretKeySpec(eKey, 0, eKey.length, "AES");
    }

    public byte[] getByteEKey(){
        return this.eKey;
    }

    public void setEKey(SecretKey eKey) {
        this.eKey = eKey.getEncoded();
    }


}
