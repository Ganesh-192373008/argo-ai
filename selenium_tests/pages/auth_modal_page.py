from selenium.webdriver.common.by import By
from selenium_tests.pages.base_page import BasePage

class AuthModalPage(BasePage):
    TAB_LOGIN_BTN = (By.ID, "tab-btn-login")
    TAB_REGISTER_BTN = (By.ID, "tab-btn-register")
    LOGIN_EMAIL_INPUT = (By.ID, "login-email")
    LOGIN_PASSWORD_INPUT = (By.ID, "login-password")
    LOGIN_SUBMIT_BTN = (By.ID, "btn-login-submit")
    
    REG_NAME_INPUT = (By.ID, "reg-name")
    REG_EMAIL_INPUT = (By.ID, "reg-email")
    REG_PHONE_INPUT = (By.ID, "reg-phone")
    REG_PASSWORD_INPUT = (By.ID, "reg-password")
    REG_CONFIRM_PASSWORD_INPUT = (By.ID, "reg-confirm-password")
    REG_SUBMIT_BTN = (By.ID, "btn-register-submit")

    def switch_to_login_tab(self):
        return self.click_button(*self.TAB_LOGIN_BTN)

    def switch_to_register_tab(self):
        return self.click_button(*self.TAB_REGISTER_BTN)

    def login(self, email, password):
        self.switch_to_login_tab()
        self.enter_text(*self.LOGIN_EMAIL_INPUT, email)
        self.enter_text(*self.LOGIN_PASSWORD_INPUT, password)
        return self.click_button(*self.LOGIN_SUBMIT_BTN)

    def register(self, name, email, phone, password, confirm_password):
        self.switch_to_register_tab()
        self.enter_text(*self.REG_NAME_INPUT, name)
        self.enter_text(*self.REG_EMAIL_INPUT, email)
        self.enter_text(*self.REG_PHONE_INPUT, phone)
        self.enter_text(*self.REG_PASSWORD_INPUT, password)
        self.enter_text(*self.REG_CONFIRM_PASSWORD_INPUT, confirm_password)
        return self.click_button(*self.REG_SUBMIT_BTN)
