import java.util.HashMap;

public class BulletinBoard {

    private BulletinBoardCell[] board;

    //Constructor
    public BulletinBoard(int boardsize){

        board = new BulletinBoardCell[boardsize];
        for(int i = 0; i < boardsize; i++){
            board[i] = new BulletinBoardCell();
        }
    }

    //Method to add a value/tag pair to the board
    public void addToBoard(int index, byte[] tag, byte[] value){
        board[index].addToCell(tag, value);
    }

    //Method to get a value from a specific index if hashed tag is present
    public byte[] getFromBoard(int index, byte[] tag){
        return board[index].getFromCell(tag);
    }

}


