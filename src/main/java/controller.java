import com.drew.imaging.ImageProcessingException;

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

    private static final String INPUT_FOLDER = "input";
    private static final String MODEL_DICT_NAME = "camera-dictionary.txt";
    private static final boolean KEEP_SIMILAR_IMAGES = true;
    private static final boolean CAMERA_MODEL_REQUIRED = true;
    private static final boolean MP4_FILE_DATE_FALLBACK = false;
    private static final boolean MOV_FILE_DATE_FALLBACK = true;
    private static final boolean VERBOSE = true;

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


    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss");
    private static final DateTimeFormatter DIR_NAME_FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM");




    public static void main(String args[]){

        if (!inputDir.exists() || inputDir.isFile()){
            System.err.println("ERROR: Directory '"+INPUT_FOLDER+"' not found");
            System.err.println(inputDir.getAbsolutePath());
            System.exit(1);
        }

        out.print("Camera Model dictionary: ");
        modelMap = parseModelMap();

        if (modelMap != null){
            out.println("LOADED");
            printModelMap();
            if (modelMap.size() == 0){
                modelMap = null;
            }
        }

        out.println("Processing directories:");
        processDirectory(inputDir);
        out.println("\n\nDONE");
    }



    private static Map<String,String> parseModelMap(){
        Map<String,String> result = new HashMap<String, String>();
        Scanner inputReader;
        try{
            inputReader = new Scanner(modelDict);
        }catch (FileNotFoundException e){
            out.println("FILE NOT FOUND");
            return null;
        }
        inputReader.useDelimiter(":");
        try {
            String[] line;
            while (inputReader.hasNextLine()) {
                line = inputReader.nextLine().split(":",2);
                result.put(line[0],line[1]);
            }
        }catch (Exception e){
            System.err.println("WARNING: Couldn't parse dictionary completely");
            //e.printStackTrace();
        }

        return result;
    }

    private static void printModelMap(){
        for (String key: modelMap.keySet()){
            out.printf("'%s' --> '%s'\n",key,modelMap.get(key));
        }
        out.println("--------------------------------------------");
    }

    private static void processDirectory(File dir){
        File[] children = dir.listFiles();
        out.println(tabs+dir.getName());
        tabs+="\t";
        for(File child: children){
            if (child.isDirectory()){
                processDirectory(child);
            }else{
                processFile(child);
            }
        }
        //all children have been processed, so one tab less and if no children exist, remove the dir.
        tabs = tabs.substring(0,tabs.length()-1);
        if (dir.listFiles().length == 0) dir.deleteOnExit();
    }

    private static void processFile(File file){
        String extension = getExtension(file.getName());

        Object[] data = getApplicableExifData(file,extension);
        //on error parsing exifdata return.
        if (data == null) return;

        LocalDateTime date = (LocalDateTime) data[0];
        String model = (String) data[1];

        //If not enough exifdata skip the file
        if (date==null|| extension==null    ||   CAMERA_MODEL_REQUIRED && model == null){
            if (VERBOSE) out.println(tabs+"Skipping:"+ file.getName() + "   date: "+date+"   model:"+model);
            return;
        }
        //rewrite camera model from dictionary if possible.
        model = rewriteModelName(model);

        //build new filename with the available exif data
        StringBuilder newFileName = new StringBuilder();
        //path
        newFileName.append(workingDir.getAbsolutePath());
        newFileName.append("/"+date.format(DIR_NAME_FORMATTER)+"/");

        //create the yyyy-MM dir if needed
        File dir = new File(newFileName.toString());
        if (!dir.exists()){
            dir.mkdir();
        }

        //append the filename
        newFileName.append(date.format(FILE_NAME_FORMATTER));
        newFileName.append(" ");
        newFileName.append(model);
        newFileName.append(extension);

        //handle multiple shots within one minute
        File dest = handleFileName(newFileName.toString());

        //only move if destination file is available/allowed
        if (dest != null) {
            file.renameTo(dest);
        }
    }

    private static Object[] getApplicableExifData(File file, String extension) {
        Object[] result = new Object[2];
        try {
            switch (extension){
                case ".mp4":
                    result[0] = EXIF.getMp4VideoDate(file,MP4_FILE_DATE_FALLBACK);
                    result[1] = "";
                    break;
                case ".mov":
                    result[0] = EXIF.getMovVideoDate(file,MOV_FILE_DATE_FALLBACK);
                    result[1] = EXIF.getMovVideoModel(file);
                    break;
                default:
                    result[0] = EXIF.getImageDate(file);
                    result[1] = EXIF.getImageModel(file);
                    break;
            }
        }catch (ImageProcessingException | IOException e2) {
            System.err.printf("Not parsable: %s\n",file.getAbsolutePath());
            return null;
        }catch (Exception e){
            System.err.printf("ERROR parsing: %s\n",file.getAbsolutePath());
            e.printStackTrace();
            return null;
        }

        return result;
    }

    private static File handleFileName(String fileName) {
        File dest = new File(fileName);
        if (dest.exists()){
            if (!KEEP_SIMILAR_IMAGES){
                System.err.println("ERROR: FILE ALREADY EXISTS:"+fileName);
                return null;
            }else{
                int number = 2;
                while (dest.exists()){
                    dest = new File(fileName.replace(" ","_"+number+" "));
                }
            }
        }
        return dest;
    }

    private static String rewriteModelName(String model) {
        if (model == null) {
            return "Unknown";
        }
        if (modelMap !=null && modelMap.containsKey(model)) {
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
            return m.group(1).toLowerCase();
        } else {
            System.err.println("ERROR: Couldn't get file extension of: "+filename);
            return null;
        }

    }

}


