@Grab("commons-io:commons-io:2.4")

import org.apache.commons.io.FileUtils

import java.text.SimpleDateFormat
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.Files
import java.nio.file.Paths

private void sortPicturesInFolder(File folder) {
    File[] files = folder.listFiles()
    // Process each file in the folder
    files.each { File file ->
        // If file is a folder, recursively call this method
        if (file.isDirectory()) {
            sortPicturesInFolder(file)
        }
        // If this is a picture or video file, process it
        if (fileToMove(file.getName())) {
            moveFile(file)
        }
    }
    // When done processing the folder, check to see if it can be deleted
    files = folder.listFiles()
    boolean deleteFolder = true
    files.each { File file ->
        if (file.isDirectory()) {
            deleteFolder = false
        }
        String fileName = file.getName()
        if (!isFileOkayToDelete(fileName)) {
            deleteFolder = false
        }
    }
    if (deleteFolder) {
        log("--------------")
        log("Deleting folder: ${folder.getName()}")
        folder.deleteDir()
    }
}

// Determine if the file name should be processed, based on file extension
private boolean fileToMove(String fileName) {
    fileName = fileName.toLowerCase()
    if (fileName.endsWith(".jpeg") ||
            fileName.endsWith(".jpg") ||
            fileName.endsWith(".png") ||
            fileName.endsWith(".mov")) {
        return true
    }
    return false
}

// Based on the file name, determine if the file can be deleted
private boolean isFileOkayToDelete(String fileName) {
    if (fileName == ".DS_Store") {
        return true
    }
    return false
}

// Handle processing of an individual file
private void moveFile(File file) {
    log("--------------")
    log("Processing file: ${file.getAbsolutePath()}")
    BasicFileAttributes fileAttributes = Files.readAttributes(Paths.get(file.getAbsolutePath()), BasicFileAttributes.class)
    Calendar calendar = Calendar.getInstance()
    calendar.setTimeInMillis(fileAttributes.creationTime().toMillis())
    String month = (calendar.get(Calendar.MONTH)+1).toString()
    month = month.length() == 1 ? "0${month}" : month
    String year = calendar.get(Calendar.YEAR).toString()
    // Get year folder, or create it
    File yearFolder = new File(year)
    if (!yearFolder.exists()) {
        yearFolder.mkdir()
    }
    // Get year-month folder, or create it
    File yearMonthFolder = new File(yearFolder, "${year}-${month}")
    if (!yearMonthFolder.exists()) {
        yearMonthFolder.mkdir()
    }

    // Options to take with a file:
    // 1. move the file
    // 2. rename and move the file
    // 3. remove the file

    String fileName = file.getName()
    // Check to see if file with the same name already exists
    File duplicateFile = new File(yearMonthFolder, file.getName())
    if (duplicateFile.exists()) {
        log("duplicateFile exists: ${duplicateFile.getAbsolutePath()}")
        boolean fileContentsEqual = FileUtils.contentEquals(file, duplicateFile)
        log("fileContentsEqual: $fileContentsEqual")
        // Files are equal, delete the current file
        if (fileContentsEqual) {
            log("deleting file: ${file.getAbsolutePath()}")
            file.delete()
        }
        // Files are not equal, rename the current file
        else {
            boolean fileNameExists = true
            int counter = 0
            while (fileNameExists) {
                int index = fileName.lastIndexOf(".")
                String newFileName = "${fileName.substring(0, index)}_${counter++}${fileName.substring(index)}"
                File fileWithCounter = new File(yearMonthFolder, newFileName)
                if (!fileWithCounter.exists()) {
                    fileName = newFileName
                    fileNameExists = false
                }
            }
            log("using new name: $fileName")
        }
    }
    // Move file to new location
    file.renameTo("${yearMonthFolder}/${fileName}")
    log("moving file to: ${yearMonthFolder.getAbsolutePath()}/${fileName}")
//    log("name: ${file.getName()}"
//    log("original file location: ${file.getAbsolutePath()}"
//    log("duplicateFile location: ${duplicateFile.getAbsolutePath()}"
//    boolean fileContentsEqual = FileUtils.contentEquals(file, duplicateFile)
//    log("fileContentsEqual: ${fileContentsEqual}"
//    // If the name has a conflict, iterate through the options of new filenames
//    int counter = 0
//    String fileName = file.getName()
//    while (duplicateFile.exists() && !fileContentsEqual) {
//        int index = fileName.lastIndexOf(".")
//        String newFileName = "${fileName.substring(0, index)}_${counter++}${fileName.substring(index)}"
//        File fileWithCounter = new File(yearMonthFolder, newFileName)
//        if (!fileWithCounter.exists()) {
//            fileName = newFileName
//            fileContentsEqual = true
//        }
//    }
//    log("fileName: ${fileName}"
//
////    google humingbird.jpg not getting a new name
//
//    // Actually move the file to the new location, by renaming it
//    file.renameTo("${yearMonthFolder}/${fileName}")
}

private void log(String message) {
    println message
    logFile.append(message+"\n")
}

File logs = new File("logs")
if (!logs.exists() || !logs.isDirectory()) {
    logs.mkdir()
}
String dateTime = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date())
logFile = new File(logs, "PictureSorter-${dateTime}.log")
File rootFolder = new File("Pictures to go through")
sortPicturesInFolder(rootFolder)
log("DONE")
