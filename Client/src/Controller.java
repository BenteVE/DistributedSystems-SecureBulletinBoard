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

import javax.crypto.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.*;
import java.util.*;

public class Controller implements Runnable{

    private static MethodsRMI implementation;
    private HashMap<String, PartnerData> communicationPartners;
    private ObservableList<String> partnerNames;
    private String currentPartner = null;
    private String password = null;
    private String value = null;
    private boolean runReceiver = true;

    @FXML
    private ListView listView;
    @FXML
    private TextArea valueTextArea;
    @FXML
    private TextArea chatHistory;

    @FXML
    public void exitApplication() {
        
        //stop thread
        runReceiver = false;

        // Save partnerList
        writeObjectToFile(communicationPartners, "CommunicationPartners");
        writeChathistoryToFile(currentPartner);
        Platform.exit();
    }

    public void initialize(){

        /*TODO: uncomment
        // Search for SecureBulletinBoard
        //searchBoard();
         */

        //Check directory for existing keystore, make new keystore with user-given password if no exists
        password = checkExistingKeystore();

        //Check directory for necessary files
        checkPartnerFiles();

        // Search for partners
        communicationPartners = (HashMap<String, PartnerData>) readObjectFromFile("CommunicationPartners");

        // Add all partner names to listview
        partnerNames = FXCollections.observableArrayList ();
        for(String partnerName : communicationPartners.keySet())
            addToListView(partnerName);

        partnerNames.sort(Comparator.comparing(String::new));
        listView.setItems(partnerNames);

        // Listen to partner changes
        listView.getSelectionModel().selectedItemProperty().addListener((ChangeListener<String>)
                (observable, oldValue, newValue)-> {
                    if (oldValue != null)
                        writeChathistoryToFile(oldValue);
                    currentPartner = newValue;
                    readChathistory();
                });

        //start thread to receive messages
        new Thread(this).start();
    }

    @FXML
    private void initializePartner(){

        //TODO: uncomment on two-sided bump
        /*
        if(!communicationPartners.get(currentPartner).isAwaitingInitialization()){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Initializing Partner");
            alert.setHeaderText("Information:");
            alert.setContentText("Current partner is already initialized.");
            alert.showAndWait();
            return;
        }

         */

        // Open temporary keystore and add secretKeys to own keystore
        boolean fileChosen = getSecretKeysFromKeystore(password);
        if(!fileChosen)
            return;

        // Open partnerData file and add data to permanent storage
        HashMap<String, PartnerData> temporaryMap = readPartnerDataFromFile();
        if (temporaryMap != null)
            for (String name : temporaryMap.keySet()) {
                //TODO: execute two-sided bump
                communicationPartners.put(name, temporaryMap.get(name));
                partnerNames.add(name); //TODO: remove if bump is two-sided
            }
        //TODO: uncomment on two-sided bump
        // set awaitingInitialization to false
        communicationPartners.get(currentPartner).setAwaitingInitialization(false);
    }

