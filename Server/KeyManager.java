package Server;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

class KeyManager {

    private static KeyPair serverKeyPair;
    private static KeyStore keyStore;
    private final char[] KEYSTORE_PASSWORD = "Th0s3whoDaR3toF@|lM15eraBlYCaN@CHi3v3greatly".toCharArray();
    private final char[] SERVER_ENTRY_PASSWORD = "S3rv3rPr1vKeY".toCharArray();
    private final String SERVER_ENTRY_ALIAS = "server";

    KeyManager(){
        try{
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, KEYSTORE_PASSWORD);
            serverKeyPair = KeyGenerator.generateNewKeyPair();

            //TODO generate self-signed cert and add to cert chain
            Certificate[] certChain = new Certificate[1];
            //add to keystore
            keyStore.setKeyEntry(SERVER_ENTRY_ALIAS,serverKeyPair.getPrivate(),SERVER_ENTRY_PASSWORD,certChain);
        }
        catch (NoSuchAlgorithmException nsae){
            nsae.printStackTrace();
        }
        catch (KeyStoreException kse) {
            System.out.println("Caught KeyStore Exception: "+ kse.getLocalizedMessage());
        }
        catch (CertificateException ce) {
            ce.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    KeyStore.PrivateKeyEntry getPrivateKeyEntry(KeyStore keyStore, String alias, char[] entryPassword)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore.PasswordProtection pswd = new KeyStore.PasswordProtection(entryPassword);

        return (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias,pswd);
    }


    KeyStore getKeyStore(){

        return keyStore;
    }

    KeyPair getServerKeyPair(){

        return serverKeyPair;
    }
}
