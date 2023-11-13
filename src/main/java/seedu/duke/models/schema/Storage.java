package seedu.duke.models.schema;


import seedu.duke.utils.Parser;
import seedu.duke.utils.exceptions.CorruptedFileException;
import seedu.duke.utils.exceptions.InvalidTimetableUserCommandException;
import seedu.duke.utils.exceptions.MissingFileException;
import seedu.duke.utils.exceptions.TimetableUnavailableException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class Storage {

    private String userDirectory = System.getProperty("user.dir");

    /**
     * Constructs a new Storage instance with the specified file path.
     */
    public Storage() {

    }

    /**
     * Retrieves a list of modules requirements for a specified major.
     *
     * @author ryanlohyr
     * @param major The major for which to retrieve requirements.
     * @return An ArrayList of module codes.
     * @throws RuntimeException If the specified major requirements file is not found.
     */
    public static ArrayList<String> getRequirements(String major) {
        String[] courseArray = determineRequirements(major);
        return new ArrayList<>(Arrays.asList(courseArray));
    }

    /**
     * Determines the course requirements based on the specified major. Function is used if file is not found
     *
     * @param major A string representing the major (e.g., "CEG" for Computer Engineering, "CS" for Computer Science).
     * @return An array of strings containing the course requirements for the specified major.
     */
    private static String[] determineRequirements(String major) {
        String[] courseArray;

        String[] csCourseArray = {
            "CS1101S", "ES2660", "GEC1000", "GEA1000", "GESS1000",
            "GEN2000", "IS1108", "CS1231S", "CS2030", "CS2040S",
            "CS2100", "CS2101", "CS2103T", "CS2106", "CS2109S",
            "CS3230", "MA1521", "MA1522", "ST2334", "CP3880"
        };
        String[] cegCourseArray = {
            "CG1111A", "MA1511", "MA1512", "CS1010", "GESS1000",
            "GEC1000", "GEN2000", "ES2631", "GEA1000", "DTK1234",
            "EG1311", "IE2141", "EE2211", "EG2501", "CDE2000",
            "PF1101", "CG4002", "MA1508E", "EG2401A", "CP3880",
            "CG2111A", "CS1231", "CG2023", "CG2027", "CG2028",
            "CG2271", "ST2334", "CS2040C", "CS2113", "EE2026", "EE4204"
        };

        if(major.equals("CEG")){
            courseArray = cegCourseArray;
        }else{
            courseArray = csCourseArray;
        }
        return courseArray;
    }

    /**
     * Creates a "schedule.txt" file in the storage directory.
     */
    public void createUserStorageFile() {
        String dataDirectory = userDirectory + "/data";

        createDirectory(dataDirectory);

        createFileInDirectory(dataDirectory, "schedule.txt");
        createFileInDirectory(dataDirectory, "studentDetails.txt");
        createFileInDirectory(dataDirectory, "timetable.txt");

    }



    public Schedule loadSchedule() throws MissingFileException, CorruptedFileException {

        String scheduleFilePath = userDirectory + "/data/schedule.txt";

        if (!isFileExist(scheduleFilePath)) {
            throw new MissingFileException();
        }

        Schedule schedule = new Schedule();

        try {
            // Create a FileReader and BufferedReader to read the file.
            FileReader fileReader = new FileReader(scheduleFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;

            int targetIndex = 0;
            int[] modulesPerSemArray = new int[]{0, 0, 0, 0, 0, 0, 0, 0};

            // Read lines from the file and add them to the ArrayList.
            while ((line = bufferedReader.readLine()) != null) {

                String[] splitParts = line.split(" \\| ");

                switch (splitParts[0]) {

                // Happens once on the first line of txt file so that sorting subsequent modules is possible
                case "ModulesPerSem":
                    String[] modulesPerSemStringArray = splitParts[1].split(",");
                    for (int i = 0; i < modulesPerSemArray.length; i++) {
                        modulesPerSemArray[i] = Integer.parseInt(modulesPerSemStringArray[i]);
                    }
                    break;
                case "Module":
                    String module = splitParts[1];
                    int targetSemester = 1;
                    int indexOfLastModuleOfSem = modulesPerSemArray[targetSemester - 1] - 1;
                    while (targetIndex > indexOfLastModuleOfSem) {
                        indexOfLastModuleOfSem += modulesPerSemArray[targetSemester];
                        targetSemester += 1;
                    }

                    schedule.addModule(module, targetSemester);
                    if (splitParts[2].equals("O")) {
                        schedule.getModule(module).markModuleAsCompleted();
                    }
                    targetIndex += 1;
                    break;
                default:
                    if (!splitParts[0].trim().isEmpty()) {
                        throw new CorruptedFileException();
                    }
                }
            }

            // Close the BufferedReader to release resources.
            bufferedReader.close();
        } catch (Exception e) {
            throw new CorruptedFileException();
        }

        return schedule;

    }

    public ArrayList<String> loadStudentDetails() throws MissingFileException, CorruptedFileException {

        String studentDetailsFilePath = userDirectory + "/data/studentDetails.txt";

        if (!isFileExist(studentDetailsFilePath)) {
            throw new MissingFileException();
        }

        try {
            // Create a FileReader and BufferedReader to read the file.
            FileReader fileReader = new FileReader(studentDetailsFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            ArrayList<String> studentDetails = new ArrayList<>(3);

            String line;
            int lineNumber = 0;

            // to track which line it is supposed to be on
            HashMap<String, Integer> variableMap = new HashMap<>();
            // Adding key-value pairs
            variableMap.put("Name", 0);
            variableMap.put("Major", 1);
            variableMap.put("Year", 2);

            // Read lines from the file and add them to the ArrayList.
            while ((line = bufferedReader.readLine()) != null) {

                String[] splitParts = line.split(" \\| ");

                String userAttribute = splitParts[0];

                //validation to see that variables has not been tampered with
                if(variableMap.get(userAttribute) != lineNumber){
                    throw new CorruptedFileException();
                }

                switch (splitParts[0]) {

                case "Name":
                    String name = splitParts[1];
                    studentDetails.add(0, name);
                    break;
                case "Major":
                    String major = splitParts[1];

                    // Check if major stored in txt file is valid
                    Major.valueOf(major.toUpperCase());

                    studentDetails.add(1, major);
                    break;
                case "Year":
                    String year = splitParts[1];

                    //Check if year stored in txt file is valid
                    if (!Parser.isValidAcademicYear(year)){
                        throw new CorruptedFileException();
                    }

                    studentDetails.add(2, year);
                    break;
                default:
                    if (!splitParts[0].trim().isEmpty()) {
                        throw new CorruptedFileException();
                    }
                }
                lineNumber += 1;
            }
            // Close the BufferedReader to release resources.
            bufferedReader.close();

            return studentDetails;
        } catch (Exception e) {
            throw new CorruptedFileException();
        }
    }

    public ArrayList<TimetableUserCommand> loadTimetable(Student student)
            throws MissingFileException, CorruptedFileException {

        String timetableFilePath = userDirectory + "/data/timetable.txt";

        if (!isFileExist(timetableFilePath)) {
            throw new MissingFileException();
        }

        ArrayList<TimetableUserCommand> timetableUserCommands;
        try {
            // Create a FileReader and BufferedReader to read the file.
            FileReader fileReader = new FileReader(timetableFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            if ((line = bufferedReader.readLine()) != null) {
                if (!line.equals("TimetableForCurrentSem")) {
                    throw new CorruptedFileException();
                }
            }
            timetableUserCommands = new ArrayList<>();

            // Read lines from the file and add them to the ArrayList.
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    timetableUserCommands.add(new TimetableUserCommand(student,
                            student.getTimetable().getCurrentSemesterModulesWeekly(), line));
                } catch (InvalidTimetableUserCommandException e) {
                    //corrupted
                    throw new CorruptedFileException();
                }

            }

            // Close the BufferedReader to release resources.
            bufferedReader.close();
        } catch (Exception e) {
            throw new CorruptedFileException();
        }

        return timetableUserCommands;

    }

    public void addEventsToStudentTimetable(ArrayList<TimetableUserCommand> timetableUserCommands, Student student)
            throws CorruptedFileException {
        ArrayList<ModuleWeekly> currentSemModulesWeekly = student.getTimetable().getCurrentSemesterModulesWeekly();
        for (TimetableUserCommand currentTimetableCommand : timetableUserCommands) {
            //not exit, not clear
            try {
                currentTimetableCommand.processTimetableCommand(currentSemModulesWeekly);
            } catch (InvalidTimetableUserCommandException e) {
                //corrupted
                throw new CorruptedFileException();
            }
        }
    }

    public void saveStudentDetails (Student student) throws IOException {

        String studentDetailsFilePath = userDirectory + "/data/studentDetails.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(studentDetailsFilePath))) {

            String name = student.getName();

            String major = student.getMajor();

            String year = student.getYear();

            // Write the new content to the file
            writer.write("Name | " + name);
            writer.newLine();

            writer.write("Major | " + major);
            writer.newLine();

            writer.write(("Year | " + year));
            writer.newLine();
        }
    }

    public static void saveSchedule(Student student) throws IOException {

        String scheduleFilePath = System.getProperty("user.dir") + "/data/schedule.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(scheduleFilePath))) {

            int[] modulesPerSemArray = student.getSchedule().getModulesPerSem();

            StringBuilder modulesPerSemNumbers = new StringBuilder(Integer.toString(modulesPerSemArray[0]));
            for (int i = 1; i < modulesPerSemArray.length; i++) {
                modulesPerSemNumbers.append(",").append(modulesPerSemArray[i]);
            }

            // Write the new content to the file
            writer.write("ModulesPerSem | " + modulesPerSemNumbers);
            writer.newLine();

            ModuleList modulesPlanned = student.getSchedule().getModulesPlanned();
            int numberOfModules = student.getSchedule().getModulesPlanned().getMainModuleList().size();
            String completionStatus;
            for (int i = 0; i < numberOfModules; i++) {
                String moduleCode = modulesPlanned.getModuleByIndex(i).getModuleCode();
                if (modulesPlanned.getModuleByIndex(i).getCompletionStatus()) {
                    completionStatus = "O";
                } else {
                    completionStatus = "X";
                }
                writer.write("Module | " + moduleCode + " | " + completionStatus);
                writer.newLine();  // Move to the next line
            }
        }
    }

    public static void saveTimetable(Student student) throws IOException {

        String timetableFilePath = System.getProperty("user.dir") + "/data/timetable.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(timetableFilePath))) {

            // Write the new content to the file
            writer.write("TimetableForCurrentSem");
            writer.newLine();

            //latest info
            student.updateTimetable();

            ArrayList<ModuleWeekly> currentSemesterModules = student.getTimetable().getCurrentSemesterModulesWeekly();
            for (ModuleWeekly module : currentSemesterModules) {
                for (Event event : module.getWeeklyTimetable()) {
                    writer.write(event.toSave());
                    writer.newLine();
                }
            }
        } catch (TimetableUnavailableException e) {
            //no events in timetable, do nothing
        }
    }

    // Below this comment are standard file methods

    /**
     * Takes in the location of the file in question and returns whether the file exist
     *
     * @param filePath
     * @return return true if the file exist,return false otherwise
     */
    public static boolean isFileExist(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    /**
     * This method takes in a path and creates a directory at that location. Should the
     * directory already exist, no new directory will be created.
     *
     * @param folderPath the location of where the directory should be created
     */
    public static void createDirectory(String folderPath) {

        File folder = new File(folderPath);
        if (folder.mkdir()) {
            //System.out.println("Folder created successfully.");
        } else {
            //System.out.println("Folder already exists");
        }
    }

    /**
     * This method takes in the path of a directory and creates a file 'fileName' in
     * the directory. Should the file already exist, no new file will be created.
     *
     * @param directoryPath the location of the directory where the file should be created
     * @param fileName the name of the file to be created
     */
    public static void createFileInDirectory(String directoryPath, String fileName) {

        // Create the full path to the text file
        String filePath = directoryPath + "/" + fileName;

        File file = new File(filePath);

        try {
            // Create the file
            if (file.createNewFile()) {
                //System.out.println("Text file created successfully at: " + filePath);
            } else {
                //System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An IOException occurred: " + e.getMessage());
        }
    }

    /**
     * This method takes in the path of a txt file and adds 'textToAdd' to the last line
     * of the file
     *
     * @param filePath location of the file to be edited
     * @param textToAdd String to be added to the end of the txt file
     */
    public static void addTextToFile(String filePath, String textToAdd) {
        try {
            // Create a FileWriter object with the specified file path in append mode (true).
            FileWriter fileWriter = new FileWriter(filePath, true);

            // Create a BufferedWriter to efficiently write text.
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // Write the text to the file.
            bufferedWriter.write(textToAdd);

            // Write a newline character to separate lines.
            bufferedWriter.newLine();

            // Close the BufferedWriter to release resources.
            bufferedWriter.close();

            //System.out.println("Text added to the file successfully.");
        } catch (IOException e) {
            System.out.println("An IOException occurred: " + e.getMessage());
        }
    }
}
