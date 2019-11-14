import java.io.*;

public class Message implements Serializable {

    int index;
    byte[] tag;
    String message;

    //Constructor
    public Message(int index, byte[] tag, String message){
        this.index = index;
        this.tag = tag;
        this.message = message;
    }

    //Method to turn object into bytearray
    public byte[] getBytes(){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            return baos.toByteArray();
        }catch (IOException ioe){
            System.err.println(ioe.getLocalizedMessage());
            return null;
        }

    }

    //Method to turn bytearray into object
    public static Message getInstance(byte[] sMessage){
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(sMessage);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object o = ois.readObject();
            if(o instanceof Message)
                return (Message) o;
            else return null;
        }catch  (IOException ioe){
            System.err.println(ioe.getLocalizedMessage());
            return null;
        }catch (ClassNotFoundException nfe){
            System.err.println(nfe.getLocalizedMessage());
            return null;
        }
    }
}
