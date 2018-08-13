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
import com.drew.metadata.mp4.Mp4Dictionary;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.mp4.media.Mp4TextDirectory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.Date;

public class EXIF {



protected static void printMetadata(File file){
    try {
        Metadata metadata = ImageMetadataReader.readMetadata(file);

        print(metadata, "Using ImageMetadataReader");
    } catch (ImageProcessingException e) {
    } catch (IOException e) {
    }

}   /*IMAGE PROTECTED CLASSES*/

    protected static String getImageModel(File file)throws IOException,ImageProcessingException{
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        return getImageModel(metadata);
    }

   protected static String getImageModel(Metadata metadata){

        ExifIFD0Directory idf = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        return idf.getDescription(272);
    }


    protected static LocalDateTime getImageDate(File file) throws IOException,ImageProcessingException{
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        return getImageDate(metadata);
    }

    protected static LocalDateTime getImageDate(Metadata metadata){
        Date creationDate = getImageCreationDate(metadata);
        if (creationDate == null){
            creationDate = getImageDigitizedDate(metadata);
            if (creationDate == null){
                return null;
            }
        }
        LocalDateTime creationDateTime = LocalDateTime.ofInstant(creationDate.toInstant(),ZoneId.systemDefault());
        return creationDateTime;
    }



    /*IMAGE PRIVATE CLASSES*/

    private static Date getImageCreationDate(Metadata metadata){


        ExifSubIFDDirectory sub =  metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        return sub.getDateOriginal();
    }



    private static Date getImageDigitizedDate(Metadata metadata){


        ExifSubIFDDirectory sub =  metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        return  sub.getDateDigitized();
    }






    /*VIDEO PROTECTED CLASSES*/







    protected static LocalDateTime getMovVideoDate(File file, boolean modifiedFallback)throws IOException,ImageProcessingException{
        Metadata metadata = QuickTimeMetadataReader.readMetadata(file);
        metadata.getDirectories().forEach(p->{
            p.getTags().stream().forEach(tag -> {
                System.out.println(tag.toString());
            });
        });
        return null;
    }

    protected static LocalDateTime getMp4VideoDate(File file, boolean modifiedFallback)throws IOException,ImageProcessingException{
        Metadata metadata = Mp4MetadataReader.readMetadata(file);
        Mp4Directory mp4Dir = metadata.getFirstDirectoryOfType(Mp4Directory.class);
        FileSystemDirectory fsDir = metadata.getFirstDirectoryOfType(FileSystemDirectory.class);
        Date creationDate = mp4Dir.getDate(256);//creation time
        if (creationDate == null) creationDate = mp4Dir.getDate(257); //try modification time
        if (creationDate == null && modifiedFallback) creationDate = fsDir.getDate(3); //try filesystem modified date

        return LocalDateTime.ofInstant(creationDate.toInstant(),ZoneId.systemDefault());
    }







   /* public static void printEssentials(File file){
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            System.out.println("-------------------");
            System.out.println(getCreationDate(metadata));
            System.out.println(getDigitizedDate(metadata));

            LocalDateTime date =  LocalDateTime.ofInstant(getCreationDate(metadata).toInstant(), ZoneId.systemDefault());
            System.out.println(date);
            System.out.println(getModel(metadata));


        } catch (ImageProcessingException e) {
        } catch (Exception e) {
        }
    }
*/




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
