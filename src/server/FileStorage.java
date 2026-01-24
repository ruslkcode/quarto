package server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * FileStorage is responsible for persistent storage of player ratings (MMR).
 */
public class FileStorage {

    /*@
      @ private invariant FILE_NAME != null;
      @ private invariant ratings != null;
      @*/

    /** Name of the file used to store ratings */
    private String FILE_NAME = "ratings.txt";

    /** Map storing usernames and their corresponding MMR values */
    private Map<String, Integer> ratings = new HashMap<>();

    /**
     * Initializes the file storage and loads existing ratings from file.
     */
    public void FileStorage(){
        load();
    }

    /**
     * Loads player ratings from the file.
     * <p>
     * If the file does not exist, it is created automatically.
     * Each line is expected to follow the format: username:mmr
     */
    /*@
      @ assignable ratings;
      @*/
    public void load(){
        File file = new File(FILE_NAME);

        // Create file if it does not exist
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.getMessage();
            }
            return;
        }

        // Read ratings line by line
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

    /**
     * Saves all current ratings to the file.
     * Each rating is written in the format: username:mmr
     */
    /*@
      @ assignable \nothing;
      @*/
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

    /**
     * Returns the MMR of a given user.
     *
     * @param username player username
     * @return stored MMR value, or 1000 if the user is not found
     */
    /*@
      @ ensures \result >= 0;
      @*/
    public int getMmr(String username){
        return ratings.getOrDefault(username, 1000);
    }

    /**
     * Updates the MMR of a user by adding the given number of points.
     * The resulting MMR value cannot be negative.
     * After updating, the ratings are saved to file.
     *
     * @param username player username
     * @param point MMR change (positive or negative)
     */
    /*@
      @ requires username != null;
      @ assignable ratings;
      @*/
    public void updateMmr(String username, int point){
        int currentMmr = getMmr(username);
        int newMmr = currentMmr + point;

        // Prevent negative MMR values
        if (newMmr < 0) newMmr = 0;

        ratings.put(username, newMmr);
        save();
    }

    /**
     * Returns player rankings formatted for protocol transmission.
     * Rankings are sorted in descending order of MMR.
     *
     * @return protocol-formatted rankings string
     */
    /*@
      @ ensures \result != null;
      @*/
    public String getRankingsForProtocol() {

        java.util.List<java.util.Map.Entry<String, Integer>> list =
                new java.util.ArrayList<>(ratings.entrySet());

        // Sort players by MMR in descending order
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        StringBuilder sb = new StringBuilder();

        for (java.util.Map.Entry<String, Integer> entry : list) {
            sb.append(protocol.Protocol.SEPARATOR)
                    .append(entry.getKey())
                    .append(protocol.Protocol.SEPARATOR)
                    .append(entry.getValue());
        }

        // Example result: "~Alice~1200~Bob~1000"
        return sb.toString();
    }
}
