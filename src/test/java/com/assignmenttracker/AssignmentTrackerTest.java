package com.assignmenttracker;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Selenium Test Suite for Assignment Tracker Web Application
 *
 * Application Stack:
 *   - Frontend : HTML + CSS + Vanilla JS
 *   - Backend  : Node.js + Express
 *   - Database : MongoDB
 *   - Port     : http://localhost:3000
 *
 * Tests use headless Chrome (required for Jenkins / AWS EC2).
 * WebDriverManager auto-downloads the correct ChromeDriver version.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AssignmentTrackerTest {

    // ─── Configuration ────────────────────────────────────────────────────────
    private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:3000");
    private static final int WAIT_TIMEOUT = 10; // seconds

    // ─── Shared Driver (one browser session for the full suite) ───────────────
    private static WebDriver driver;
    private static WebDriverWait wait;

    // ─── Setup & Teardown ─────────────────────────────────────────────────────

    @BeforeAll
    static void setupDriver() {
        // Auto-download correct ChromeDriver binary
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // ── Headless Chrome (REQUIRED for Jenkins / AWS EC2) ──
        options.addArguments("--headless=new");         // new headless mode (Chrome 112+)
        options.addArguments("--no-sandbox");           // required in Docker / EC2
        options.addArguments("--disable-dev-shm-usage"); // overcome limited /dev/shm
        options.addArguments("--disable-gpu");          // required on some Linux envs
        options.addArguments("--window-size=1920,1080"); // fixed viewport
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));

        System.out.println("✅ ChromeDriver started in headless mode.");
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) {
            driver.quit();
            System.out.println("✅ ChromeDriver closed.");
        }
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    /** Navigate to home page and wait until it is fully loaded. */
    private void goHome() {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
    }

    /**
     * Fill the Add-Assignment form on the home page and click Add.
     *
     * @param title    assignment title
     * @param subject  subject name
     * @param priority "Low" | "Medium" | "High"
     * @param dueDate  format: yyyy-MM-dd  (e.g. "2025-12-31")
     */
    private void addAssignment(String title, String subject,
                               String priority, String dueDate) {
        WebElement titleInput = driver.findElement(By.id("title"));
        titleInput.clear();
        titleInput.sendKeys(title);

        WebElement subjectInput = driver.findElement(By.id("subject"));
        subjectInput.clear();
        subjectInput.sendKeys(subject);

        Select prioritySelect = new Select(driver.findElement(By.id("priority")));
        prioritySelect.selectByVisibleText(priority);

        WebElement dueDateInput = driver.findElement(By.id("dueDate"));
        dueDateInput.clear();
        dueDateInput.sendKeys(dueDate);

        driver.findElement(By.xpath("//button[normalize-space()='Add']")).click();

        // Wait for table to refresh
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("assignmentList")));
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
    }

    /** Return all rows currently visible in the assignment table body. */
    private List<WebElement> getTableRows() {
        return driver.findElements(By.cssSelector("#assignmentList tr"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TEST CASES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * TC-01 – Page Title
     * Verify the browser tab shows the correct page title.
     */
    @Test
    @Order(1)
    @DisplayName("TC-01: Page title should be 'Assignment Dashboard'")
    void testPageTitle() {
        goHome();
        String pageTitle = driver.getTitle();
        assertEquals("Assignment Dashboard", pageTitle,
                "Page title mismatch.");
    }

    /**
     * TC-02 – Header Text
     * Verify the main heading "Smart Assignment Manager" is visible.
     */
    @Test
    @Order(2)
    @DisplayName("TC-02: Main heading should display 'Smart Assignment Manager'")
    void testMainHeading() {
        goHome();
        WebElement heading = driver.findElement(By.tagName("h1"));
        assertEquals("Smart Assignment Manager", heading.getText().trim(),
                "Main heading text mismatch.");
    }

    /**
     * TC-03 – Form Elements Present
     * Verify all input fields (title, subject, priority, dueDate) and the
     * Add button exist on the page.
     */
    @Test
    @Order(3)
    @DisplayName("TC-03: Add-assignment form elements should all be present")
    void testFormElementsPresent() {
        goHome();
        assertTrue(driver.findElement(By.id("title")).isDisplayed(),   "Title input missing.");
        assertTrue(driver.findElement(By.id("subject")).isDisplayed(), "Subject input missing.");
        assertTrue(driver.findElement(By.id("priority")).isDisplayed(),"Priority select missing.");
        assertTrue(driver.findElement(By.id("dueDate")).isDisplayed(), "Due-date input missing.");
        assertTrue(driver.findElement(
                By.xpath("//button[normalize-space()='Add']")).isDisplayed(),     "Add button missing.");
    }

    /**
     * TC-04 – Add Assignment (Happy Path)
     * Fill the form with valid data, submit, and verify the new row appears
     * in the assignments table.
     */
    @Test
    @Order(4)
    @DisplayName("TC-04: Adding a valid assignment should show it in the table")
    void testAddAssignment() {
        goHome();
        int rowsBefore = getTableRows().size();

        addAssignment("Math Homework", "Mathematics", "High", "2025-12-31");

        int rowsAfter = getTableRows().size();
        assertTrue(rowsAfter > rowsBefore,
                "Expected a new row in the table after adding an assignment.");
    }

    /**
     * TC-05 – Correct Data Rendered in Table
     * After adding an assignment, verify the title and subject appear in the
     * most-recently added row.
     */
    @Test
    @Order(5)
    @DisplayName("TC-05: Added assignment data should be correctly rendered in the table")
    void testAddedAssignmentDataInTable() {
        goHome();
        addAssignment("Physics Lab Report", "Physics", "Medium", "2025-11-15");

        List<WebElement> rows = getTableRows();
        assertFalse(rows.isEmpty(), "Table should not be empty.");

        // Check last row contains the expected data
        WebElement lastRow = rows.get(rows.size() - 1);
        String rowText = lastRow.getText();
        assertTrue(rowText.contains("Physics Lab Report"),
                "Title 'Physics Lab Report' not found in last table row.");
        assertTrue(rowText.contains("Physics"),
                "Subject 'Physics' not found in last table row.");
    }

    /**
     * TC-06 – Default Priority Option
     * When the page loads, the priority dropdown should default to "Low".
     */
    @Test
    @Order(6)
    @DisplayName("TC-06: Priority dropdown should default to 'Low'")
    void testDefaultPriorityOption() {
        goHome();
        Select prioritySelect = new Select(driver.findElement(By.id("priority")));
        String defaultOption = prioritySelect.getFirstSelectedOption().getText();
        assertEquals("Low", defaultOption, "Default priority should be 'Low'.");
    }

    /**
     * TC-07 – Priority Dropdown Options
     * The priority dropdown must contain exactly Low, Medium, and High.
     */
    @Test
    @Order(7)
    @DisplayName("TC-07: Priority dropdown should have Low, Medium, High options")
    void testPriorityDropdownOptions() {
        goHome();
        Select prioritySelect = new Select(driver.findElement(By.id("priority")));
        List<WebElement> options = prioritySelect.getOptions();

        assertEquals(3, options.size(), "Priority dropdown should have 3 options.");
        assertEquals("Low",    options.get(0).getText());
        assertEquals("Medium", options.get(1).getText());
        assertEquals("High",   options.get(2).getText());
    }

    /**
     * TC-08 – Empty Form Validation
     * Submitting the form without a title or subject should trigger a browser
     * alert saying "Please fill all required fields".
     */
    @Test
    @Order(8)
    @DisplayName("TC-08: Submitting empty form should show validation alert")
    void testEmptyFormValidation() {
        goHome();

        // Clear fields and click Add
        driver.findElement(By.id("title")).clear();
        driver.findElement(By.id("subject")).clear();
        driver.findElement(By.xpath("//button[normalize-space()='Add']")).click();

        // Expect a browser alert
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        String alertText = alert.getText();
        alert.accept(); // dismiss

        assertEquals("Please fill all required fields", alertText,
                "Validation alert text mismatch.");
    }

    /**
     * TC-09 – Search Functionality (Match Found)
     * Add a uniquely-named assignment, then search for its title and verify
     * only matching rows remain visible.
     */
    @Test
    @Order(9)
    @DisplayName("TC-09: Searching by title should filter the table correctly")
    void testSearchFilterMatch() {
        goHome();
        String uniqueTitle = "UniqueSearchTest";
        addAssignment(uniqueTitle, "Testing", "Low", "2025-10-01");

        WebElement searchInput = driver.findElement(By.id("search"));
        searchInput.clear();
        searchInput.sendKeys(uniqueTitle);
        driver.findElement(By.xpath("//button[normalize-space()='Search']")).click();

        List<WebElement> visibleRows = driver.findElements(
                By.cssSelector("#assignmentList tr:not([style*='display: none'])"));

        assertTrue(visibleRows.size() >= 1,
                "At least one row should be visible after searching for '" + uniqueTitle + "'.");

        for (WebElement row : visibleRows) {
            assertTrue(row.getText().toLowerCase()
                            .contains(uniqueTitle.toLowerCase()),
                    "Visible row does not contain the search term.");
        }
    }

    /**
     * TC-10 – Search Functionality (No Match)
     * Searching for a term that does not exist should hide all rows.
     */
    @Test
    @Order(10)
    @DisplayName("TC-10: Searching for non-existent term should hide all rows")
    void testSearchFilterNoMatch() {
        goHome();

        WebElement searchInput = driver.findElement(By.id("search"));
        searchInput.clear();
        searchInput.sendKeys("XYZNONEXISTENT99999");
        driver.findElement(By.xpath("//button[normalize-space()='Search']")).click();

        List<WebElement> visibleRows = driver.findElements(
                By.cssSelector("#assignmentList tr:not([style*='display: none'])"));

        assertEquals(0, visibleRows.size(),
                "No rows should be visible when search term doesn't match anything.");
    }

    /**
     * TC-11 – Mark Assignment as Completed
     * Click the "Complete" button on the first row and verify that row's
     * status cell changes to "Completed".
     */
    @Test
    @Order(11)
    @DisplayName("TC-11: Clicking 'Complete' should update status to 'Completed'")
    void testMarkAssignmentCompleted() {
        goHome();
        // Make sure at least one assignment exists
        addAssignment("TC11 Assignment", "Science", "High", "2025-09-30");

        List<WebElement> rows = getTableRows();
        assertFalse(rows.isEmpty(), "Table must have at least one row.");

        // Find the first Complete button and click it
        WebElement completeBtn = driver.findElement(
                By.xpath("(//button[normalize-space()='Complete'])[1]"));
        completeBtn.click();

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        // Check status cell of first row (4th column, index 3)
        WebElement statusCell = driver.findElement(
                By.cssSelector("#assignmentList tr:first-child td:nth-child(4)"));
        assertEquals("Completed", statusCell.getText().trim(),
                "Status should be 'Completed' after clicking Complete.");
    }

    /**
     * TC-12 – Delete Assignment (Confirm)
     * Click "Delete" on the last row, accept the confirmation dialog, and
     * verify the total row count decreases by one.
     */
    @Test
    @Order(12)
    @DisplayName("TC-12: Confirming delete should remove the assignment from the table")
    void testDeleteAssignmentConfirm() {
        goHome();
        addAssignment("TC12 Delete Me", "Chemistry", "Low", "2025-08-20");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int rowsBefore = getTableRows().size();

        // Click last delete button
        List<WebElement> deleteButtons = driver.findElements(
                By.xpath("//button[normalize-space()='Delete']"));
        assertFalse(deleteButtons.isEmpty(), "There should be at least one Delete button.");
        deleteButtons.get(deleteButtons.size() - 1).click();

        // Accept the confirmation dialog
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        int rowsAfter = getTableRows().size();
        assertEquals(rowsBefore - 1, rowsAfter,
                "Row count should decrease by 1 after confirming delete.");
    }

    /**
     * TC-13 – Cancel Delete
     * Click "Delete" on a row, dismiss (cancel) the confirmation dialog, and
     * verify the row count stays the same.
     */
    @Test
    @Order(13)
    @DisplayName("TC-13: Cancelling delete should keep the assignment in the table")
    void testDeleteAssignmentCancel() {
        goHome();
        addAssignment("TC13 Keep Me", "History", "Medium", "2025-07-15");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int rowsBefore = getTableRows().size();

        List<WebElement> deleteButtons = driver.findElements(
                By.xpath("//button[normalize-space()='Delete']"));
        deleteButtons.get(deleteButtons.size() - 1).click();

        // Dismiss (cancel) the confirmation dialog
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.dismiss();

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int rowsAfter = getTableRows().size();
        assertEquals(rowsBefore, rowsAfter,
                "Row count should NOT change when delete is cancelled.");
    }

    /**
     * TC-14 – Analytics: Total Assignments Counter
     * Add a known number of assignments and verify the "Total Assignments"
     * analytics counter reflects the correct total.
     */
    @Test
    @Order(14)
    @DisplayName("TC-14: Analytics 'Total Assignments' counter should match table row count")
    void testAnalyticsTotalCount() {
        goHome();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        int tableRows  = getTableRows().size();
        String totalText = driver.findElement(By.id("totalAssignments")).getText().trim();
        int analyticsTotal = Integer.parseInt(totalText);

        assertEquals(tableRows, analyticsTotal,
                "Analytics total should equal the number of rows in the table.");
    }

    /**
     * TC-15 – Analytics: Completed Count After Marking
     * Mark one assignment as complete and verify the "Completed" analytics
     * counter increments by 1.
     */
    @Test
    @Order(15)
    @DisplayName("TC-15: 'Completed' counter should increment when an assignment is completed")
    void testAnalyticsCompletedCount() {
        goHome();
        addAssignment("TC15 Analytics Test", "Biology", "High", "2025-12-01");
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int completedBefore = Integer.parseInt(
                driver.findElement(By.id("completedAssignments")).getText().trim());

        // Click the last Complete button
        List<WebElement> completeBtns = driver.findElements(
                By.xpath("//button[normalize-space()='Complete']"));
        completeBtns.get(completeBtns.size() - 1).click();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        int completedAfter = Integer.parseInt(
                driver.findElement(By.id("completedAssignments")).getText().trim());

        assertEquals(completedBefore + 1, completedAfter,
                "'Completed' analytics counter should increase by 1.");
    }

    /**
     * TC-16 – High Priority Assignment Displayed Correctly
     * Add a High-priority assignment and verify its Priority column shows "High".
     */
    @Test
    @Order(16)
    @DisplayName("TC-16: High-priority assignment should display 'High' in the Priority column")
    void testHighPriorityDisplayed() {
        goHome();
        addAssignment("TC16 High Priority", "Art", "High", "2025-11-30");

        List<WebElement> rows = getTableRows();
        WebElement lastRow = rows.get(rows.size() - 1);
        // Priority is the 3rd column (index 2)
        WebElement priorityCell = lastRow.findElement(
                By.cssSelector("td:nth-child(3)"));
        assertEquals("High", priorityCell.getText().trim(),
                "Priority column should show 'High'.");
    }

    /**
     * TC-17 – Search Box and Button Presence
     * Verify the Search input and Search button are rendered on the page.
     */
    @Test
    @Order(17)
    @DisplayName("TC-17: Search input and button should be present on the page")
    void testSearchElementsPresent() {
        goHome();
        assertTrue(driver.findElement(By.id("search")).isDisplayed(),
                "Search input should be visible.");
        assertTrue(driver.findElement(
                By.xpath("//button[normalize-space()='Search']")).isDisplayed(),
                "Search button should be visible.");
    }

    /**
     * TC-18 – Analytics Section Visible
     * Verify all five analytics card elements are present and visible.
     */
    @Test
    @Order(18)
    @DisplayName("TC-18: All analytics cards should be visible on the page")
    void testAnalyticsSectionVisible() {
        goHome();
        String[] analyticsIds = {
            "totalAssignments",
            "completedAssignments",
            "pendingAssignments",
            "overdueAssignments",
            "completionRate"
        };
        for (String id : analyticsIds) {
            assertTrue(driver.findElement(By.id(id)).isDisplayed(),
                    "Analytics element '" + id + "' should be visible.");
        }
    }

    /**
     * TC-19 – Completion Rate Format
     * The "Completion Rate" analytics card should display a value ending with "%".
     */
    @Test
    @Order(19)
    @DisplayName("TC-19: Completion Rate should display a percentage value (ends with '%')")
    void testCompletionRateFormat() {
        goHome();
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        String rate = driver.findElement(By.id("completionRate")).getText().trim();
        assertTrue(rate.endsWith("%"),
                "Completion rate should end with '%'. Actual: " + rate);
    }

    /**
     * TC-20 – Table Column Headers
     * Verify the assignments table has the correct six column headers:
     * Title, Subject, Priority, Status, Due, Action.
     */
    @Test
    @Order(20)
    @DisplayName("TC-20: Assignments table should have correct column headers")
    void testTableColumnHeaders() {
        goHome();
        List<WebElement> headers = driver.findElements(By.cssSelector("table thead th"));
        assertEquals(6, headers.size(), "Table should have 6 column headers.");

        String[] expected = {"Title", "Subject", "Priority", "Status", "Due", "Action"};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], headers.get(i).getText().trim(),
                    "Column header mismatch at index " + i);
        }
    }
}
