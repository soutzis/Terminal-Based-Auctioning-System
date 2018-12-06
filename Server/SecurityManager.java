package Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

class SecurityManager {

    private static final int RSA_KEY_LENGTH = 2048;
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    static KeyPair generateNewKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        kpg.initialize(RSA_KEY_LENGTH, SecureRandom.getInstanceStrong());

        return kpg.generateKeyPair();
    }

    static byte[] generateDigitalSignature(PrivateKey authorKey, Object objToSign)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException {
        Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
        dsa.initSign(authorKey);

        BufferedInputStream bis = convertObjectToBufferedStream(objToSign);

        byte[] buffer = new byte[RSA_KEY_LENGTH];
        int len;
        while ((len = bis.read(buffer)) >= 0) {
            dsa.update(buffer, 0, len);
        }
        bis.close();

        //writeSignatureToDisk(signature);
        return dsa.sign();
    }

    static boolean verifySignature(PublicKey pubKey, byte[] signatureData, Object objToVerify)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(pubKey);

        BufferedInputStream bis = convertObjectToBufferedStream(objToVerify);
        byte[] buffer = new byte[RSA_KEY_LENGTH];

        int len;
        while (bis.available() != 0) {
            len = bis.read(buffer);
            signature.update(buffer, 0, len);
        }
        bis.close();

        return signature.verify(signatureData);
    }

    static void writeKeyToDisk(Key key, String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(key.getEncoded());
        fos.close();
    }

    static PrivateKey readPrivateKey(String filename)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] data = readFileBytes(filename);
        //Generate key from its format
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

        return keyFactory.generatePrivate(spec);
    }

    static PublicKey readPublicKey(String filename)
            throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        byte[] data = readFileBytes(filename);
        //Generate key from its format
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

        return keyFactory.generatePublic(spec);
    }

    private static void writeSignatureToDisk(byte[] data) throws IOException {

        FileOutputStream fos = new FileOutputStream("DigitalSignature");
        fos.write(data);
        fos.close();
    }

    private static BufferedInputStream convertObjectToBufferedStream(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

        return new BufferedInputStream(inputStream);
    }

    private static byte[] readFileBytes(String filename) throws IOException {
        Path path = Paths.get(filename);

        return Files.readAllBytes(path);
    }
}
