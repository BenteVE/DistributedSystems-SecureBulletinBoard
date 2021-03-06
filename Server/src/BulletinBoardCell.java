import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class BulletinBoardCell implements Serializable {

    //Every cell of the Board is a map with hashed tag and value
    private HashMap<byte[], byte[]> cell = new HashMap<>();

    //Method to add a value tag pair to a cell on the board
    public void addToCell(byte[] tag, byte[] value){
        cell.put(tag, value);
    }

    //Method to retrieve a value from a cell on the board
    public byte[] getFromCell(byte[] tag){
        for(byte[] tagInBoard : cell.keySet()) {
            if (Arrays.equals(tagInBoard, tag)){
                System.out.println("match found");
                System.out.println("tagInBoard "  +tagInBoard);
                System.out.println("meegegeven tag" + tag);
                return cell.remove(tagInBoard);
            }
        }
        return null;
    }

    public int getMessageAmountCell(){
        return cell.size();
    }
}
