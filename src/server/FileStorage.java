package server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileStorage {
    private String FILE_NAME = "ratings.txt";
    private Map<String, Integer> ratings = new HashMap<>();

    public void FileStorage(){
        load();
    }

    public void load(){
        File file = new File(FILE_NAME);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
               e.getMessage();
            }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String username = parts[0];
                    int mmr = Integer.parseInt(parts[1]);
                    ratings.put(username, mmr);
                }
            }
            System.out.println("Ratings loaded from file!");
        } catch (IOException e) {
            System.out.println("Error loading ratings: " + e.getMessage());
        }
    }

    public void save(){
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FILE_NAME))){
            for ()

        } catch (IOException e) {
            e.getMessage();
        }
    }

}
