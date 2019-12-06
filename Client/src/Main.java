import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.*;
import java.security.cert.CertificateException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }


    public static void main(String[] args) {

        //launch(args);

        try{
            // fire to localhost port 1099
            Registry myRegistry= LocateRegistry.getRegistry("localhost", 1099);
            //TODO read in from file for more flexibility

            // search for SecureBulletinBoard
            MethodsRMI implementation = (MethodsRMI) myRegistry.lookup("SecureBulletinBoard");

            //call methods implemented by server with: implementation.methodname();

            //TEST:
            bump();
            send(implementation, "test");
            String testmessage = receive(implementation);
            System.out.println(testmessage);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void bump() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {

        //Generate Random key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey SymmetricKey = keyGen.generateKey();

        //Generate Random Index
        int index = generateIndex();

        //Generate Random Tag
        byte[] tag = generateTag();

        //Create File for index and tag for communication A => B
        CommunicationPartner Bob = new CommunicationPartner("Bob", index, tag);
        writeObjectToFile(Bob, "Bob");

        //Create File for index and tag for communication B => A
        CommunicationPartner Alice = new CommunicationPartner("Alice", index, tag);
        writeObjectToFile(Alice, "Alice");

        //Create Keystore for communication A => B
        createKeystore("Bob", "passwordAlice", SymmetricKey);

        //Create Keystore for communication B => A
        createKeystore("Alice", "passwordBob", SymmetricKey);

    }

    public static void send(MethodsRMI impl, String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, BadPaddingException, IllegalBlockSizeException, NotBoundException {

        String partnerName = "Bob";

        //generate new index for next message in bulletin board (depends on size of array bulletin board)
        int nextIndex = generateIndex();

        //generate new tag to protect next message from malicious deleting
        byte[] nextTag = generateTag();

        //create Message Object with index, tag and message string
        Message message = new Message(nextIndex, nextTag, value);

        //Get symmetric key for communication partner
        SecretKey secretKey = getSecretKey(partnerName, "passwordAlice");

        //Create and initialise cipher
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        //Encrypt object with cipher
        byte[] cipherMessage = cipher.doFinal(message.getBytes());

        //read index and tag from file for certain communication partner
        CommunicationPartner partner = (CommunicationPartner) readObjectFromFile(partnerName);
        int index = partner.getIndex();
        byte[] tag = partner.getTag();

        //hash the tag before placing in Bulletin Board
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedTag = md.digest(tag);

        //use function add (implemented by server) to place encrypted package on specific index, associated with hashed tag
        impl.add(index, hashedTag, cipherMessage);

        //replace the stored old index and tag with the new index and tag
        partner.setIndex(nextIndex);
        partner.setTag(nextTag);
        writeObjectToFile(partner, partnerName);

        //use a key deriviation function to generate a new symmetric key from the old key
        //TODO: implement key derivation function

        //write derived key to keystore for communication partner
        saveKeyInKeystore(partnerName, "passwordAlice", secretKey);
    }

    public static String receive(MethodsRMI impl) throws RemoteException, NoSuchPaddingException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        String partnerName = "Alice";

        //read index and tag from file for certain communication partner
        CommunicationPartner partner = (CommunicationPartner) readObjectFromFile(partnerName);
        int index = partner.getIndex();
        byte[] tag = partner.getTag();

        //use function get (implemented by server) to check a specific index and tag in the bulletin board
        //(use get function with regular tag, server hashes tag to match hashed tag associated with value in Bulletin Board)
        byte[] encryptedByteArray = impl.get(index, tag);

        //IF there is a message on that index of the Bulletin board with that specific tag (message will be returned by get function)
        if (encryptedByteArray != null){

            //Get symmetric key for communication partner
            SecretKey secretKey = getSecretKey(partnerName, "passwordBob");

            //Create and initialise cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            //Decrypt the bytearray
            byte[] decryptedByteArray = cipher.doFinal(encryptedByteArray);

            //Convert decrypted bytearray into Message object
            Message message = Message.getInstance(decryptedByteArray);

            //IF the message is successfully decrypted by Bob
            //replace the current index and tag by the new index and tag that were piggybacked on the message
            partner.setIndex(message.getIndex());
            partner.setTag(message.getTag());
            writeObjectToFile(partner, partnerName);

            //TODO: use a key deriviation function to generate a new symmetric key from the old key
            //SecretKey newSymmetricKey = new SecretKeySpec(SymmetricKey.getEncoded(), "AES");//=> wrong, just uses same key
            //use HKDF => https://github.com/patrickfav/hkdf

            //write derived key to keystore for communication partner
            saveKeyInKeystore(partnerName, "passwordBob", secretKey);

            //return the message
            return message.getMessage();
        }
        //Else return null => currently no message in board
        else
            return "No message";

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

    private static void createKeystore(String name, String pwd, SecretKey secretKey){

        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(null, pwdArray);

            //Save Secretkey in Keystore
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(pwdArray);
            keyStore.setEntry("key", secretKeyEntry, password);

            //Save Keystore to File system
            FileOutputStream fileOutputStream = new FileOutputStream(name+".jks");
            keyStore.store(fileOutputStream, pwdArray);

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private static void saveKeyInKeystore(String name, String pwd, SecretKey secretKey){
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(name + ".jks"), pwdArray);

            //Save Secretkey in Keystore
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(pwdArray);
            keyStore.setEntry("key", secretKeyEntry, password);

            //Save Keystore to File system
            FileOutputStream fileOutputStream = new FileOutputStream(name);
            keyStore.store(fileOutputStream, pwdArray);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static SecretKey getSecretKey(String name, String pwd){
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");

            //Load keystore
            char[] pwdArray = pwd.toCharArray();
            keyStore.load(new FileInputStream(name + ".jks"), pwdArray);
            SecretKey secretKey = (SecretKey) keyStore.getKey("key", pwdArray);
            return secretKey;

        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static void writeObjectToFile(Object object, String filename){
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static Object readObjectFromFile(String filename){
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object object = objectInputStream.readObject();
            objectInputStream.close();
            return object;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
