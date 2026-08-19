from selenium.webdriver.common.by import By
from selenium_tests.pages.base_page import BasePage

class DashboardPage(BasePage):
    HEADER_TITLE = (By.ID, "app-title-display")
    STORES_BADGE_BTN = (By.ID, "nearby-stores-badge-btn")
    MANDIS_BADGE_BTN = (By.ID, "nearby-mandis-badge-btn")
    CART_BADGE_COUNT = (By.ID, "cart-count")
    NOTIF_BADGE_COUNT = (By.ID, "notif-count")
    
    # Sidebar items
    SIDEBAR_DASHBOARD = (By.XPATH, "//button[contains(text(), 'Dashboard')]")
    SIDEBAR_DISEASE = (By.XPATH, "//button[contains(text(), 'Disease Detection')]")
    SIDEBAR_HISTORY = (By.XPATH, "//button[contains(text(), 'Scan History')]")
    SIDEBAR_CHATBOT = (By.XPATH, "//button[contains(text(), 'AI Assistant')]")
    SIDEBAR_WEATHER = (By.XPATH, "//button[contains(text(), 'Weather forecast')]")
    SIDEBAR_MARKET = (By.XPATH, "//button[contains(text(), 'Market Rates')]")
    SIDEBAR_MANDIS = (By.XPATH, "//button[contains(text(), 'Nearest Mandis')]")
    SIDEBAR_SCHEMES = (By.XPATH, "//button[contains(text(), 'Govt Schemes')]")
    SIDEBAR_WATER = (By.XPATH, "//button[contains(text(), 'Water Management')]")
    SIDEBAR_FERTILIZER = (By.XPATH, "//button[contains(text(), 'Fertilizers')]")
    SIDEBAR_REPORTS = (By.XPATH, "//button[contains(text(), 'Analytics')]")
    SIDEBAR_PROFILE = (By.XPATH, "//button[contains(text(), 'My Profile')]")
    SIDEBAR_SETTINGS = (By.XPATH, "//button[contains(text(), 'Settings')]")
    SIDEBAR_LOGOUT = (By.XPATH, "//button[contains(text(), 'Log Out')]")

    def navigate_to_view(self, view_name):
        view_map = {
            "dashboard": self.SIDEBAR_DASHBOARD,
            "disease-detection": self.SIDEBAR_DISEASE,
            "history": self.SIDEBAR_HISTORY,
            "chatbot": self.SIDEBAR_CHATBOT,
            "weather": self.SIDEBAR_WEATHER,
            "market": self.SIDEBAR_MARKET,
            "mandis": self.SIDEBAR_MANDIS,
            "schemes": self.SIDEBAR_SCHEMES,
            "water": self.SIDEBAR_WATER,
            "fertilizer": self.SIDEBAR_FERTILIZER,
            "reports": self.SIDEBAR_REPORTS,
            "profile": self.SIDEBAR_PROFILE,
            "settings": self.SIDEBAR_SETTINGS
        }
        if view_name in view_map:
            return self.click_button(*view_map[view_name])
        return False

    def logout(self):
        return self.click_button(*self.SIDEBAR_LOGOUT)
