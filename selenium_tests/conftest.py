import pytest
import os
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium_tests.config.config import Config

@pytest.fixture(scope="session")
def driver():
    options = Options()
    if Config.HEADLESS:
        options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    
    try:
        drv = webdriver.Chrome(options=options)
    except Exception:
        # Fallback if Chrome binary is not present in local path
        drv = None
        
    if drv:
        drv.implicitly_wait(Config.DEFAULT_IMPLICIT_WAIT)
        drv.get(Config.BASE_URL)
        yield drv
        drv.quit()
    else:
        yield None

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    rep = outcome.get_result()
    if rep.when == "call" and rep.failed:
        drv = item.funcargs.get("driver", None)
        if drv:
            os.makedirs("reports/screenshots", exist_ok=True)
            screenshot_path = os.path.join("reports/screenshots", f"{item.name}_failed.png")
            try:
                drv.save_screenshot(screenshot_path)
            except Exception:
                pass
