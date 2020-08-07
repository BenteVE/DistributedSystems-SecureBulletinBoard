import com.sun.deploy.net.HttpRequest;
import com.sun.deploy.net.HttpResponse;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import sun.net.www.http.HttpClient;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.*;
import java.sql.SQLOutput;
import java.time.Duration;
import java.util.*;
import java.lang.*;

public class Controller implements Runnable{

    private static int BOARDSIZE = 25;
    private String serverIP = "localhost";

    private MethodsRMI implementation;
    private MethodsRMI sendImplementation;
    private HashMap<String, PartnerData> communicationPartners;
    private ObservableList<String> partnerNames;
    private String currentPartner = null;
    private boolean loginComplete = false;
    private String username = null;
    private String password = null;
    private String value = null;
    private boolean recover = false;
    private boolean runReceiver = false;
    private int tellerTest = 0;

    @FXML
    private ListView listView;
    @FXML
    private TextField valueTextField;
    @FXML
    private TextArea chatHistory;

    public void initialize(){

        //Check directory for existing keystore, make new keystore with user-given password if no exists
        File file = new File("CommunicationPartners.jks");
        if(file.exists()){
            login();
        }
        else {
            createAccount();
        }
        if(loginComplete) {
            File file2 = new File(username +"SK.jks");
            if(!file2.exists()){
                getStaticSecretKeyAndSafeInFile(password);
            }
            // Add all partner names to listview
            reloadListView();

            // Listen to partner changes
            listView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>)
                    (observable, oldValue, newValue) -> {
                        currentPartner = newValue;
                        System.out.println("selected " + currentPartner);

                        if (communicationPartners.get(currentPartner).isAwaitingInitialization())
                            chatHistory.setText("This partner is still awaiting initialization.");
                        else
                            displayChathistory();
                    });

            // Select first item if available
            if (!listView.getItems().isEmpty())
                listView.getSelectionModel().select(0);

