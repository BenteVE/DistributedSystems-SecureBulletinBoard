import java.util.HashMap;

public class BulletinBoard {

    private static BulletinBoardCell[] board = new BulletinBoardCell[256];

    //Constructor
    public BulletinBoard(){}

    //Method to add a value/tag pair to the board
    public static void addToBoard(int index, byte[] tag, byte[] value){
        board[index].addToCell(tag, value);
    }

    //Method to get a value from a specific index if hashed tag is present
    public static byte[] getFromBoard(int index, byte[] tag){
        return board[index].getFromCell(tag);
    }

}


