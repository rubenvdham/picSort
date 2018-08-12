import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class EXIF {



protected static void printMetadata(File file){
    try {
        Metadata metadata = ImageMetadataReader.readMetadata(file);

        print(metadata, "Using ImageMetadataReader");
    } catch (ImageProcessingException e) {
    } catch (IOException e) {
    }

}


   private static String getModel(Metadata metadata){

        ExifIFD0Directory idf = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        return idf.getDescription(272);
    }
   private static Date getCreationDate(Metadata metadata){


       ExifSubIFDDirectory sub =  metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
       return sub.getDateOriginal();
    }
    private static Date getCreationDate2(Metadata metadata){


        ExifSubIFDDirectory sub =  metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        return  sub.getDateDigitized();
    }



    public static void printEssentials(File file){
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            System.out.println("-------------------");
            System.out.println(getCreationDate(metadata));
            System.out.println(getCreationDate2(metadata));

            LocalDateTime date =  LocalDateTime.ofInstant(getCreationDate(metadata).toInstant(), ZoneId.systemDefault());
            System.out.println(date);
            System.out.println(getModel(metadata));


        } catch (ImageProcessingException e) {
        } catch (Exception e) {
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
