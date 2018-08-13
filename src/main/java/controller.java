import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class controller {

    ;
    private static final String INPUT_FOLDER = "input";
    private static final String MODEL_DICT_NAME = "camera-dictionary.txt";


    private static final File workingDir;
    private static final File inputDir;
    private static final File modelDict;
    private static PrintStream out = System.out;
    private static String tabs = "";
    private static Map<String,String> modelMap;

    static {
        workingDir = new File("");
        inputDir = new File(workingDir.getAbsolutePath()+"/"+INPUT_FOLDER);
        modelDict = new File(workingDir.getAbsolutePath()+"/"+MODEL_DICT_NAME);
    }


    private static final boolean CAMERA_MODEL_REQUIRED = true;
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmSS");
    private static final DateTimeFormatter DIR_NAME_FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM");

    public static void main(String args[]){

        if (!inputDir.exists() || inputDir.isFile()){
            System.err.println("ERROR: Directory '"+INPUT_FOLDER+"' not found");
            System.err.println(inputDir.getAbsolutePath());
            System.exit(1);
        }


        out.print("Camera Model dictionary loaded: ");
        modelMap = parseModelMap();
        if (modelMap == null){
            out.println("NO");
        }else {
            out.println("YES");
        }

        out.print("Processing directories:");
        processDirectory(inputDir);
        out.println("\n\nDONE");
    }



    private static Map<String,String> parseModelMap(){
        Map<String,String> result = new HashMap<String, String>();
        Scanner inputReader;
        try{
             inputReader = new Scanner(modelDict);
        }catch (FileNotFoundException e){
            return null;
        }
        inputReader.useDelimiter(":");
        try {
            while (inputReader.hasNextLine()) {
                result.put(inputReader.next(), inputReader.next());
            }
        }catch (Exception e){
            System.err.println("WARNING: Couldn't parse dictionary completely");
        }
        return result;
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
        //EXIF.printEssentials(file);
        //grab info
        String model = null;
        LocalDateTime date = null;
        try {
            model = EXIF.getModel(file);
            date = EXIF.getDate(file);
        }catch(Exception e){
            System.err.printf("ERROR parsing: %s",file.getAbsolutePath());
            e.printStackTrace();
            return;
        }
        String extension = getExtension(file.getName());
        if (date==null|| extension==null    ||   CAMERA_MODEL_REQUIRED && model == null){
            return;
        }

        model = rewriteModelName(model);



        StringBuilder newFileName = new StringBuilder();
        //path
        newFileName.append(workingDir.getAbsolutePath());
        newFileName.append("/"+date.format(DIR_NAME_FORMATTER)+"/");

        //filename
        newFileName.append(date.format(FILE_NAME_FORMATTER));
        newFileName.append(" ");
        newFileName.append(model);
        newFileName.append(extension);


        File dest = new File(newFileName.toString());
        if (dest.exists()){
            System.err.println("ERROR: FILE ALREADY EXISTS:"+newFileName.toString());
            return;
        }
        file.renameTo(dest);
    }

    private static String rewriteModelName(String model) {
        if (model == null) {
            return "Unknown";
        }
        if (modelMap.containsKey(model)) {
            return modelMap.get(model);
        } else {
            return model;
        }
    }

    private static String getExtension(String filename){

        // Create a Pattern object
        Pattern r = Pattern.compile(".+(\\.\\w+)$");

        // Now create matcher object.
        Matcher m = r.matcher(filename);

        if (m.find()) {
            return m.group(0);
        } else {
            System.err.println("ERROR: Couldn't get file extension of: "+filename);
            return null;
        }

    }

}


