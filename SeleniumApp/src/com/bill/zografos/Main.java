package com.bill.zografos;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static String[] site = {"https://www.orbitz.com/Hotel-Search?", "https://www.travelocity.com/Hotel-Search?", "https://www.cheaptickets.com/Hotel-Search?", "https://www.expedia.com/Hotel-Search?"};
    private static String[] siteName = {"Orbitz", "Travelocity", "Cheaptickets", "Expedia"};

    public synchronized static String getSite(int i) {
        return site[i];
    }
    public synchronized static String getSiteName(int i) {
        return siteName[i];
    }

    private static synchronized String getCurrentDay (){
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = calendar.getTime();
        String todayStr = formatter.format(date);
        return todayStr;
    }

    private static synchronized String getNextDay () {
        Date dt = new Date();
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.setTime(dt);
        c.add(Calendar.DATE, 1);

        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = c.getTime();
        String nextDayStr = formatter.format(date);
        return nextDayStr;
    }

    public synchronized static void showHotelNum(String siteName, int num) {
        System.out.println(siteName+" Total Hotels: " + num+"\n");
    }

    public static synchronized void showData(String siteName, String hotel, String city, int price, int review, String reviewsNum) {
        System.out.println(siteName+" Hotel: " + hotel);
        System.out.println(siteName+" City: " + city);
        System.out.println(siteName+" Price: " + price);
        System.out.println(siteName+" Avg Review: " + review);
        System.out.println(siteName+" Reviews: " + reviewsNum);
        System.out.print("\n");
    }

    public static synchronized void showFinalData(String siteName, double priceAvg, String review, int reviewsSum) {
        // show avg price, avg review, total reviews
        System.out.println(siteName + "'s average price: " + priceAvg);
        System.out.println(siteName + "'s average review: " + review);
        System.out.println(siteName + "'s total reviews: " + reviewsSum);
        System.out.println("\n");
    }

    public static void main(String[] args) {

        // prepare database
        Storage storage = Storage.getInstance();
        storage.create();

        while (true) {

            // Read user's input
            Scanner scanner = new Scanner(System.in);
            System.out.println("Choose an option:");
            System.out.println("1. Find hotels for a destination");
            System.out.println("2. Find average price for a destination");
            System.out.println("3. Find the cheapest hotel for a destination");
            System.out.println("4. Find the cheapest hotel from a specific site for a destination");
            System.out.println("5. Find available hotels in a specific city for a destination");
            System.out.println("6. Find all hotels for a specific site");
            System.out.println("7. Find total reviews for a destination");
            System.out.println("8. Find average review for a destination");
            System.out.println("9. Exit");
            String input = scanner.nextLine();

            // exit
            if (input.equals("9")) {
                System.out.println("Exiting...");
                break;
            }

            // read the destination
            if (input.equals("1")) {

                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                if (!storage.destinationExists(destination)) {
                    // prepare task excecutor
                    ExecutorService service = Executors.newFixedThreadPool(4);

                    // prepare tasks
                    List<Callable<Boolean>> callableTasks = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        int finalI = i;
                        String finalDestination = destination; // for insert!

                        Callable<Boolean> callableTask = () -> {
                            // get current site
                            String site = getSite(finalI);
                            String siteName = getSiteName(finalI);

                            // prepare browser
                            WebDriver driver = null;
                            try {
                                driver = new RemoteWebDriver(
                                        new URL("http://127.0.0.1:9515"),
                                        new ChromeOptions());
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            FluentWait wait = new WebDriverWait(driver, 20);

                            // goto to the ith site
                            String urlParams = "destination="+finalDestination+"&startDate="+getCurrentDay()+"&endDate="+getNextDay()+"&adults=1+room%2C+2+travelers";
                            driver.navigate().to(site + urlParams);

                            // get the list of hotels when the list is loaded
                            wait.until(ExpectedConditions.visibilityOfElementLocated(
                                    By.cssSelector(".results-list > li:last-of-type")));
                            WebElement olElem = driver.findElement(By.cssSelector(".results-list"));
                            List<WebElement> liElems = olElem.findElements(By.tagName("li"));

                            showHotelNum(siteName, liElems.size());

                            // find prices, reviews and number of reviews from each hotel
                            List<Integer> prices = new ArrayList<>();
                            List<Integer> reviews = new ArrayList<>();
                            List<Integer> totalReviews = new ArrayList<>();

                            liElems.forEach(el -> {
                                List<WebElement> hotelEl = el.findElements(
                                        By.cssSelector("h3[data-stid=\"content-hotel-title\"]"));
                                List<WebElement> cityEl = el.findElements(
                                        By.cssSelector("div[data-test-id=\"content-hotel-neighborhood\"]"));
                                List<WebElement> priceEl = el.findElements(
                                        By.cssSelector("span[data-stid=\"content-hotel-lead-price\"]"));
                                List<WebElement> reviewEl = el.findElements(
                                        By.cssSelector("span[data-stid=\"content-hotel-reviews-superlative\"]"));
                                List<WebElement> totalReviewsEl = el.findElements(
                                        By.cssSelector("span[data-stid=\"content-hotel-reviews-total\"]"));

                                // do we have review elem, price elem, number of reviews elem city and hotel
                                if (!priceEl.isEmpty()
                                        && !reviewEl.isEmpty()
                                        && !totalReviewsEl.isEmpty()
                                        && !hotelEl.isEmpty()
                                        && !cityEl.isEmpty()) {

                                    int p = 0;
                                    String r = "None";
                                    int r2 = 0;
                                    String h = "Unknown";
                                    String c = "Unknown";

                                    if (!priceEl.get(0).getText().isEmpty()) {
                                        int price = Integer.decode(priceEl.get(0).getText().replace("$", ""));
                                        prices.add(price);
                                        p = price;
                                    }

                                    if (!totalReviewsEl.get(0).getText().isEmpty()) {
                                        int reviewsNum = Integer.decode(
                                                totalReviewsEl.get(0).getText()
                                                        .replace("(", "")
                                                        .replace(")", "")
                                                        .replace("reviews", "")
                                                        .replace(" ", "")
                                                        .replace(",", ""));
                                        totalReviews.add(reviewsNum);
                                        r2 = reviewsNum;
                                    }

                                    if (!reviewEl.get(0).getText().isEmpty()) {
                                        String review = reviewEl.get(0).getText();
                                        reviews.add(storage.getReviewNum(review));
                                        r = review;
                                    }

                                    if (!hotelEl.get(0).getText().isEmpty()) {
                                        String hotel = hotelEl.get(0).getText();
                                        h = hotel;
                                    }

                                    if (!cityEl.get(0).getText().isEmpty()) {
                                        String city = cityEl.get(0).getText();
                                        c = city;
                                    }

                                    if (p != 0 && !r.equals("None") && r2 != 0 && !h.equals("Unknown") && !c.equals("Unknown")) {
                                        try {
                                            storage.insert(h, finalDestination, c, siteName, p, r, r2);
                                        } catch (ClassNotFoundException e) {
                                            System.out.print("Something gone wrong please try again.");
                                        }
                                    }

                                    showData(siteName, h, c, p, r2, r);
                                }
                            });

                            // find the average price
                            Integer[] priceArray = prices.toArray(new Integer[0]);
                            double priceAvg = Arrays.stream(priceArray)
                                    .mapToInt(Integer::intValue)
                                    .average()
                                    .orElse(Double.NaN);

                            // find the average review
                            Integer[] reviewArray = reviews.toArray(new Integer[0]);
                            double reviewAvg = Arrays.stream(reviewArray)
                                    .mapToInt(Integer::intValue)
                                    .average()
                                    .orElse(Double.NaN);
                            int review = (int) reviewAvg;

                            // find the number of total reviews
                            Integer[] totalReviesArray = totalReviews.toArray(new Integer[0]);
                            int reviewsSum = Arrays.stream(totalReviesArray)
                                    .mapToInt(Integer::intValue)
                                    .sum();

                            showFinalData(siteName, priceAvg, storage.getReview(review), reviewsSum);

                            // exit
                            driver.quit();

                            return true;
                        };

                        callableTasks.add(callableTask);

                    }

                    List<Future<Boolean>> futures = null;
                    try {
                        futures = service.invokeAll(callableTasks);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    List<Boolean> results = new ArrayList<>();

                    futures.forEach(future -> {
                        try {
                            results.add(future.get()); // wait for each future to finish
                        } catch (InterruptedException e) {
                            results.add(false); // something gone wrong
                        } catch (ExecutionException e) {
                            results.add(false); // something gone wrong
                        }
                    });

                    for (int i = 0; i < results.size(); i++) {
                        System.out.println("Thread " + i + " finished successfully\n");
                    }

                    service.shutdownNow();

                } else {
                    try {
                        storage.showAvgPriceTotalReviews(destination);
                    } catch (Exception e) {
                        System.out.print("Something gone wrong please try again.");
                    }
                }
            }

            if (input.equals("2")) {
                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                double avgPrice = 0;
                try {
                    avgPrice = storage.findAvgPriceForDestination(destination);
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                }
                System.out.println("Average price for "+destination+" is:"+avgPrice);
            }

            if (input.equals("3")) {
                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                Map<String, Object> hotel = null;
                try {
                    hotel = storage.findCheapestHotel(destination);
                    if (hotel != null) {
                        showData(
                                hotel.get("siteName").toString(),
                                hotel.get("name").toString(),
                                hotel.get("city").toString(),
                                (int) hotel.get("avgPrice"),
                                (int) hotel.get("totalReviews"),
                                hotel.get("avgReview").toString());
                    } else {
                        System.out.print("No results.\n");
                    }
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                }
            }

            if (input.equals("4")) {
                // read site name
                System.out.println("Enter the site's name:");
                String siteName = scanner.nextLine();

                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                Map<String, Object> hotel = null;
                try {
                    hotel = storage.findCheapestHotelFromSite(siteName, destination);
                    if (hotel != null) {
                        showData(
                                siteName,
                                hotel.get("name").toString(),
                                hotel.get("city").toString(),
                                (int) hotel.get("avgPrice"),
                                (int) hotel.get("totalReviews"),
                                hotel.get("avgReview").toString());
                    } else {
                        System.out.print("No results.\n");
                    }
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                }
            }

            if (input.equals("5")) {
                // read city
                System.out.println("Enter the city:");
                String city = scanner.nextLine();

                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                List<Map<String, Object>> hotels = null;
                try {
                    hotels = storage.findHotelsByCity(city, destination);
                    if (hotels != null) {
                        hotels.forEach(hotel -> {
                            showData(
                                    hotel.get("siteName").toString(),
                                    hotel.get("name").toString(),
                                    hotel.get("city").toString(),
                                    (int) hotel.get("avgPrice"),
                                    (int) hotel.get("totalReviews"),
                                    hotel.get("avgReview").toString());
                        });
                    } else{
                        System.out.print("No results.\n");
                    }
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                }
            }

            if (input.equals("6")) {
                // read site name
                System.out.println("Enter the site's name:");
                String siteName = scanner.nextLine();

                try {
                    storage.findHotelsBySiteName(siteName, (hotel) -> {
                        System.out.println("");
                        showData(
                                hotel.get("siteName").toString(),
                                hotel.get("name").toString(),
                                hotel.get("city").toString(),
                                (int) hotel.get("avgPrice"),
                                (int) hotel.get("totalReviews"),
                                hotel.get("avgReview").toString());
                    });
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                }
            }

            if (input.equals("7")) {
                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                double avgPrice = 0;
                try {
                    avgPrice = storage.findTotalReviewsForDestination(destination);
                    if (avgPrice != Double.NaN) {
                        System.out.println("Average price for " + destination + " is:" + avgPrice);
                    } else {
                        System.out.print("No results.\n");
                    }
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                }
            }

            if (input.equals("8")) {
                // read destination
                System.out.println("Enter your destination:");
                String destination = scanner.nextLine();

                String avgReview = null;
                try {
                    avgReview = storage.findAvgReviewForDestination(destination);
                    if (!avgReview.equals("")) {
                        System.out.println("Average review for " + destination + " is:" + avgReview);
                    } else {
                        System.out.print("No results.\n");
                    }
                } catch (SQLException e) {
                    System.out.print("Something gone wrong please try again.");
                } catch (ClassNotFoundException e) {
                    System.out.print("Something gone wrong please try again.");
                }
            }
        }

        storage.clear();
    }
}
