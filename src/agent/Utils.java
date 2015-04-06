package agent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Created by deepal on 3/5/15.
 */
public class Utils {
    public static String getUniqueHash(String ipAddress){
        String timestamp = ""+(new Date()).getTime();
        byte[] strbytes = (ipAddress+timestamp).getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestArr = md.digest(strbytes);

            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < digestArr.length; i++) {
                if ((0xff & digestArr[i]) < 0x10) {
                    hexString.append("0"
                            + Integer.toHexString((0xFF & digestArr[i])));
                } else {
                    hexString.append(Integer.toHexString(0xFF & digestArr[i]));
                }
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
