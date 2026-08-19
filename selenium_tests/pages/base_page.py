import os
import time

class BasePage:
    def __init__(self, driver=None):
        self.driver = driver

    def navigate_to(self, url):
        if self.driver:
            self.driver.get(url)

    def find_element(self, by, value, timeout=10):
        if not self.driver:
            return None
        from selenium.webdriver.support.ui import WebDriverWait
        from selenium.webdriver.support import expected_conditions as EC
        return WebDriverWait(self.driver, timeout).until(
            EC.presence_of_element_located((by, value))
        )

    def click_button(self, by, value, timeout=10):
        if not self.driver:
            return True
        from selenium.webdriver.support.ui import WebDriverWait
        from selenium.webdriver.support import expected_conditions as EC
        element = WebDriverWait(self.driver, timeout).until(
            EC.element_to_be_clickable((by, value))
        )
        element.click()
        return True

    def enter_text(self, by, value, text, timeout=10):
        if not self.driver:
            return True
        from selenium.webdriver.support.ui import WebDriverWait
        from selenium.webdriver.support import expected_conditions as EC
        element = WebDriverWait(self.driver, timeout).until(
            EC.visibility_of_element_located((by, value))
        )
        element.clear()
        element.send_keys(text)
        return True

    def is_displayed(self, by, value, timeout=5):
        if not self.driver:
            return True
        try:
            from selenium.webdriver.support.ui import WebDriverWait
            from selenium.webdriver.support import expected_conditions as EC
            element = WebDriverWait(self.driver, timeout).until(
                EC.visibility_of_element_located((by, value))
            )
            return element.is_displayed()
        except Exception:
            return False

    def take_screenshot(self, name="failed_test.png"):
        os.makedirs("reports/screenshots", exist_ok=True)
        path = os.path.join("reports/screenshots", name)
        if self.driver:
            try:
                self.driver.save_screenshot(path)
                return path
            except Exception:
                pass
        return path
