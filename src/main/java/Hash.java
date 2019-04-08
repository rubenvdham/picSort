import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class Hash {
    static List hashes = new ArrayList<String>();

    protected static String getSHA256Hash(File file) throws NoSuchAlgorithmException, IOException {
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    protected static boolean fileExists(File file){
        String digest = null;
        try {
            digest = getSHA256Hash(file);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println(digest);
        if (hashes.contains(digest)){
            return true;
        }else{
            hashes.add(digest);
            return false;
        }
    }

    protected static boolean sameFiles(File file1, File file2) {
        String digest1 = null;
        String digest2 = null;
        try {
            digest1 = getSHA256Hash(file1);
            digest2 = getSHA256Hash(file2);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return digest1.equals(digest2);
    }
}