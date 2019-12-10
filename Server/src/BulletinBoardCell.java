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
    public byte[] getFromCell(byte[] tag){ //=> probleem: andere bytearray (met zelfde inhoud) => vindt het niet als key
        for(byte[] tagInBoard : cell.keySet()) {
            if (Arrays.equals(tagInBoard, tag)){
                return cell.remove(tagInBoard);
            }
        }
        return null;
    }
}
