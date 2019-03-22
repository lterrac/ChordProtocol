package utilities;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static model.NodeProperties.KEY_SIZE;

public class Utilities {

    public static final int BUFFER_SIZE = 1024;

    /**
     * Example of SHA 1 -> uses m = 160 bit
     * @param args
     * @throws NoSuchAlgorithmException
     */
    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println(sha1("192.168.1.140:9001"));
    }


    public static int sha1(String text) {
        try
        {
            String sha1 = null;
            MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
            msdDigest.update(text.getBytes("UTF-8"), 0, text.length());
            sha1 = DatatypeConverter.printHexBinary(msdDigest.digest());
            return Integer.parseInt(new BigInteger(sha1, 16).mod(BigInteger.valueOf(KEY_SIZE)).toString(2), 2);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }
}
