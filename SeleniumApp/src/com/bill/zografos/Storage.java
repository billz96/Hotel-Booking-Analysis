package com.bill.zografos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;

/**
 * Created by vasilis on 23/10/2020.
 */
public class Storage {

    private static String[] sites = {"Orbitz", "Travelocity", "Cheaptickets", "Expedia"};
    private static String URL = "jdbc:sqlite:selenium-app.db";
    private static Map<String, Integer> Review;
    private static Map<Integer, String> ReviewR;
    private static volatile Storage instance;

    private Storage() {
        initReviewMap();
        initReviewRMap();
    }

    public static Storage getInstance() {
        Storage result = instance;
        if (result != null) {
            return result;
        }
        synchronized(Storage.class) {
            if (instance == null) {
                instance = new Storage();
            }
            return instance;
        }
    }

    private static void initReviewMap() {
        // Map reviews to values
        Review = new HashMap<>();
        Review.put("Excellent", 1);
        Review.put("Wonderful", 2);
        Review.put("Exceptional", 3);
        Review.put("Very Good", 4);
        Review.put("Good", 5);
    }

    private static void initReviewRMap() {
        // Review Map Reversed
        ReviewR = new HashMap<>();
        ReviewR.put(1, "Excellent");
        ReviewR.put(2, "Wonderful");
        ReviewR.put(3, "Exceptional");
        ReviewR.put(4, "Very Good");
        ReviewR.put(5, "Good");
    }

    public synchronized String getReview(int num) {
        return ReviewR.get(num);
    }
    public synchronized Integer getReviewNum(String name) {
        return Review.get(name);
    }

    public static void fetchHotels(String dest, Handler<Map<String, Object>> func) throws SQLException, ClassNotFoundException {
        String sql = "SELECT * FROM Hotels WHERE destination = '"+dest+"';";
        Connection con = DriverManager.getConnection(URL);
        Class.forName("org.sqlite.JDBC");
        Statement stmt = con.createStatement();

        // Execute the SQL Query. Store results in ResultSet
        ResultSet rs= stmt.executeQuery(sql);

        // While Loop to iterate through all data and print results
        while (rs.next()){
            String name = rs.getString("name");
            String city = rs.getString("city");
            String siteName = rs.getString("siteName");
            int avgPrice = rs.getInt("avgPrice");
            String avgReview = rs.getString("avgReview");
            int totalReviews = rs.getInt("totalReviews");

            Map<String, Object> hotel = new HashMap<>();
            hotel.put("name", name);
            hotel.put("city", city);
            hotel.put("siteName", siteName);
            hotel.put("avgPrice", avgPrice);
            hotel.put("avgReview", avgReview);
            hotel.put("totalReviews", totalReviews);

            func.run(hotel);
        }

        // closing DB Connection
        con.close();
    }

    public void create() {

        // create datatable IF NOT EXISTS
        String sql = new StringBuilder()
                    .append("CREATE TABLE IF NOT EXISTS Hotels (\n")
                    .append("	name text NOT NULL,\n")
                    .append("	city text NOT NULL,\n")
                    .append("	siteName text NOT NULL,\n")
                    .append("	destination text NOT NULL,\n")
                    .append("	avgPrice INTEGER NOT NULL,\n")
                    .append("	avgReview text NOT NULL,\n")
                    .append("	totalReviews INTEGER NOT NULL\n")
                    .append(");").toString();
        Connection con = null;
        try {
            con = DriverManager.getConnection(URL);
            Class.forName("org.sqlite.JDBC");
             Statement stmt = con.createStatement();
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public void clear() {
        File file = new File("./selenium-app.db");
        if (file.delete()) {
            System.out.println("Deleted the file: " + file.getName());
        } else {
            System.out.println("Failed to delete the file.");
        }
    }

    public boolean destinationExists(String destination) {
        // check if the given destination exists
        Connection con = null;
        try {
            con = DriverManager.getConnection(URL);
            Class.forName("org.sqlite.JDBC");
            Statement s = con.createStatement();

            ResultSet r = s.executeQuery("SELECT COUNT(*) AS rowcount FROM Hotels WHERE destination = '"+destination+"';");
            r.next();
            int count = r.getInt("rowcount");
            r.close();
            con.close();
            return count > 0;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public synchronized boolean insert(String hotel, String destination, String city, String siteName, int avgPrice, String avgReview, int totalReviews) throws ClassNotFoundException {
        // insert new destination+hotel
        Connection con = null;
        try {
            con = DriverManager.getConnection(URL);
            Class.forName("org.sqlite.JDBC");
            String sql = "INSERT INTO Hotels(name,city,siteName,destination,avgPrice,avgReview,totalReviews) VALUES(?,?,?,?,?,?,?);";
            PreparedStatement pstmt = con.prepareStatement(sql);

            pstmt.setString(1, hotel);
            pstmt.setString(2, city);
            pstmt.setString(3, siteName);
            pstmt.setString(4, destination);
            pstmt.setInt(5, avgPrice);
            pstmt.setString(6, avgReview);
            pstmt.setInt(7, totalReviews);
            pstmt.executeUpdate();
            pstmt.close();

            con.close();

            return true;
        } catch (SQLException e) {
            System.out.println("INSERT ERROR: "+e.getMessage());
            return false;
        }
    }

    public void findHotelsBySiteName(String site, Handler<Map<String, Object>> func) throws ClassNotFoundException, SQLException {
        String sql = "SELECT * FROM Hotels WHERE siteName = '"+site+"';";
        Connection con = DriverManager.getConnection(URL);
        Class.forName("org.sqlite.JDBC");
        Statement stmt = con.createStatement();

        // Execute the SQL Query. Store results in ResultSet
        ResultSet rs= stmt.executeQuery(sql);

        // While Loop to iterate through all data and print results
        while (rs.next()){
            String name = rs.getString("name");
            String city = rs.getString("city");
            String siteName = rs.getString("siteName");
            int avgPrice = rs.getInt("avgPrice");
            String avgReview = rs.getString("avgReview");
            int totalReviews = rs.getInt("totalReviews");

            Map<String, Object> hotel = new HashMap<>();
            hotel.put("name", name);
            hotel.put("city", city);
            hotel.put("siteName", siteName);
            hotel.put("avgPrice", avgPrice);
            hotel.put("avgReview", avgReview);
            hotel.put("totalReviews", totalReviews);

            func.run(hotel);
        }

        // closing DB Connection
        con.close();
    }

    public List<Map<String, Object>> findHotelsByCity(String city, String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
        });

        List<Map<String, Object>> hotels2 = hotels.stream().filter(h -> h.get("city").toString().equals(city)).collect(Collectors.toList());

        return hotels2;
    }

    public Map<String, Object> findCheapestHotelFromSite(String site, String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
        });

        List<Map<String, Object>> hotels2 = hotels.stream().filter(h -> h.get("siteName").toString().equals(site)).collect(Collectors.toList());

        hotels2.sort(Comparator.comparingInt(h -> (int) h.get("avgPrice")));

        return (hotels.size() > 0) ? hotels.get(0) : null ;
    }

