import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;

import javax.crypto.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.*;
import java.util.Comparator;
import java.util.HashMap;

public class Controller {

    private static MethodsRMI implementation;
    private HashMap<String, PartnerData> communicationPartners;
    private String currentPartner = null;
    private String password = null;
    private String value = null;

    @FXML
    private ListView listView;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextArea valueTextArea;

    @FXML
    public void exitApplication() {
        // Save partnerList
        writeObjectToFile(communicationPartners, "CommunicationPartners");
        Platform.exit();
    }

    public void initialize(){

        /*
        // Search for SecureBulletinBoard
        //searchBoard();
         */

        //Check directory for necessary files
        checkFiles();

        // Search for partners
        communicationPartners = (HashMap<String, PartnerData>) readObjectFromFile("CommunicationPartners");

        // Add all partner names to listview
        ObservableList<String> partnerNames = FXCollections.observableArrayList ();
        for(String partnerName : communicationPartners.keySet())
            partnerNames.add(partnerName);

        partnerNames.sort(Comparator.comparing(String::new));
        listView.setItems(partnerNames);

        // Listen to partner changes
        listView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue)
                -> currentPartner = newValue);

    }

    public void confirmPassword(){
        //TODO: change so pasword is has to be entered before application start
        String password = passwordField.getText();

    }

    public void createPartner(){

        // TODO: implement function so both parties have input in the resulting key, index and tag
        // use given temporary files and own files to generate new indexes, tags and keys
    }

    public void bump() {

        String thisUser = "Alice";
        String partnerUser = "Bob";

        //Generate Random keys
        SecretKey secretKeyAB = generateSecretKey();
        SecretKey secretKeyBA = generateSecretKey();

        //Create random indexes
        int indexAB = generateIndex();
        int indexBA = generateIndex();

        //Create random tags
        byte[] tagAB = generateTag();
        byte[] tagBA = generateTag();

        // Create PartnerData
        PartnerData partnerData = new PartnerData(indexAB, tagAB, indexBA, tagBA);

        //Create Data for indexes and tags for communicationpartner
        communicationPartners.put(partnerUser, partnerData);

        //Save SecretKeys in Keystore for communicationpartner
        saveKeyInKeystore("CommunicationPartners.jks",partnerUser + "-sending", password, secretKeyAB);
        saveKeyInKeystore("CommunicationPartners.jks", partnerUser + "-receiving", password, secretKeyBA);

        //Create temporary files to give to partner manually
        HashMap<String, PartnerData> temporary = new HashMap<>();
        PartnerData thisUserData = new PartnerData(indexBA, tagBA, indexAB, tagAB);
        temporary.put(thisUser, thisUserData);
        writeObjectToFile(temporary, "temporary" + partnerUser);

        //Create temporary keystore to give to partner manually
        createKeystore("temporary" + partnerUser, "temporary");
        saveKeyInKeystore("temporary" + partnerUser, partnerUser + "-receiving", "temporary", secretKeyAB);
        saveKeyInKeystore("temporary" + partnerUser, partnerUser + "-sending", "temporary", secretKeyBA);

    }

    public void send() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException{

        // Get value from textArea
        value = valueTextArea.getText();

        //generate new index for next message in bulletin board (depends on size of array bulletin board)
        int nextIndex = generateIndex();

        //generate new tag to protect next message from malicious deleting
        byte[] nextTag = generateTag();

        //create Message Object with index, tag and message string
        Message message = new Message(nextIndex, nextTag, value);

        //Get symmetric key for communication partner
        SecretKey secretKey = getSecretKey("CommunicationPartners.jks", currentPartner+"-sending", password);

        //Create and initialise cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        //Encrypt object with cipher
        byte[] cipherMessage = cipher.doFinal(message.getBytes());

        //read index and tag from file for certain communication partner
        int index = communicationPartners.get(currentPartner).getSendingIndex();
        byte[] tag = communicationPartners.get(currentPartner).getSendingTag();

        //hash the tag before placing in Bulletin Board
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedTag = md.digest(tag);

        //use function add (implemented by server) to place encrypted package on specific index, associated with hashed tag
        implementation.add(index, hashedTag, cipherMessage);

        //use a key deriviation function to generate a new symmetric key from the old key
        //TODO: implement key derivation function

        //write derived key to keystore for communication partner
        saveKeyInKeystore("CommunicationPartners.jks", currentPartner+"-sending", password, secretKey);

        //replace the stored old index and tag with the new index and tag
        communicationPartners.get(currentPartner).setSendingIndex(nextIndex);
        communicationPartners.get(currentPartner).setSendingTag(nextTag);

    }

    public String receive() throws RemoteException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        //read index and tag from file for certain communication partner
        int index = communicationPartners.get(currentPartner).getReceivingIndex();
        byte[] tag = communicationPartners.get(currentPartner).getReceivingTag();

        //use function get (implemented by server) to check a specific index and tag in the bulletin board
        //(use get function with regular tag, server hashes tag to match hashed tag associated with value in Bulletin Board)
        byte[] encryptedByteArray = implementation.get(index, tag);

        //IF there is a message on that index of the Bulletin board with that specific tag (message will be returned by get function)
        if (encryptedByteArray != null){

            //Get symmetric key for communication partner
            SecretKey secretKey = getSecretKey("CommunicationPartners.jks", currentPartner+"-receiving", password);

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            //Decrypt the bytearray
            byte[] decryptedByteArray = cipher.doFinal(encryptedByteArray);

            //Convert decrypted bytearray into Message object
            Message message = Message.getInstance(decryptedByteArray);

            //IF the message is successfully decrypted by Bob
            //replace the current index and tag by the new index and tag that were piggybacked on the message
            communicationPartners.get(currentPartner).setReceivingIndex(message.getIndex());
            communicationPartners.get(currentPartner).setReceivingTag(message.getTag());

            //TODO: use a key deriviation function to generate a new symmetric key from the old key
            //use HKDF => https://github.com/patrickfav/hkdf

            //write derived key to keystore for communication partner
            saveKeyInKeystore("CommunicationPartners.jks", currentPartner+"-receiving", password, secretKey);

            //return the message
            return message.getMessage();
        }
        //Else return null => currently no message in board
        else
            return "No message";

    }

    private static SecretKey generateSecretKey(){
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();

        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static int generateIndex(){
        SecureRandom random = new SecureRandom();
        return random.nextInt(100);//size Bulletin Board
    }

    private static byte[] generateTag(){
        SecureRandom secureRandom = new SecureRandom();
        byte[] tag = new byte[8]; //choose number of bytes in tag
        secureRandom.nextBytes(tag);
        return tag;
    }

    private static void createKeystore(String keyStoreName, String pwd){

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(null, pwdArray);

            //Save Keystore to File system
            FileOutputStream fileOutputStream = new FileOutputStream(keyStoreName);
            keyStore.store(fileOutputStream, pwdArray);

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private static void saveKeyInKeystore(String keyStoreName, String partnerName, String pwd, SecretKey secretKey){
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(keyStoreName), pwdArray);

            //Save Secretkey in Keystore
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(pwdArray);
            keyStore.setEntry(partnerName, secretKeyEntry, password);

            //Save Keystore to File system
            FileOutputStream fileOutputStream = new FileOutputStream(keyStoreName);
            keyStore.store(fileOutputStream, pwdArray);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static SecretKey getSecretKey(String keyStoreName, String partnername, String pwd){
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(keyStoreName), pwdArray);
            SecretKey secretKey = (SecretKey) keyStore.getKey(partnername, pwdArray);

            return secretKey;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static void writeObjectToFile(Object object, String filename){
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename))) {
            objectOutputStream.writeObject(object);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Object readObjectFromFile(String filename){
        Object object = null;
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filename))){
            object = objectInputStream.readObject();
        } catch (Exception e){
            e.printStackTrace();
        }
        return object;
    }

    private static void searchBoard(){

        try{
            // fire to localhost port 1099
            Registry myRegistry= LocateRegistry.getRegistry("localhost", 1099);
            //TODO read in from file for more flexibility

            // search for SecureBulletinBoard
            implementation = (MethodsRMI) myRegistry.lookup("SecureBulletinBoard");

            //call methods implemented by server with: implementation.methodname();

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static void checkFiles(){

        //Check for existing partnerfile
        Object object = null;
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("CommunicationPartners"))){
            object = objectInputStream.readObject();
        } catch (FileNotFoundException e){
            HashMap<String, PartnerData> hashMap = new HashMap<>();
            writeObjectToFile(hashMap, "CommunicationPartners");
            System.out.println("Created new empty partnerfile");
        } catch (Exception e){
            e.printStackTrace();
        }

        //Check for existing keystore
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //Load keystore
            char[] pwdArray = "wrongPassword".toCharArray();
            keyStore.load(new FileInputStream("CommunicationPartners.jks"), pwdArray);
        } catch (FileNotFoundException e) {
            createKeystore("CommunicationPartners.jks", "temporary");
            //TODO: avoid using password "temporary" => make user enter password first
            System.out.println("Created keystore with password 'temporary'");
        } catch (IOException e) {
            //wrong password causes IOException
        } catch (Exception e){
            e.printStackTrace();
        }



    }
}
