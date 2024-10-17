package com.revature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SeleniumTest {

    private WebDriver webDriver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", "driver/chromedriver"); // linux_64

        File file = new File("src/main/java/com/revature/index.html");
        String path = "file://" + file.getAbsolutePath();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        webDriver = new ChromeDriver(options);
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        webDriver.get(path);
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

    }

    @AfterEach
    public void tearDown() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }

   // #1: The user should be able to search for books.
    @Test
    public void testSearchBooksSucceeds() {
       // Perform setup that allows tests to find searchBooks function
       JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriver;
       wait.until(
               driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));

       // Test success with query #1
       String script = "return searchBooks(arguments[0], arguments[1]).then(JSON.stringify);";
       String actual1 = (String) jsExecutor.executeScript(script, "harry potter", "title");

       if (actual1 == null) {
           fail("No results provided by the SearchBooks function.");
       }
      
       String expectedPartial1 = "[{\"author_name\":[\"J.K.Rowling\"],\"cover_i\":10521270,\"ebook_access\":\"borrowable\",\"first_publish_year\":1997,\"isbn\":[";
       Assertions.assertTrue(actual1.toLowerCase().replaceAll("[ \n]", "").contains(expectedPartial1.toLowerCase()),
               "Results of searchBooks not as expected.");

       // Test success with query #2
       String actual2 = (String) jsExecutor.executeScript(script, "poe",
               "author");
       String expectedPartial2 = "[{\"author_name\":[\"EdgarAllanPoe\"],\"cover_i\":11774455,\"ebook_access\":\"public\",\"first_publish_year\":1895,\"isbn\":[";
       Assertions.assertTrue(actual2.toLowerCase().replaceAll("[ \n]", "").contains(expectedPartial2.toLowerCase()),
               "Results of searchBooks not as expected.");

       // Test success with query #3
       String actual3 = (String) jsExecutor.executeScript(script,
               "9781472539342", "isbn");
       String expectedPartial3 = "[{\"author_name\":[\"CormacMcCarthy\",\"TomStechschulte\"],\"cover_i\":198120,\"ebook_access\":\"borrowable\",\"first_publish_year\":2006,\"isbn\":[";
       Assertions.assertTrue(actual3.toLowerCase().replaceAll("[ \n]", "").contains(expectedPartial3.toLowerCase()),
               "Results of searchBooks not as expected.");

       // Assert only 10 books or less are returned from the function
       Object actual4 = jsExecutor.executeScript("return searchBooks(arguments[0], arguments[1]);", "9781725757264",
               "isbn");
       Assertions.assertTrue(((List) actual4).size() <= 10, "The list of books returned is over 10 elements in size");
    }
    

    // #2: Our application should be able to display book search results.
    @Test
    public void testDisplayOfBookSearchResults() {
        // Attempt to find the correct UI elements for use
        WebElement searchInput = null;
        WebElement searchType = null;
        WebElement searchButton = null;

        try {
            searchInput = webDriver.findElement(By.id("search-input"));
            searchType = webDriver.findElement(By.id("search-type"));
            searchButton = webDriver.findElement(By.id("search-button"));

        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        // If found, use UI elements to send a query
        searchType.sendKeys("title");
        searchInput.sendKeys("Test");
        searchButton.click();

        // Ensure the book list has loaded and is populated
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));

        WebElement bookList = webDriver.findElement(By.id("book-list"));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#book-list > li")));
        List<WebElement> books = bookList.findElements(By.tagName("li"));
        assertFalse(books.isEmpty(), "No books displayed.");

        // Check that each book as correct UI elements within it
        books.forEach(book -> {
            assertNotNull(book.findElement(By.className("title-element")).getText());
            assertNotNull(book.findElement(By.className("cover-element")).isDisplayed());
            assertNotNull(book.findElement(By.className("rating-element")).getText());
            assertNotNull(book.findElement(By.className("ebook-element")).getText());
        });
    }

    // #3: The application should handle the event of the user performing a book
    // search.
    @Test
    public void testSearchFormElementsIncluded() {
        // Attempt to find the correct UI element for use
        WebElement searchForm = null;

        try {
            searchForm = webDriver.findElement(By.id("search-form"));
        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        // Assert it has the correct elements within it
        assertNotNull(searchForm.findElement(By.id("search-input")));
        assertNotNull(searchForm.findElement(By.id("search-type")));
        assertNotNull(searchForm.findElement(By.id("search-button")));

        // Check the select element's options and make assertions
        List<WebElement> options = searchForm.findElements(By.tagName("option"));
        boolean selectOptionsValid = true;
        boolean optionTitleExists = false;
        boolean optionAuthorExists = false;
        boolean optionIsbnExists = false;

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getAttribute("value").toLowerCase().equals("title")) {
                optionTitleExists = true;
            } else if (options.get(i).getAttribute("value").toLowerCase().equals("author")) {
                optionAuthorExists = true;
            } else if (options.get(i).getAttribute("value").toLowerCase().equals("isbn")) {
                optionIsbnExists = true;
            } else {
                selectOptionsValid = false;
            }
        }
        assertTrue(selectOptionsValid, "One of the options of your select element is an invalid type");
        assertTrue(optionTitleExists, "The option with value 'title' does not exist.");
        assertTrue(optionAuthorExists, "The option with value 'author' does not exist.");
        assertTrue(optionIsbnExists, "The option with value 'isbn' does not exist.");
    }

    // 4: The application should handle the event of the user clicking on a book
    // returned from a search result.
    @Test
    public void testDisplayDetailedBookInformation() {
        // Attempt to find the correct UI element for use
        WebElement searchInput = null;
        WebElement searchType = null;
        WebElement searchButton = null;

        try {
            searchInput = webDriver.findElement(By.id("search-input"));
            searchType = webDriver.findElement(By.id("search-type"));
            searchButton = webDriver.findElement(By.id("search-button"));

        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        // Use elements to send a query
        searchType.sendKeys("title");
        searchInput.sendKeys("test");
        searchButton.click();

        // Wait until the book list has loaded
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));

        // Wait until the first book has loaded
        WebElement firstBookItem = wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("#book-list > li:first-child")));

        // Now interact with the element and make assertions
        firstBookItem.click();
        WebElement selectedBook = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("selected-book")));
        assertNotNull(selectedBook, "Element with id of selected-book cannot be found.");
        assertTrue(selectedBook.isDisplayed(), "Element with id of selected-book is not displayed.");

        // Assert booklist is not visible
        WebElement bookList = webDriver.findElement(By.id("book-list"));
        assertFalse(bookList.isDisplayed(), "If a single book is clicked, the booklist should not be visible");

        // Wait for cover to load in visually, it can take a few seconds
        WebElement coverElement = selectedBook.findElement(By.className("cover-element"));
        wait.until(ExpectedConditions.visibilityOf(coverElement));

        // Assert the book's necessary data is visible
        assertNotNull(selectedBook.findElement(By.className("title-element")).getText());
        assertNotNull(selectedBook.findElement(By.className("author-element")).getText());
        assertTrue(selectedBook.findElement(By.className("cover-element")).isDisplayed());
        assertNotNull(selectedBook.findElement(By.className("rating-element")).getText());
        assertNotNull(selectedBook.findElement(By.className("ebook-element")).getText());
        assertNotNull(selectedBook.findElement(By.className("published-element")).getText());
        assertNotNull(selectedBook.findElement(By.className("isbn-element")).getText());
    }

    // 5: Our application’s search results should be sortable by rating.
    @Test
    public void testHandleSort() {
        // Attempt to find the correct UI element for use
        WebElement searchInput = null;
        WebElement searchType = null;
        WebElement searchButton = null;

        try {
            searchInput = webDriver.findElement(By.id("search-input"));
            searchType = webDriver.findElement(By.id("search-type"));
            searchButton = webDriver.findElement(By.id("search-button"));

        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        // Use elements to send a query
        searchType.sendKeys("title");
        searchInput.sendKeys("test");
        searchButton.click();

        // Wait for the booklist to load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));
        wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#book-list li"), 10));

        // Use the sort-rating button
        WebElement button = null;
        try {
            button = webDriver.findElement(By.id("sort-rating"));

        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }
        button.click();

        // Check booklist and make assertions
        WebElement bookList = webDriver.findElement(By.id("book-list"));
        List<WebElement> books = bookList.findElements(By.tagName("li"));
        assertFalse(books.isEmpty(), "No books displayed.");

        for (int i = 0; i < books.size() - 1; i++) {
            String ratingA = books.get(i).findElement(By.className("rating-element")).getText();
            float ratingOfCurrentBook = Float
                    .parseFloat(ratingA.replaceAll("[^0-9.]", ""));

            String ratingB = books.get(i + 1).findElement(By.className("rating-element")).getText();
            System.out.println(ratingA + " and " + ratingB);
            float ratingOfNextBook = Float
                    .parseFloat(ratingB.replaceAll("[^0-9.]", ""));

            if (ratingOfCurrentBook < ratingOfNextBook) {
                fail("Books are not sorted.");
            }

        }
    }

    // 6: Our application’s search results should be filterable by whether or not
    // the results are available as ebooks.
    @Test
    public void testHandleFilter() {
        // Attempt to find the correct UI element for use
        WebElement searchInput = null;
        WebElement searchType = null;
        WebElement searchButton = null;

        try {
            searchInput = webDriver.findElement(By.id("search-input"));
            searchType = webDriver.findElement(By.id("search-type"));
            searchButton = webDriver.findElement(By.id("search-button"));

        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        // Use elements to send a query
        searchType.sendKeys("title");
        searchInput.sendKeys("test");
        searchButton.click();

        // Find and use filter checkbox
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));

        // Wait for the booklist to load
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("book-list")));
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#book-list li"), 0));

        WebElement checkbox = null;
        try {
            checkbox = webDriver.findElement(By.id("ebook-filter"));
        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        checkbox.click();

        // Check booklist
        WebElement bookList = webDriver.findElement(By.id("book-list"));
        List<WebElement> books = bookList.findElements(By.tagName("li"));
        assertFalse(books.isEmpty(), "No books displayed.");

        for (int i = 0; i < books.size() - 1; i++) {
            WebElement book = books.get(i);
            String ebookValue = book.findElement(By.className("ebook-element")).getText().toLowerCase();
            if (book.isDisplayed() && !ebookValue.contains("borrowable")) {
                fail("A book's ebook value is not 'borrowable'");
            }
        }
        ;
    }

    // Semantic elements should be included in HTML for web accessibility.
    @Test
    public void testSemanticHtmlElements() {
        String htmlFileContent = TestingUtils.getContent("index.html");

        String[] elementsNeeded = { "article", "aside", "details", "figcaption", "figure", "footer", "header", "main",
                "nav", "section" };
        int count = 0;

        for (int i = 0; i < elementsNeeded.length; i++) {
            if (htmlFileContent.contains(elementsNeeded[i])) {
                count++;
            }
        }

        assertTrue(count > 2, "More semantic HTML elements are required");
    }

    // 8: CSS styling should be used to create a responsive web application.
    @Test
    public void testResponsiveDesignIsIncluded() {
        String cssFileContent = TestingUtils.getContent("styles.css");

        String[] elementsNeeded = { "@media", "grid", "flex" };
        boolean isResponsive = false;

        for (int i = 0; i < elementsNeeded.length; i++) {
            if (cssFileContent.contains(elementsNeeded[i])) {
                isResponsive = true;
            }
        }

        assertTrue(isResponsive, "Responsive CSS styles need to be included.");
    }
}

class TestingUtils {
    public static String getContent(String filename) {
        String content = "";
        try {
            content = Files.readString(Paths.get("./src/main/java/com/revature/" + filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}