            // Set the ip for the SecureBulletinBoard and connect of possible
            setServerIP();
        }
        else{
            exitApplication();
        }
    }

    @FXML public void exitApplication() {
        System.out.println("Stopping application ...");
        if(!loginComplete)
            Platform.exit();
        else{
            //stop thread
            runReceiver = false;

            encryptFiles();
            Platform.exit();
        }
    }

    @FXML private void initializePartner(){

        if(currentPartner == null){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No partner selected");
            alert.setHeaderText("Information:");
            alert.setContentText("You have to create a partner before initialization.");
            alert.showAndWait();
            return;
        }

        if(!communicationPartners.get(currentPartner).isAwaitingInitialization()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Initializing Partner");
            alert.setHeaderText("Information:");
            alert.setContentText("Current partner is already initialized.");
            alert.showAndWait();
            return;
        }

        // Open temporary keystore and add secretKeys to own keystore
        String filepath = getSecretKeyFromTemporaryKeystore(password);
        if(filepath == null) return;

        // Open partnerData file and add data to permanent storage
        HashMap<String, PartnerData> temporaryMap = getPartnerFromTemporaryFile(filepath);
        if (temporaryMap != null)
            for (String name : temporaryMap.keySet()) {
                communicationPartners.get(currentPartner).setSendingIndex(temporaryMap.get(name).getReceivingIndex());
                communicationPartners.get(currentPartner).setSendingTag(temporaryMap.get(name).getReceivingTag());
                communicationPartners.get(currentPartner).setHashSendingIndex(hashIndex(temporaryMap.get(name).getReceivingIndex()));
                communicationPartners.get(currentPartner).setHashSendingTag(hashTag(temporaryMap.get(name).getReceivingTag()));
                System.out.println("Still in initializing before setEncryptedKeyset");
                setEncryptedKeysetForSending();
                System.out.println("after encrypted keyset");
                setEncryptedKeysetForReceiving();
            }

        communicationPartners.get(currentPartner).setIpAdres(serverIP);
        // set awaitingInitialization to false
        communicationPartners.get(currentPartner).setAwaitingInitialization(false);
        displayChathistory();

    }

    private void setEncryptedKeysetForSending(){
        SecretKey secretKeyAB = getSecretKey("CommunicationPartners.jks", currentPartner + "-send", password);
        System.out.println("secretkeyAB"+secretKeyAB);
        int index = communicationPartners.get(currentPartner).getSendingIndex();
        System.out.println("index " + index);
        byte[] tag = communicationPartners.get(currentPartner).getSendingTag();
        System.out.println("tag"+tag);
        Keyset keyset = new Keyset(index, tag, secretKeyAB);
        System.out.println("tag keyset: " + keyset.getTag());
        System.out.println("encrypted keyset: " + encryptKeyset(keyset));
        communicationPartners.get(currentPartner).setEncryptedKeysetSend(encryptKeyset(keyset));
    }
    private void setEncryptedKeysetForReceiving(){
        SecretKey secretKeyBA = getSecretKey("CommunicationPartners.jks", currentPartner + "-receive", password);
        int index = communicationPartners.get(currentPartner).getReceivingIndex();
        byte[] tag = communicationPartners.get(currentPartner).getReceivingTag();
        Keyset keyset = new Keyset(index, tag, secretKeyBA);
        communicationPartners.get(currentPartner).setEncryptedKeysetReceive(encryptKeyset(keyset));
    }

    @FXML private void searchBoard(){

        try {
            //set socksproxy to tor
            //System.getProperties().put( "proxySet", "true" );
            //System.setProperty("socksProxyHost","127.0.0.1");
            //System.setProperty("socksProxyPort", "9050");


            // fire to localhost port 1099
            System.out.println(serverIP);
            Registry myRegistry = LocateRegistry.getRegistry(serverIP, 1099);
            //TODO read in from file for more flexibility

            // search for SecureBulletinBoard
            implementation = (MethodsRMI) myRegistry.lookup("SecureBulletinBoard");

            sendImplementation = implementation;

            //if not receiver thread is running, start a new thread
            if (!runReceiver) {
                runReceiver = true;
                new Thread(this).start();
            }

        } catch (ConnectException e){
            System.out.println(e);
            System.out.println("Connection to server lost");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Lost Connection to server");
            alert.setContentText("Try again later or change the host");
            alert.showAndWait();
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("Couldn't reach server ...");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Can't reach server");
            alert.setContentText("Try again later or change the host");
            alert.showAndWait();
        }
        System.out.println("gelukt");
    }

    @FXML private void bump() {

        // Get partnerUser's name from dialog
        String partnerUser;
        TextInputDialog dialog = new TextInputDialog("Your Partner's Name");
        dialog.setTitle("Creating new partner");
        dialog.setHeaderText("Creating new partner");
        dialog.setContentText("Please enter your partner's name:");
        Optional<String> result= dialog.showAndWait();
        if (result.isPresent()){
            partnerUser = result.get();
            PartnerData partnerData = new PartnerData();
            communicationPartners.put(partnerUser, partnerData);
            System.out.println("Added " + partnerUser + " to communicationpartners");
            addToListView(partnerUser);
            System.out.println("Added " + partnerUser + " to listview");
        }
        else return;

        //Generate random key for sending
        SecretKey secretKeyAB = generateSecretKey();

        //Create random index for receiving
        int indexBA = generateIndex();

        //Create random tag for receiving
        byte[] tagBA = generateTag();

        //Add data to partner
        communicationPartners.get(partnerUser).setReceivingIndex(indexBA);
        communicationPartners.get(partnerUser).setReceivingTag(tagBA);
        communicationPartners.get(partnerUser).setHashReceivingIndex(hashIndex(indexBA));
        communicationPartners.get(partnerUser).setHashReceivingTag(hashTag(tagBA));


        //Create temporary file to give to partner manually
        HashMap<String, PartnerData> temporary = new HashMap<>();
        temporary.put(username, new PartnerData(indexBA, tagBA));
        writePartnerToTemporaryFile(temporary, "bump"+username+ "To" + partnerUser);

        //Save SecretKeys in own Keystore for communicationpartner
        saveKeyInKeystore("CommunicationPartners.jks",partnerUser + "-send", password, secretKeyAB);

        //Create temporary keystore with secretkey to give to partner manually
        createKeystore("bump"+username+ "To" + partnerUser + ".jks", "temporary");
        saveKeyInKeystore("bump"+username+ "To" + partnerUser + ".jks", "temporary", "temporary", secretKeyAB);

        //Select new partner in listview
        if (!listView.getItems().isEmpty())
            listView.getSelectionModel().select(partnerUser);

    }

    @FXML private void send() throws RemoteException{

        if(currentPartner == null) return;

        // Can't start sending messages if partner if not initialised
        if (communicationPartners.get(currentPartner).isAwaitingInitialization()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Message not sent");
            alert.setHeaderText("Error");
            alert.setContentText("You can't send messages when the partner is not initialized.");
            alert.showAndWait();
            return;
        }
        boolean change = implementation.changeServer();
        System.out.println("change: " + change);
        if (change){
            TextInputDialog dialog = new TextInputDialog(serverIP);
            dialog.setTitle("Set new Server IP");
            dialog.setHeaderText("Server too busy, change server");
            dialog.setContentText("Server IP-address:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                serverIP = result.get();
                System.out.println(result.get());
                System.out.println("New server IP set");
            });
        }
        // Get value from textArea
        value = valueTextField.getText();
        valueTextField.setText("");

        //generate new index for next message in bulletin board (depends on size of array bulletin board)
        int nextIndex = generateIndex();

        //generate new tag to protect next message from malicious deleting
        byte[] nextTag = generateTag();

        //create Message Object with index, tag and message string
        Message message = new Message(nextIndex, nextTag, value, serverIP);

        //Encrypt message
        byte[] cipherMessage = encryptMessage(message);

        //read index and tag from file for certain communication partner
        int index = communicationPartners.get(currentPartner).getSendingIndex();
        byte[] tag = communicationPartners.get(currentPartner).getSendingTag();

        //controleer hash index en tag
        byte[] hashedIndex = hashIndex(index);
        byte[] hashedTag2 = hashTag(tag);
        //if corrupted index
        if(!Arrays.equals(hashedIndex,communicationPartners.get(currentPartner).getHashSendingIndex())){
            //try to recover index
            byte[] encryptedKeysetSend = communicationPartners.get(currentPartner).getEncryptedKeysetSend();
            byte[] decryptedKeysetSend = decryptKeyset(encryptedKeysetSend);
            Keyset keyset = Keyset.getInstance(decryptedKeysetSend);
            index = keyset.getIndex();
            communicationPartners.get(currentPartner).setSendingIndex(index);
            hashedIndex = hashIndex(index);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Corrupt index");
            alert.setHeaderText("Recovery started");
            alert.setContentText("Corrupt index, recovery started!");
            alert.showAndWait();
        }
        //if corrupted tag
        if(!Arrays.equals(hashedTag2,communicationPartners.get(currentPartner).getHashSendingTag())){
            //try to recover index
            byte[] encryptedKeysetSend = communicationPartners.get(currentPartner).getEncryptedKeysetSend();
            byte[] decryptedKeysetSend = decryptKeyset(encryptedKeysetSend);
            Keyset keyset = Keyset.getInstance(decryptedKeysetSend);
            tag = keyset.getTag();
            communicationPartners.get(currentPartner).setSendingTag(tag);
            hashedTag2 = hashTag(tag);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Corrupt tag");
            alert.setHeaderText("Recovery started");
            alert.setContentText("Corrupt tag, recovery started!");
            alert.showAndWait();
        }
        /*else{
            //enkel voor te testen
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("succes");
            alert.setHeaderText("Correct test");
            alert.setContentText("succes");
            alert.showAndWait();
        }*/

        //calculate hash index and tag
        communicationPartners.get(currentPartner).setHashSendingIndex(hashIndex(nextIndex));
        communicationPartners.get(currentPartner).setHashSendingTag(hashTag(nextTag));

        //hash the tag before placing in Bulletin Board
        byte[] hashedTag = hashTag(tag);

        try{
            //use function add (implemented by server) to place encrypted package on specific index, associated with hashed tag
            sendImplementation.add(index, hashedTag, cipherMessage);

            //update to chatHistory
            updateChathistory(value, "send");

            deriveKey("send");

            //replace the stored old index and tag with the new index and tag
            communicationPartners.get(currentPartner).setSendingIndex(nextIndex);
            communicationPartners.get(currentPartner).setSendingTag(nextTag);

            setEncryptedKeysetForSending();

        }catch(NullPointerException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Message not sent");
            alert.setContentText("Server could not be reached");
            alert.showAndWait();
        }
        if(change){
            //receiveOnce();
            changeServer();
        }
    }

    private void receive() throws RemoteException {
        if (currentPartner == null) return;

        //read index and tag from file for certain communication partner
        int index = communicationPartners.get(currentPartner).getReceivingIndex();
        byte[] tag = communicationPartners.get(currentPartner).getReceivingTag();

        //test hash
        byte[] hashedIndex = hashIndex(index);
        byte[] hashedTag2 = hashTag(tag);
        if(!Arrays.equals(hashedIndex,communicationPartners.get(currentPartner).getHashReceivingIndex())){
            //corrupted index
            System.out.println("corrupted index");
            updateChathistory("Corruption detected, recovery started", "send");
            //valueTextField.setText("Stop sending, corruption detected in index");

            //try to recover index
            byte[] encryptedKeysetReceive = communicationPartners.get(currentPartner).getEncryptedKeysetReceive();
            byte[] decryptedKeysetReceive = decryptKeyset(encryptedKeysetReceive);
            Keyset keyset = Keyset.getInstance(decryptedKeysetReceive);
            index = keyset.getIndex();
            communicationPartners.get(currentPartner).setReceivingIndex(index);
            hashedIndex = hashIndex(index);
        }
        if(!Arrays.equals(hashedTag2,communicationPartners.get(currentPartner).getHashReceivingTag())){
            System.out.println("foute tag");
            updateChathistory("Corruption detected, recovery started", "send");
            //valueTextField.setText("Stop sending, corruption detected in tag");

            //try to recover tag
            byte[] encryptedKeysetReceive = communicationPartners.get(currentPartner).getEncryptedKeysetReceive();
            byte[] decryptedKeysetReceive = decryptKeyset(encryptedKeysetReceive);
            Keyset keyset = Keyset.getInstance(decryptedKeysetReceive);
            tag = keyset.getTag();
            communicationPartners.get(currentPartner).setReceivingTag(tag);
            hashedTag2 = hashTag(tag);
        }

        //use function get (implemented by server) to check a specific index and tag in the bulletin board
        //(use get function with regular tag, server hashes tag to match hashed tag associated with value in Bulletin Board)
        byte[] encryptedByteArray = implementation.get(index, tag);
        System.out.println(encryptedByteArray);
        if (encryptedByteArray != null) {
            System.out.println("in encrypted byte array");

            //IF there is a message on that index of the Bulletin board with that specific tag (message will be returned by get function)
            //Decrypt bytearray and Convert into Message object
            byte[] decryptedMessage = decryptMessage(encryptedByteArray);
            //if message could be decrypted
            if(decryptedMessage!=null){
                Message message = Message.getInstance(decryptedMessage);
                if(!communicationPartners.get(currentPartner).getIpAdres().equals(message.getNewIp())){
                    System.out.println("ipadres veranderd: "+ message.getNewIp());
                    communicationPartners.get(currentPartner).setIpAdres(message.getNewIp());
                    //implementationSend wijzigen
                    try{
                        Registry partnerRegistry = LocateRegistry.getRegistry(communicationPartners.get(currentPartner).getIpAdres(), 1099);
                        sendImplementation = (MethodsRMI) partnerRegistry.lookup("SecureBulletinBoard");
                    }catch (ConnectException e){
                        System.out.println(e);
                        System.out.println("Connection to server lost");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Lost Connection to server");
                        alert.setContentText("Try again later or change the host");
                        alert.showAndWait();
                    } catch(Exception e) {
                        System.out.println(e);
                        System.out.println("Couldn't reach server ...");
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Can't reach server");
                        alert.setContentText("Try again later or change the host");
                        alert.showAndWait();
                    }

                }else{
                    System.out.println("ipadres uit message niet veranderd: "+ message.getNewIp());
                }

                value = message.getMessage();

                //IF the message is successfully decrypted by Bob
                //replace the current index and tag by the new index and tag that were piggybacked on the message
                communicationPartners.get(currentPartner).setReceivingIndex(message.getIndex());
                communicationPartners.get(currentPartner).setReceivingTag(message.getTag());

                //calculate new hash index and tag
                communicationPartners.get(currentPartner).setHashReceivingIndex(hashIndex(message.getIndex()));
                communicationPartners.get(currentPartner).setHashReceivingTag(hashTag(message.getTag()));

                deriveKey("receive");
            }

            setEncryptedKeysetForReceiving();

            //update chathistory
            updateChathistory(value, "receive");

            //receive again to get a cluster of messages faster on startup
            receive();

        }
    }

    private void changeServer(){
        try{
            Registry registry = LocateRegistry.getRegistry(serverIP, 1099);
            implementation = (MethodsRMI) registry.lookup("SecureBulletinBoard");
        }catch (ConnectException e){
            System.out.println(e);
            System.out.println("Connection to server lost");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Lost Connection to server");
            alert.setContentText("Try again later or change the host");
            alert.showAndWait();
        } catch(Exception e) {
            System.out.println(e);
            System.out.println("Couldn't reach server ...");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Can't reach server");
            alert.setContentText("Try again later or change the host");
            alert.showAndWait();
        }
    }

    private byte[] hashTag(byte[] tag){
        try{
            //System.out.println("Tag" + tag);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashedTag = messageDigest.digest(tag);
            return hashedTag;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    //TODO test hash en test recovery door recovery te plaatsen in code
    private byte[] hashIndex(int index){
        try{
            byte bindex = (byte) index;
            byte[] byteArray = new byte[]{bindex};
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashedIndex = messageDigest.digest(byteArray);
            //System.out.println("hashedIndex" + hashedIndex);
            return hashedIndex;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        /*
        try{
            System.out.println(index);
            Integer s_int = new Integer(index);
            System.out.println("index" + s_int);;
            // Returning a hash code value for this object
            int hashedIndex = s_int.hashCode();
            System.out.println("hashedIndex" + hashedIndex);
            return hashedIndex;
        }
        catch (Exception e){
            e.printStackTrace();
            return -1;
        }

         */
    }

    private void deriveKey(String origin){
        try{
            //Get symmetric key for communication partner
            System.out.println("Deriving: " + currentPartner + "-" + origin);
            SecretKey secretKey = getSecretKey("CommunicationPartners.jks", currentPartner + "-" + origin, password);

            //use a key deriviation function to generate a new symmetric key from the old key
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] secretKeyByteArray = md.digest(secretKey.getEncoded());
            SecretKey newSecretKey = new SecretKeySpec(secretKeyByteArray, 0, secretKeyByteArray.length, "AES");

            //write derived key to keystore for communication partner
            saveKeyInKeystore("CommunicationPartners.jks", currentPartner + "-" + origin, password, newSecretKey);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private byte[] encryptMessage(Message message){

        try{
            //Get symmetric key for communication partner
            SecretKey secretKey = getSecretKey("CommunicationPartners.jks", currentPartner+"-send", password);

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            //Encrypt object with cipher
            byte[] cipherMessage = cipher.doFinal(message.getBytes());

            return cipherMessage;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    private byte[] encryptKeyset(Keyset keyset){
        System.out.println("in encrypted keyset");

        try{
            //Get symmetric key for communication partner
            SecretKey secretKey = getStaticSecretKey(username +"SK.jks", username, password);

            System.out.println("keyset.getIndex" + keyset.getIndex());
            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
           // System.out.println("keyset.getKey: "+ keyset.getByteEKey());
            //System.out.println("keyset.getBytes: " + keyset.getBytes());
            //Encrypt object with cipher
            byte[] bytesKeyset = keyset.getBytes();
            //System.out.println("bytesKeyset: " + bytesKeyset);
            byte[] cipherMessage = cipher.doFinal(keyset.getBytes());
            //System.out.println("cipherMessage: " +cipherMessage);
            return cipherMessage;
        }
        catch (Exception e){
            System.out.println("in exception getBytes()");
            e.printStackTrace();
            return null;
        }

    }

    private byte[] decryptMessage(byte[] encryptedMessage){
        try{
            //Get symmetric key for communication partner
            SecretKey secretKey = getSecretKey("CommunicationPartners.jks", currentPartner+"-receive", password);

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            //Encrypt object with cipher
            byte[] decryptedMessage = cipher.doFinal(encryptedMessage);

            recover = false;
            return decryptedMessage;
        }
        catch (Exception e){
            if(recover) {
                updateChathistory("Corruption detected, please inform sending partner", "send");
                valueTextField.setText("Stop sending, corruption detected");
                return null;
            }else{
                //Try to recover correct key

                byte[] encryptedKeysetReceive = communicationPartners.get(currentPartner).getEncryptedKeysetReceive();
                byte[] decryptedKeysetReceive = decryptKeyset(encryptedKeysetReceive);
                Keyset keyset = Keyset.getInstance(decryptedKeysetReceive);
                SecretKey eKey = keyset.getEKey();
                saveKeyInKeystore("CommunicationPartners.jks", currentPartner +"-receive", password, eKey);
                System.out.println("key recovered");
                recover = true;
                return null;
            }
        }
    }

    private byte[] decryptKeyset(byte[] keyset){
        try{
            //Get static key
            SecretKey staticSecretKey = getStaticSecretKey(username +"SK.jks", username, password);

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, staticSecretKey);

            //Encrypt object with cipher
            byte[] decryptedKeyset = cipher.doFinal(keyset);

            return decryptedKeyset;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static SecretKey generateSecretKey(){
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static int generateIndex(){
        SecureRandom random = new SecureRandom();
        return random.nextInt(BOARDSIZE);
    }

    private static byte[] generateTag(){
        SecureRandom secureRandom = new SecureRandom();
        byte[] tag = new byte[8]; //choose number of bytes in tag
        secureRandom.nextBytes(tag);
        return tag;
    }

    private static void createKeystore(String keyStoreName, String pwd){

        try {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

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
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

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

            System.out.println("Saved in keystore: " + partnerName);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void saveStaticKeyInKeystore(String keyStoreName, String user, String pwd, SecretKey secretKey){
        try{
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(keyStoreName), pwdArray);

            //Save Secretkey in Keystore
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(pwdArray);
            keyStore.setEntry(user, secretKeyEntry, password);

            //Save Keystore to File system
            FileOutputStream fileOutputStream = new FileOutputStream(keyStoreName);
            keyStore.store(fileOutputStream, pwdArray);

            System.out.println("Saved in keystore: " + user);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static SecretKey getSecretKey(String keyStoreName, String partnername, String pwd) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(keyStoreName), pwdArray);
            SecretKey secretKey = (SecretKey) keyStore.getKey(partnername, pwdArray);

            return secretKey;
        }
        catch (Exception e){
            return null;
        }
    }

    private static SecretKey getStaticSecretKey(String keyStoreName, String user, String pwd) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            //System.out.println("keystore " + keyStore);
            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(keyStoreName), pwdArray);
            SecretKey secretKey = (SecretKey) keyStore.getKey(user, pwdArray);
            //System.out.println("secretkey "+ secretKey);

            return secretKey;
        }
        catch (Exception e){
            return null;
        }
    }

    private String getSecretKeyFromTemporaryKeystore(String password){

        //Give info to user
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Choosing keyfile created by partner");
        alert.setHeaderText("Filechooser information");
        alert.setContentText("Use the following filechooser to choose the keystore file created in the bump of your partner. Make sure the other bump-file is in the same directory.");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();

        // Set extension filter
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter keystoreFilter = new FileChooser.ExtensionFilter("Keystore files", "*.jks*");
        fileChooser.getExtensionFilters().add(keystoreFilter);
        File file = fileChooser.showOpenDialog(new Stage());

        try {
            KeyStore keyStore = KeyStore.getInstance("JCEKS");

            //Load temporary keystore
            char[] pwdArray = "temporary".toCharArray();
            keyStore.load(new FileInputStream(file), pwdArray);

            // Add keys to permanent keystore
            saveKeyInKeystore("CommunicationPartners.jks", currentPartner + "-receive", password,
                    (SecretKey) keyStore.getKey("temporary", pwdArray));

            if(file.delete()){
                System.out.println("Deleted Temporary Keystore");
                return file.getAbsolutePath();
            }
        }
        catch (NullPointerException e){
            return null;
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private void getStaticSecretKeyAndSafeInFile(String password){

        SecretKey staticKey = generateSecretKey();

        createKeystore(username +"SK.jks", password);
        saveStaticKeyInKeystore(username +"SK.jks", username, password, staticKey);

    }

    private static HashMap<String, PartnerData> getPartnerFromTemporaryFile(String filepath){

        System.out.println(filepath);

        File file = new File(filepath.substring(0, filepath.length()-4));
        HashMap<String, PartnerData> temporaryMap;
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))){
            temporaryMap = (HashMap<String, PartnerData>) objectInputStream.readObject();
            objectInputStream.close();//TODO: file doesn't delete
            if(file.delete())
                System.out.println("Deleted Temporary file");
            return temporaryMap;
        } catch (NullPointerException e){
            System.out.println("Wrong file selected for reading partnerdata");
            return null;
        } catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }

    private static void writePartnerToTemporaryFile(HashMap<String, PartnerData> map, String filename){
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename))) {
            objectOutputStream.writeObject(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addToListView(String partnerName){
        partnerNames.add(partnerName);
    }

    private void reloadListView(){
        partnerNames = FXCollections.observableArrayList ();
        for(String partnerName : communicationPartners.keySet())
            addToListView(partnerName);

        partnerNames.sort(Comparator.comparing(String::new));
        listView.setItems(partnerNames);
    }

    private void createAccount(){
        // Create the create Account dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Creating new keystore");
        dialog.setHeaderText("Choose username and password");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameTextField = new TextField();
        usernameTextField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameTextField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        usernameTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(usernameTextField::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(usernameTextField.getText(), passwordField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if(result.isPresent()){
            username = result.get().getKey();
            password = result.get().getValue();
            //create keystore and store username as alias for random key to decrypt files
            createKeystore("CommunicationPartners.jks", password);
            saveKeyInKeystore("CommunicationPartners.jks", username, password, generateSecretKey());
            communicationPartners = new HashMap<>();
            loginComplete = true;
        }
        else{
            //show warning before closing app
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Closing application");
            alert.setContentText("Need username and password to create keystore");
            alert.showAndWait();
        }

    }

    private void login(){
        // Create the login dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login to existing keystore");
        dialog.setHeaderText("Login");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameTextField = new TextField();
        usernameTextField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameTextField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        usernameTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(usernameTextField::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(usernameTextField.getText(), passwordField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        if(result.isPresent()){
            username = result.get().getKey();
            password = result.get().getValue();

            //Try to decrypt files with username and password
            if (!decryptFiles()){
                // Wrong password
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Couldn't decrypt files");
                alert.setContentText("Wrong username or password");
                alert.showAndWait();
                login();
            }
        }
        else{
            //show warning before closing app
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Closing application");
            alert.setContentText("Can't log in without credentials");
            alert.showAndWait();
        }
    }

    private boolean decryptFiles() {
        //decrypt the file with partnerlist and all textfiles with chathistory
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("CommunicationPartners"));
            SealedObject sealedObject = (SealedObject) objectInputStream.readObject();

            // Get secretkey to decrypt files
            SecretKey secretKey = getSecretKey("CommunicationPartners.jks", username, password);

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            //Open Sealed object
            communicationPartners = (HashMap<String, PartnerData>) sealedObject.getObject(cipher);
            loginComplete = true;
            System.out.println("Decryption successful");
            return true;
        }
        catch (Exception e){
            System.out.println("Decryption failed");
            return false;
        }
    }

    private void encryptFiles(){
        System.out.println("Encrypting files...");
        try{
            // Get secretkey to decrypt files
            SecretKey secretKey = getSecretKey("CommunicationPartners.jks", username, password);

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            //Create SealedObject
            SealedObject sealedObject = new SealedObject(communicationPartners, cipher);

            //Write SealedObject to File
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("CommunicationPartners"));
            objectOutputStream.writeObject(sealedObject);
            System.out.println("Encrypting finished");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void displayChathistory(){
        chatHistory.setText("");
        for (String message : communicationPartners.get(currentPartner).getChathistory())
            chatHistory.appendText(message);
    }

    private void updateChathistory(String value, String origin){
        if(origin == "send"){
            value = "You: " + value + "\n";
            communicationPartners.get(currentPartner).addToChathistory(value);
            chatHistory.appendText(value);
        }
        else if(origin == "receive"){
            value = currentPartner + ": " + value + "\n";
            communicationPartners.get(currentPartner).addToChathistory(value);
            chatHistory.appendText(value);
        }
    }

    @FXML private void setServerIP(){
        TextInputDialog dialog = new TextInputDialog(serverIP);
        dialog.setTitle("Set Server IP");
        dialog.setHeaderText("Set the Server IP-address");
        dialog.setContentText("Server IP-address:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            serverIP = result.get();
            System.out.println(result.get());
            System.out.println("New server IP set");
            searchBoard();
        });
    }

    @Override
    public void run() {
        try {
            while(runReceiver){
                //test for messages from current partner every 5 seconds
                if (!communicationPartners.isEmpty() && !communicationPartners.get(currentPartner).isAwaitingInitialization())
                    receive();
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}