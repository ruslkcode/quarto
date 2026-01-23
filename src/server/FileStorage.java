package server;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
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
            for (String name : ratings.keySet()){
                int mmr = ratings.get(name);
                bufferedWriter.write(name + ":" + mmr);
                bufferedWriter.newLine();
            }

        } catch (IOException e) {
            e.getMessage();
        }

    }
    public int getMmr(String username){
        return ratings.getOrDefault(username, 1000);
    }

    public void updateMmr(String username, int point){
        int currentMmr = getMmr(username);
        int newMmr = currentMmr + point;
        if (newMmr < 0) newMmr = 0;
        ratings.put(username, newMmr);
        save();
    }


    public String getRankingsForProtocol() {
        java.util.List<java.util.Map.Entry<String, Integer>> list = new java.util.ArrayList<>(ratings.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        StringBuilder sb = new StringBuilder();

        for (java.util.Map.Entry<String, Integer> entry : list) {
            sb.append(protocol.Protocol.SEPARATOR)
                    .append(entry.getKey())
                    .append(protocol.Protocol.SEPARATOR)
                    .append(entry.getValue());
        }

        // Пример результата: "~Alice~1200~Bob~1000"
        return sb.toString();
    }


}
