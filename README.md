# Assignment Tracker – Selenium Test Suite

## Tech Stack
- **Language:** Java 11
- **Test Framework:** JUnit 5
- **Browser Automation:** Selenium 4
- **Driver Management:** WebDriverManager (auto-downloads ChromeDriver)
- **Build Tool:** Maven
- **Browser:** Headless Chrome (required for Jenkins / AWS EC2)

---

## Project Structure

```
selenium-tests/
├── pom.xml
└── src/
    └── test/
        └── java/
            └── com/
                └── assignmenttracker/
                    └── AssignmentTrackerTest.java   ← 20 test cases
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 11 or higher |
| Maven | 3.6+ |
| Google Chrome | 112+ (installed on machine) |
| Node.js + MongoDB | Running (for the app itself) |

---

## Step 1 – Start Your Web Application

```bash
cd "Assignment_tracker - Copy"
npm install
node server.js
```

App should be running at: **http://localhost:3000**

---

## Step 2 – Run the Selenium Tests

```bash
cd selenium-tests
mvn test
```

WebDriverManager will **automatically** download the correct ChromeDriver binary — no manual setup needed.

---

## Test Cases Summary (20 Total)

| # | Test Name | What It Tests |
|---|---|---|
| TC-01 | Page Title | Browser tab shows "Assignment Dashboard" |
| TC-02 | Main Heading | H1 shows "Smart Assignment Manager" |
| TC-03 | Form Elements Present | All inputs + Add button visible |
| TC-04 | Add Assignment (Happy Path) | Row count increases after submission |
| TC-05 | Added Data Rendered Correctly | Title & subject appear in table |
| TC-06 | Default Priority Option | Dropdown defaults to "Low" |
| TC-07 | Priority Dropdown Options | Low / Medium / High all exist |
| TC-08 | Empty Form Validation | Alert shown when fields are empty |
| TC-09 | Search – Match Found | Matching rows stay visible |
| TC-10 | Search – No Match | All rows hidden for unknown search |
| TC-11 | Mark as Completed | Status cell changes to "Completed" |
| TC-12 | Delete – Confirm | Row removed after confirming dialog |
| TC-13 | Delete – Cancel | Row kept after dismissing dialog |
| TC-14 | Analytics Total Count | Counter matches table row count |
| TC-15 | Analytics Completed Count | Counter increments after completion |
| TC-16 | High Priority Displayed | Priority column shows "High" |
| TC-17 | Search Elements Present | Search box & button are visible |
| TC-18 | Analytics Section Visible | All 5 analytics cards are visible |
| TC-19 | Completion Rate Format | Shows value ending with "%" |
| TC-20 | Table Column Headers | 6 correct headers in correct order |

---

## Jenkins Pipeline (Part II)

In your Jenkinsfile, add this stage:

```groovy
stage('Run Selenium Tests') {
    steps {
        sh 'mvn test -f selenium-tests/pom.xml'
    }
    post {
        always {
            junit 'selenium-tests/target/surefire-reports/*.xml'
        }
    }
}
```

---

## Notes on Headless Chrome

The test class already configures headless Chrome via `ChromeOptions`:

```java
options.addArguments("--headless=new");
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");
options.addArguments("--disable-gpu");
options.addArguments("--window-size=1920,1080");
```

These flags are **mandatory** when running on AWS EC2 or inside a Docker container.
