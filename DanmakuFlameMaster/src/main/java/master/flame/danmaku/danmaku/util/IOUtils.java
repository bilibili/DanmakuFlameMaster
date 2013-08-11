package master.flame.danmaku.danmaku.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Yrom on 13-8-11.
 */
public class IOUtils {
    public static String getString(InputStream in){
        byte[] data = getBytes(in);
        if(data != null) return new String(data);
        return "";
    }
    public static byte[] getBytes(InputStream in){

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len = 0;
            while ((len = in.read(buffer)) != -1)
                baos.write(buffer, 0, len);
            in.close();
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }


    }
}
