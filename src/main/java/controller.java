import com.drew.imaging.ImageProcessingException;
import org.apache.commons.cli.*;

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

    /*Command line args*/
    private static String INPUT_FOLDER = "input";             //Input folder of the new media
    private static String MODEL_DICT_NAME = "camera-dictionary.txt";  //File path of the txt file containg the camera model dictionary
    private static boolean KEEP_SIMILAR_IMAGES = true;        //Similar images are images which have been taken within the same second
    private static boolean CAMERA_MODEL_REQUIRED = true;      //If yes, EXIF data MUST contain Camera model
    private static boolean MP4_FILE_DATE_FALLBACK = false;    //Use File creation date for MP4 files
    private static boolean MOV_FILE_DATE_FALLBACK = true;     //Use File creation date for MOV files
    private static boolean verbose = false;                         //set verbosity

    /* Runtime variables*/
    private static final File workingDir;
    private static final File inputDir;
    private static final File modelDict;

    private static PrintStream out = System.out;
    private static String tabs = "";
    private static Map<String,String> modelMap;
    private static boolean running = true;

    static {
        workingDir = new File("");
        inputDir = new File(workingDir.getAbsolutePath()+"/"+INPUT_FOLDER);
        modelDict = new File(workingDir.getAbsolutePath()+"/"+MODEL_DICT_NAME);
    }


    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss");
    private static final DateTimeFormatter DIR_NAME_FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM");




    public static void main(String args[]){
        parseArgs(args);

        if (!inputDir.exists() || inputDir.isFile()){
            System.err.printf("ERROR: Directory '"+INPUT_FOLDER+"' not found: %s\n",inputDir.getAbsolutePath());
            System.exit(1);
        }

        out.print("Camera Model dictionary: ");
        modelMap = parseModelMap();

        if (modelMap != null){
            if (modelMap.size() == 0){
                modelMap = null;
                out.println("empty");
            } else {
                out.println("LOADED");
                printModelMap();
            }
        }
        out.println("Processing directories:");
        processDirectory(inputDir);
        out.println("\n\nDONE");
    }

    private static void parseArgs(String[] args) {
        Options options = new Options();
        Option verbosity = new Option("v","verbose",false,"Toggle verbosity");
        Option removeSimilar = new Option("rs","remove-similar",false,"Toggle skip of similar images");
        Option doNotRequireModel = new Option("rm","relax-model",false,"Skip the camera model check");
        Option MP4FileDateFallback = new Option("mp4fb","mp4-file-date-fallback",false,"ALLOW mp4 extension fallback on filedate");
        Option MOVFileDateFallback = new Option("notmovfb","disable-mov-file-date-fallback",false,"DENY mov extension fallback on filedate");
        Option inputFolder = new Option("i","input",true,"Input folder of the new media, Default: input");
        Option dictFolder = new Option("d","dictionary",true,"File path of camera model dictionary, Default: camera-dictionary.txt");

        options.addOption(verbosity)
                .addOption(removeSimilar)
                .addOption(dictFolder)
                .addOption(doNotRequireModel)
                .addOption(MP4FileDateFallback)
                .addOption(MOVFileDateFallback)
                .addOption(inputFolder);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            out.println(e.getMessage());
            formatter.printHelp("picSort", options);
            System.exit(1);
        }

        if (cmd.hasOption("verbose")) verbose = true;
        if (cmd.hasOption("input")) INPUT_FOLDER = cmd.getOptionValue("input");
        if (cmd.hasOption("dictionary")) MODEL_DICT_NAME = cmd.getOptionValue("dictionary");
        if (cmd.hasOption("mp4-file-date-fallback")) MP4_FILE_DATE_FALLBACK = true;
        if (cmd.hasOption("disable-mov-file-date-fallback")) MOV_FILE_DATE_FALLBACK = false;
        if (cmd.hasOption("relax-model")) CAMERA_MODEL_REQUIRED = false;
        if (cmd.hasOption("remove-similar")) KEEP_SIMILAR_IMAGES = false;

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
            if (running) {
                if (child.isDirectory()) {
                    processDirectory(child);
                } else {
                    processFile(child);
                }
            }
        }
        //all children have been processed, so one tab less and if no children exist, remove the dir
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
        if (date == null || extension == null ||  (CAMERA_MODEL_REQUIRED && model == null) ){
            if (verbose) out.println(tabs+"Skipping:"+ file.getName() + "   date: "+date+"   model:"+model);
            return;
        }
        //rewrite camera model from dictionary if possible.
        model = rewriteModelName(model,extension);

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
        if (!model.equals("")){
            newFileName.append(" ");
            newFileName.append(model);
        }
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
                    result[1] = null;
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
            if (verbose) System.err.printf("Not parsable: %s\n",file.getAbsolutePath());
            return null;
        }catch (Exception e){
            if (verbose) System.err.printf("ERROR parsing: %s\n",file.getAbsolutePath());
            e.printStackTrace();
            return null;
        }
        return result;
    }

    private static File handleFileName(String fileName) {
        File dest = new File(fileName);
        if (dest.exists()){
            if (!KEEP_SIMILAR_IMAGES){
                out.println("Similar image, skipping (can be turned off): "+fileName);
                return null;
            }else{
                int number = 2;
                while (dest.exists()){
                    dest = new File(fileName.replaceFirst(" ","_"+number+" "));
                    number+=1;
                }
            }
        }
        return dest;
    }

    private static String rewriteModelName(String model, String extension) {
        if (model == null) {
            if (extension.startsWith(".jp")) {
                return "Unknown";
            }else{
                return "";
            }
        }
        if (modelMap != null && modelMap.containsKey(model)) {
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