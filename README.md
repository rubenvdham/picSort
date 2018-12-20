# picSort (Image and MP4/MOV Sort CLI Tool)

I created picSort to sort my ~100GB collection of images and videos from multiple smartphones. I wasn't able to cope with all the different naming schemes across vendors. Therefore, I programmed this little tool extracting EXIF data in order to rename the files according to this structure: `workingdir/yyyy-MM/YYYYMMdd-HHmmss model.extension`  
 For example:  
 `workingdir/2017-08/20170808-182309 Oneplus 5.jpg`

Files that cannot be processed, because they lack the required EXIF data, will stay in the input folder. Typically: screenshots and WhatsApp images.

## What does it look like IRL?
input:  
```  
├── 20140719_122725.jpg  
├── 20140719_122732.jpg  
├── 20150101_001835.jpg  
├── 20150101_002844.jpg  
├── IMG_20170808_162308_Bokeh.jpg  
├── IMG_20170808_204047.jpg  
└── IMG_20170808_204124.jpg  

0 directories, 7 files  
```

output:  
```
├── 2014-07
│   ├── 20140719-142725 SM-G900F.jpg
│   └── 20140719-142732 SM-G900F.jpg
├── 2015-01
│   ├── 20150101-011834 SM-G900F.jpg
│   └── 20150101-012844 SM-G900F.jpg
└── 2017-08
    ├── 20170808-182309 Oneplus 5.jpg
    ├── 20170808-224047 Oneplus 5.jpg
    └── 20170808-224124 Oneplus 5.jpg

3 directories, 7 files
```

## Command line options

**--input** : *Input folder of the media, Default: 'input'*

**--dictionary** : *File path of the text file containing the Camera Model dictionary, Default: 'camera-dictionary.txt'*

**--remove-similar** : *Toggle skip behavior of similar images (multiple images per second)*

**--relax-model** : *Skip the Camera Model Check, allowing media without a specified Camera Model in the EXIF data*

**--mp4-file-date-fallback** : *ALLOW media with .mp4 extension to fallback on file date when EXIF creation date is not available*

**--disable-mov-file-date-fallback** : *DENY media with .mov extension to fallback on file date when EXIF creation date is not available*

The output folder is the current working directory of the script.

### Features

#### Camera Dictionary
Used to map camera types to specified names. It should be a text file. Besides that, it should adhere to the following structure:  
`EXIF model:requested file name model`  
 e.g `ONEPLUS A5000:Oneplus 5` or `NIKON 43939394ABC:camera Alice`

 #### Sloppy source code
 It's not a bug! It's a feature!
 - Some argument variables use naming for constants
 - Some lines contain too much characters
 - Many more
