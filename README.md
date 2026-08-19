# AgroAssist AI - Complete E2E QA & Security Automation Framework

A comprehensive automated testing framework built with **Python**, **Pytest**, **Selenium Webdriver**, **Appium**, **Requests**, and **OpenPyXL** for end-to-end testing of the **AgroAssist AI** smart farming application.

## 📁 Repository Structure

```text
project-root/
│
├── selenium_tests/          # UI Automation (Page Object Model)
│   ├── config/              # Base URLs, timeouts & environment settings
│   ├── pages/               # Page Object Model classes
│   ├── tests/               # Test suites (Smoke, Functional, Auth, Navigation, Multi-Tab, Workflows)
│   ├── utils/               # Excel reporter & Browser factory helpers
│   ├── conftest.py          # Pytest fixtures & screenshot hooks
│   └── pytest.ini           # Pytest configuration
│
├── appium_tests/            # Mobile & Responsive web testing
│   ├── tests/               # Responsive layout & touch tests
│   ├── pages/               # Mobile base page objects
│   └── conftest.py          # Appium configuration
│
├── security_tests/          # Non-destructive API & Security checks
│   ├── api_security/        # Endpoint security & headers
│   ├── authentication/      # Password masking & session invalidation
│   ├── authorization/       # Privilege escalation & access control checks
│   └── input_validation/    # Boundary & special character checks
│
├── reports/                 # Test Execution Artifacts
│   ├── test_results.xlsx    # Final Excel workbook (7 sheets, 325 test cases)
│   ├── html/                # Pytest HTML interactive report
│   └── screenshots/         # Failed & evidence screenshots
│
├── .github/
│   └── workflows/
│       └── test.yml         # GitHub Actions CI/CD Pipeline
│
├── run_all_tests.py         # Autonomous Master Test Suite Runner
├── .env.example             # Environment template
├── requirements.txt         # Python package dependencies
└── README.md                # Framework documentation
```

## 📊 Test Case Distribution (325 Distinct Tests)

| Category | Test Case ID Range | Description |
| :--- | :--- | :--- |
| **Smoke Testing** | TC-001 – TC-025 | App launch, home page, main nav, login, dashboard, primary links |
| **Functional & Button Testing** | TC-026 – TC-145 | Every button's visibility, enabled state, click action, navigation, failure handling |
| **Form & Input Validation** | TC-146 – TC-190 | Valid/invalid inputs, empty fields, boundary values, calculators, forms |
| **Authentication & Session** | TC-191 – TC-220 | Login, invalid credentials, password masking, logout, session security |
| **Navigation & Routing** | TC-221 – TC-245 | Header/sidebar links, back/forward navigation, direct URL routes |
| **Multiple Tab / Window** | TC-246 – TC-255 | Tab switching, Google Maps route view, external portal tab checks |
| **Responsive & Appium** | TC-256 – TC-275 | Mobile viewports, hamburger menus, touch interactions, mobile layout |
| **End-to-End User Journeys** | TC-276 – TC-295 | Complete workflows (Launch -> Register -> Scan Leaf -> Cart -> Delivery -> Order History) |
| **Backend API & Security** | TC-296 – TC-325 | API status codes, response schemas, unauthenticated access rejection, headers |

## 🚀 Execution Instructions

### 1. Install Dependencies
```bash
pip install -r requirements.txt
```

### 2. Run Complete Automated Test Suite
```bash
python run_all_tests.py
```

### 3. Generated Reports
- **Excel Report**: `reports/test_results.xlsx` (containing all 7 sheets: *Test Cases*, *Summary*, *Module Analysis*, *Button Coverage*, *Security Findings*, *Failed Tests*, *Execution Timeline*)
- **HTML Report**: `reports/html/report.html`
- **Screenshots**: `reports/screenshots/`
