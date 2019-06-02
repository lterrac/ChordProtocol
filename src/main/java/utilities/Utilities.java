package utilities;

import model.NodeProperties;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static model.NodeProperties.KEY_SIZE;

public class Utilities {

    /**
     * Example of SHA 1
     */
    public static void main(String[] args) {
        System.out.println(sha1("File0"));
        System.out.println(sha1("File1"));
        System.out.println(sha1("File2"));
        System.out.println(sha1("File3"));
        System.out.println(sha1("File4"));
        System.out.println((int) Math.pow(2, KEY_SIZE));
    }

    /**
     * SHA1
     *
     * @param text is a string to be transformed into an integer
     * @return the SHA1 translation of the text passed as parameter
     */
    public static int sha1(String text) {
        try {
            String sha1;
            MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
            msdDigest.update(text.getBytes(StandardCharsets.UTF_8), 0, text.length());
            sha1 = DatatypeConverter.printHexBinary(msdDigest.digest());
            return Integer.parseInt(new BigInteger(sha1, 16)
                    .mod(BigInteger.valueOf((int) Math.pow(2, KEY_SIZE)))
                    .toString(2), 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Calculate the fixId to check. Keep attention that if id+2^(fixIndex)
     * is greater than the maximum key value ( 2^(KEY_SIZE) - 1 ) it must return the difference
     * between the two values.
     *
     * @param nodeId   is the id of the node
     * @param fixIndex is the index of the finger table
     * @return the upper bound of the finger table row
     */
    public static int calculateFixId(int nodeId, double fixIndex) {
        int ideal = nodeId + (int) Math.pow(2, fixIndex);
        int limit = (int) Math.pow(2, NodeProperties.KEY_SIZE);
        return ideal % limit;
    }

}
