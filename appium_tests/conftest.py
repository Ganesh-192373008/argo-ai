import pytest
from selenium_tests.config.config import Config

@pytest.fixture(scope="session")
def mobile_driver():
    # Appium mobile web / browser automation fixture stub
    caps = {
        'platformName': 'Android',
        'browserName': 'Chrome',
        'deviceName': 'Mobile_Emulator'
    }
    # In local browser testing without active Appium server, fallback safely
    yield {"capabilities": caps, "url": Config.BASE_URL, "mode": "mobile_responsive"}
