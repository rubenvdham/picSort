import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.imaging.quicktime.QuickTimeMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import com.drew.metadata.mp4.Mp4Directory;


import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.Date;

public class EXIF {

    private static ZoneId TIME_ZONE = ZoneId.of("UTC");

     /*IMAGE PROTECTED CLASSES*/

    protected static String getImageModel(File file)throws IOException,ImageProcessingException{
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        return getImageModel(metadata);
    }

    protected static String getImageModel(Metadata metadata){

        ExifIFD0Directory idf = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        try{
            if (idf.getDescription(272) == null) return null;
        }catch (NullPointerException e){return null;}
        return idf.getDescription(272);
    }


    protected static LocalDateTime getImageDate(File file) throws IOException,ImageProcessingException{
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        return getImageDate(metadata);
    }

    protected static LocalDateTime getImageDate(Metadata metadata){
        Date creationDate = null;
        try{
            creationDate = getImageCreationDate(metadata);
        }catch (NullPointerException e){

        }
        if (creationDate == null) { try { creationDate = getImageDigitizedDate(metadata);}catch (NullPointerException e){}}
        if (creationDate == null) { try { creationDate = getImageIFD0ModifyDate(metadata);}catch (NullPointerException e){}}
        if (creationDate == null) return null;


        LocalDateTime creationDateTime = LocalDateTime.ofInstant(creationDate.toInstant(), TIME_ZONE);
        return creationDateTime;
    }



    /*IMAGE PRIVATE CLASSES*/

    private static Date getImageCreationDate(Metadata metadata) throws NullPointerException{
        ExifSubIFDDirectory sub =  metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        return sub.getDateOriginal();
    }



    private static Date getImageDigitizedDate(Metadata metadata) throws NullPointerException{
        ExifSubIFDDirectory sub =  metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        return  sub.getDateDigitized();
    }


    private static Date getImageIFD0ModifyDate(Metadata metadata) throws NullPointerException{
        ExifIFD0Directory ifd=  metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        return ifd.getDate(306);
    }





    /*VIDEO PROTECTED CLASSES*/

    protected static LocalDateTime getMovVideoDate(File file, boolean modifiedFallback)throws IOException,ImageProcessingException{
        Metadata metadata = QuickTimeMetadataReader.readMetadata(file);

        QuickTimeVideoDirectory vidDir = metadata.getFirstDirectoryOfType(QuickTimeVideoDirectory.class);
        FileSystemDirectory fsDir = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
        Date creationDate = null;
        try {creationDate = vidDir.getDate(256); }catch(NullPointerException e){} //creation time
        if (creationDate == null) try{ creationDate = vidDir.getDate(257);}catch(NullPointerException e){} //try modification time
        if (creationDate == null && modifiedFallback) try{ creationDate = fsDir.getDate(3);}catch(NullPointerException e){} //try filesystem modified date
        if (creationDate == null) return null;

        return LocalDateTime.ofInstant(creationDate.toInstant(), TIME_ZONE);
    }

    protected static String getMovVideoModel(File file) throws IOException,ImageProcessingException{
        Metadata metadata = QuickTimeMetadataReader.readMetadata(file);
        QuickTimeMetadataDirectory mdDir = metadata.getFirstDirectoryOfType(QuickTimeMetadataDirectory.class);
        try {
            return mdDir.getString(1310); //Get model (iphones mostly)
        }catch (NullPointerException e){
            return null;
        }
    }



    protected static LocalDateTime getMp4VideoDate(File file, boolean modifiedFallback)throws IOException,ImageProcessingException{
        Metadata metadata = Mp4MetadataReader.readMetadata(file);
        Mp4Directory mp4Dir = metadata.getFirstDirectoryOfType(Mp4Directory.class);
        FileSystemDirectory fsDir = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);

        Date creationDate = mp4Dir.getDate(256);//creation time
        if (creationDate == null) creationDate = mp4Dir.getDate(257); //try modification time
        if (creationDate == null && modifiedFallback) creationDate = fsDir.getDate(3); //try filesystem modified date
        if (creationDate == null) return null;

        return LocalDateTime.ofInstant(creationDate.toInstant(), TIME_ZONE);
    }


    /*debug functions*/

    protected static void printMetadata(File file){
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            print(metadata, "Using ImageMetadataReader");
        } catch (ImageProcessingException e) {
        } catch (IOException e) {
        }

    }

    private static void print(Metadata metadata, String method)
    {
        System.out.println();
        System.out.println("-------------------------------------------------");
        System.out.print(' ');
        System.out.print(method);
        System.out.println("-------------------------------------------------");
        System.out.println();

        //
        // A Metadata object contains multiple Directory objects
        //



        for (Directory directory : metadata.getDirectories()) {

            //
            // Each Directory stores values in Tag objects
            //
            for (Tag tag : directory.getTags()) {
                System.out.println(tag);

            }

            //
            // Each Directory may also contain error messages
            //
            for (String error : directory.getErrors()) {
                System.err.println("ERROR: " + error);
            }
        }
    }
}
