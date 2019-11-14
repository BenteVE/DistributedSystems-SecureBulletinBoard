import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class CommunicationPartners {

    //create random symmetric key
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(128);
    SecretKey SymmetricKey = keyGen.generateKey();
}