    public Map<String, Object> findCheapestHotel(String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
        });

        hotels.sort(Comparator.comparingInt(h -> (int) h.get("avgPrice")));

        return (hotels.size() > 0) ? hotels.get(0) : null ;
    }

    public double findAvgPriceForDestination(String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
        });

        List<Integer> prices = new ArrayList<>();
        hotels.forEach(hotel -> {
            prices.add((int) hotel.get("avgPrice"));
        });

        Integer[] priceArray = prices.toArray(new Integer[0]);
        double priceAvg = Arrays.stream(priceArray)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);
        return priceAvg;
    }

    public void showHotelNum(String siteName, List<Map<String, Object>> hotels) {
        hotels = hotels.stream()
                .filter(h -> h.get("siteName").toString().equals(siteName))
                .collect(Collectors.toList());
        System.out.println(siteName+" total Hotels: " + hotels.size());
    }

    public void showAvgPrice(String siteName, String destination, List<Map<String, Object>> hotels) {
        // avg price per site
        double finalAvgPrice = hotels
                .stream()
                .filter(h -> h.get("siteName").toString().equals(siteName))
                .mapToInt(h -> (int) h.get("avgPrice"))
                .average()
                .orElse(Double.NaN);
        System.out.println(siteName+" avg price for "+destination+": "+ (int)finalAvgPrice);
    }

    public void showTotalReviews(String siteName, String destination, List<Map<String, Object>> hotels) {
        // total reviews per site
        int reviews = hotels
                .stream()
                .filter(h -> h.get("siteName").toString().equals(siteName))
                .mapToInt(h -> (int) h.get("totalReviews"))
                .sum();
        System.out.println(siteName+" total reviews for "+destination+": "+reviews+"\n");
    }

    public void showAvgPriceTotalReviews(String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
            String siteName = hotel.get("siteName").toString();
            System.out.println("");
            System.out.println(siteName+" "+hotel.get("name"));
            System.out.println(siteName+" "+hotel.get("avgPrice"));
            System.out.println(siteName+" "+hotel.get("avgReview").toString());
            System.out.println(siteName+" "+hotel.get("totalReviews"));
            System.out.println("");
        });

        for (int i = 0; i < 4; i++) {
            showHotelNum(sites[i], hotels);
            // avg price per site
            showAvgPrice(sites[i], destination, hotels);
            // total reviews per site
            showTotalReviews(sites[i], destination, hotels);
        }
    }

    public int findTotalReviewsForDestination(String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
        });

        final int[] totalReviews = {0};
        hotels.forEach(hotel -> {
            totalReviews[0] += (int)hotel.get("totalReviews");
        });

        return totalReviews[0];
    }

    public String findAvgReviewForDestination(String destination) throws SQLException, ClassNotFoundException {
        List<Map<String, Object>> hotels = new ArrayList<>();

        Storage.fetchHotels(destination, (hotel) -> {
            hotels.add(hotel);
        });

        final List<Integer> avgReviews = new ArrayList<>();
        hotels.forEach(hotel -> {
            int avgReview = Review.get(hotel.get("avgReview").toString());
            avgReviews.add(avgReview);
        });

        int avgReview = (int) avgReviews
                .stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);

        return ReviewR.getOrDefault(avgReview, "");
    }
}