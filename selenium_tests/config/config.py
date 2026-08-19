import os

class Config:
    BASE_URL = os.environ.get("BASE_URL", "http://localhost:3000")
    API_BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:3000/api")
    TEST_EMAIL = os.environ.get("TEST_EMAIL", "ramesh@example.com")
    TEST_PASSWORD = os.environ.get("TEST_PASSWORD", "password123")
    TEST_ROLE = os.environ.get("TEST_ROLE", "farmer")
    HEADLESS = os.environ.get("HEADLESS", "true").lower() == "true"
    DEFAULT_IMPLICIT_WAIT = 10
    DEFAULT_EXPLICIT_WAIT = 15
