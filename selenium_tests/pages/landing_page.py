from selenium.webdriver.common.by import By
from selenium_tests.pages.base_page import BasePage

class LandingPage(BasePage):
    NAV_LOGO = (By.ID, "nav-logo-link")
    NAV_LOGIN_BTN = (By.ID, "nav-login-btn")
    NAV_SIGNUP_BTN = (By.ID, "nav-signup-btn")
    HERO_GET_STARTED_BTN = (By.ID, "hero-get-started-btn")
    HERO_WATCH_BTN = (By.ID, "hero-watch-btn")
    CTA_GET_STARTED_BTN = (By.ID, "cta-get-started-btn")
    CONTACT_FORM_SUBMIT_BTN = (By.ID, "form-submit-btn")
    SCROLL_TOP_BTN = (By.ID, "scroll-top-btn")

    def open_landing_page(self, base_url):
        self.navigate_to(base_url)

    def click_login(self):
        return self.click_button(*self.NAV_LOGIN_BTN)

    def click_signup(self):
        return self.click_button(*self.NAV_SIGNUP_BTN)

    def click_hero_get_started(self):
        return self.click_button(*self.HERO_GET_STARTED_BTN)

    def fill_contact_form(self, name, phone, email, message):
        self.enter_text(By.ID, "name", name)
        self.enter_text(By.ID, "phone", phone)
        if email:
            self.enter_text(By.ID, "email", email)
        self.enter_text(By.ID, "message", message)
        return self.click_button(*self.CONTACT_FORM_SUBMIT_BTN)
