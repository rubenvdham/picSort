import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class controller {

    ;
    private static final String INPUT_FOLDER = "input";
    private static final File workingDir;
    private static final File inputDir;
    private static PrintStream out = System.out;
    private static String tabs = "";

    static {
        workingDir = new File("");
        inputDir = new File(workingDir.getAbsolutePath()+"/"+INPUT_FOLDER);
    }


    public static void main(String args[]){

        if (!inputDir.exists() || inputDir.isFile()){
            System.err.println("ERROR: Directory '"+INPUT_FOLDER+"' not found");
            System.err.println(inputDir.getAbsolutePath());
            System.exit(1);
        }
        out.print("Processing directories:");
        processDirectory(inputDir);
        out.println("\n\nDONE");
    }




    private static void processDirectory(File dir){
        File[] children = dir.listFiles();
        tabs+="\t";
        out.println(tabs+dir.getName());
        for(File child: children){
            if (child.isDirectory()){
                processDirectory(child);
            }else{
                processFile(child);
            }
        }
        tabs = tabs.substring(0,tabs.length()-1);
    }

    private static void processFile(File file){
        EXIF.printEssentials(file);
    }

}


