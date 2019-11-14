import java.util.HashMap;

public class BulletinBoardCell {

    //Every cell of the Board is a map with hashed tag and value
    private HashMap<byte[], byte[]> cell = new HashMap<byte[], byte[]>();

    //Method to add a value tag pair to a cell on the board
    public void addToCell(byte[] tag, byte[] value){
        cell.put(tag, value);
    }

    //Method to retrieve a value from a cell on the board
    public byte[] getFromCell(byte[] tag){
        return cell.remove(tag);
    }
}