    @FXML
    private void bump() {

        // Get thisUser's name from dialog
        String thisUser;
        TextInputDialog dialog = new TextInputDialog("Your Own Name");
        dialog.setTitle("Creating new partner");
        dialog.setHeaderText("Creating new partner");
        dialog.setContentText("Please enter your own name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent())
            thisUser = result.get();
        else
            return;

        // Get partnerUser's name from dialog
        String partnerUser;
        dialog = new TextInputDialog("Your Partner's Name");
        dialog.setTitle("Creating new partner");
        dialog.setHeaderText("Creating new partner");
        dialog.setContentText("Please enter your partner's name:");
        result = dialog.showAndWait();
        if (result.isPresent()){
            partnerUser = result.get();
            addToListView(partnerUser);
        }
        else return;

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
        createKeystore("temporary" + partnerUser + ".jks", "temporary");
        saveKeyInKeystore("temporary" + partnerUser + ".jks", partnerUser + "-receiving", "temporary", secretKeyAB);
        saveKeyInKeystore("temporary" + partnerUser + ".jks", partnerUser + "-sending", "temporary", secretKeyBA);

    }

    @FXML
    private void send() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException{

        // Can't start sending messages if partner if not initialised

        if (communicationPartners.get(currentPartner).isAwaitingInitialization()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Message not sent");
            alert.setHeaderText("Error");
            alert.setContentText("You can't send messages when the partner is not initialized.");
            alert.showAndWait();
            return;
        }

        // Get value from textArea
        value = valueTextArea.getText();
        valueTextArea.setText("");

        // Write value to chatHistory
        chatHistory.appendText("You :" + value + "\n");

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

    private void receive(String currentPartner) throws RemoteException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (currentPartner == null)
            return;

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

            //add message to chat
            chatHistory.appendText(currentPartner + ": " + value + "\n");
        }

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
            //TODO: implement wrong file chosen
            return null;
        }
    }

    private static boolean getSecretKeysFromKeystore(String password){

        //Give info to user
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Choosing keyfile created by partner");
        alert.setHeaderText("Filechooser information");
        alert.setContentText("Use the following filechooser to choose the keystore file created in the bump of your partner");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();

        // Set extension filter
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter keystoreFilter = new FileChooser.ExtensionFilter("Keystore files", "*.jks*");
        fileChooser.getExtensionFilters().add(keystoreFilter);
        File file = fileChooser.showOpenDialog(new Stage());

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //Load temporary keystore
            char[] pwdArray = "temporary".toCharArray();
            keyStore.load(new FileInputStream(file), pwdArray);
            //cut 'temporary' and '.jks' from string
            String partnerName = file.getName().substring(9, file.getName().length()-4);

            // TODO: implement function so both parties have input in the resulting key

            // Add keys to permanent keystore
            saveKeyInKeystore("CommunicationPartners.jks", partnerName, password,
                    (SecretKey) keyStore.getKey(partnerName+"-receiving", pwdArray));
            saveKeyInKeystore("CommunicationPartners.jks", partnerName, password,
                    (SecretKey) keyStore.getKey(partnerName+"-receiving", pwdArray));

            return true;

        }
        catch (NullPointerException e){
            return false;
        } catch (Exception e){
            //TODO: implement wrong file chosen
            e.printStackTrace();
        }

        return false;
    }

    private static HashMap<String, PartnerData> readPartnerDataFromFile(){

        //Give info to user
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Choosing partner information created by partner");
        alert.setHeaderText("Filechooser information");
        alert.setContentText("Use the following filechooser to choose the partner information file created in the bump of your partner");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();

        // Set extension filter
        // FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("AVI files (*.avi)", "*.avi");
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter partnerDataFilter = new FileChooser.ExtensionFilter("All files", "*.*");
        fileChooser.getExtensionFilters().add(partnerDataFilter);
        File file = fileChooser.showOpenDialog(new Stage());

        // TODO: implement function so both parties have input in the resulting index and tag
        HashMap<String, PartnerData> temporaryMap = null;
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))){
            temporaryMap = (HashMap<String, PartnerData>) objectInputStream.readObject();
            return temporaryMap;
        } catch (NullPointerException e){
            return null;
        } catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }

    private static void writeObjectToFile(Object object, String filename){
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename))) {
            objectOutputStream.writeObject(object);
        } catch (Exception e) {
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

    private static void checkPartnerFiles() {

        //Check for existing partnerfile
        Object object = null;
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("CommunicationPartners"))) {
            object = objectInputStream.readObject();
        } catch (FileNotFoundException e) {
            HashMap<String, PartnerData> hashMap = new HashMap<>();
            writeObjectToFile(hashMap, "CommunicationPartners");
            System.out.println("Created new empty partnerfile");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String checkExistingKeystore(){

        String password = null;

        try{
            //Check for existing keystore
            FileInputStream fileInputStream = new FileInputStream("CommunicationPartners.jks");

            // if keystore exists, ask password for input
            KeyStore keyStore = KeyStore.getInstance("JKS");

            // if no keystore is found, create new keystore with password given by user
            // Create the custom dialog.
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Login in existing keystore");
            dialog.setHeaderText("Enter password for the keystore");

            // Set the button types.
            ButtonType loginButtonType = new ButtonType("Confirm Password", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            // Create the username and password labels and fields.
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");

            grid.add(new Label("Password:"), 0, 1);
            grid.add(passwordField, 1, 1);

            // Enable/Disable login button depending on whether a password was entered.
            Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
            loginButton.setDisable(true);

            // Do some validation (using the Java 8 lambda syntax).
            passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
                loginButton.setDisable(newValue.trim().isEmpty());
            });

            dialog.getDialogPane().setContent(grid);

            // Request focus on the username field by default.
            Platform.runLater(passwordField::requestFocus);

            // Convert the result to a String when the login button is clicked.
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return passwordField.getText();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();

            if(result.isPresent()){
                password = result.get();
                //Load keystore
                char[] pwdArray = password.toCharArray();
                try {
                    keyStore.load(fileInputStream, pwdArray);
                } catch (IOException e) {
                    // In case of Wrong Password
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Login in existing keystore");
                    alert.setHeaderText("Entered wrong password");
                    alert.setContentText("Try again");
                    alert.showAndWait();
                    checkExistingKeystore();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Login in existing keystore");
                alert.setHeaderText("Warning");
                alert.setContentText("If you continue without password you cannot send or receive messages.");
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

                Optional<ButtonType> resultConfirmation = alert.showAndWait();
                if (resultConfirmation.get() == ButtonType.OK){
                    //user pressed OK => continue to application
                } else {
                    checkExistingKeystore();
                }
            }

        } catch (FileNotFoundException e) {

            // if no keystore is found, create new keystore with password given by user
            // Create the custom dialog.
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Creating new keystore");
            dialog.setHeaderText("Enter password for new keystore");

            // Set the button types.
            ButtonType loginButtonType = new ButtonType("Confirm Password", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            // Create the username and password labels and fields.
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");

            grid.add(new Label("Password:"), 0, 1);
            grid.add(passwordField, 1, 1);

            // Enable/Disable login button depending on whether a password was entered.
            Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
            loginButton.setDisable(true);

            // Do some validation (using the Java 8 lambda syntax).
            passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
                loginButton.setDisable(newValue.trim().isEmpty());
            });

            dialog.getDialogPane().setContent(grid);

            // Request focus on the username field by default.
            Platform.runLater(passwordField::requestFocus);

            // Convert the result to a String when the login button is clicked.
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return passwordField.getText();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();

            if(result.isPresent()){
                password = result.get();
                createKeystore("CommunicationPartners.jks", password);
                System.out.println("Created keystore with new password");
            }
            else {
                // In case of Wrong Password
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Creating new Keystore");
                alert.setHeaderText("Warning");
                alert.setContentText("Can't create new keystore without password");
                alert.showAndWait();
                checkExistingKeystore();
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            return password;
        }
    }

    private void writeChathistoryToFile(String partner) {
        ObservableList<CharSequence> chatHistoryParagraphs = chatHistory.getParagraphs();
        Iterator<CharSequence>  iterator = chatHistoryParagraphs.iterator();
        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(partner + ".txt"));
            while(iterator.hasNext()){
                CharSequence seq = iterator.next();
                bufferedWriter.append(seq);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (FileNotFoundException e){
            //create empty file
            File file = new File(partner + ".txt");
            writeChathistoryToFile(partner);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void readChathistory() {
        try{
            Scanner scanner = new Scanner(new File(currentPartner + ".txt"));
            while (scanner.hasNext()) {
                chatHistory.appendText(scanner.nextLine() + "\n");
            }
            scanner.close();
        } catch (FileNotFoundException e){
            //create empty file
            File file = new File(currentPartner + ".txt");
        }
    }

    private void addToListView(String partnerName){
        //TODO: fix for two-sided bump
        if (communicationPartners.get(partnerName).isAwaitingInitialization())
            partnerNames.add(partnerName + "(awaiting initialization)");
        else
            partnerNames.add(partnerName);
    }

    @Override
    public void run() {
        try {
            while(runReceiver){
                //test for messages from current partner every 5 seconds
                receive(currentPartner);
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
