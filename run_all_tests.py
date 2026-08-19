import os
import sys
import time
import requests
import datetime
from run_web_tests import run_web_suite
from run_appium_tests import run_appium_suite
from selenium_tests.utils.excel_reporter import ExcelReporter
from selenium_tests.config.config import Config

def run_master_suite():
    print("=" * 80)
    print(" AGROASSIST AI - MASTER AUTOMATED TEST SUITE (WEB & MOBILE APPIUM)")
    print("=" * 80)

    # 1. Execute Dedicated Web Testing Suite
    run_web_suite()

    # 2. Execute Dedicated Mobile Appium Testing Suite
    run_appium_suite()

    print("\n" + "=" * 80)
    print(" ALL TEST SUITES EXECUTED SUCCESSFULLY")
    print(" • Web Test Results: reports/test_results_web.xlsx")
    print(" • Appium Test Results: reports/test_results_appium.xlsx")
    print(" • Master Combined Report: reports/test_results.xlsx")
    print("=" * 80)

if __name__ == "__main__":
    run_master_suite()
