import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import time
import requests
import concurrent.futures
import statistics
from selenium_tests.config.config import Config

def send_api_request(endpoint):
    url = f"{Config.BASE_URL}{endpoint}"
    t_start = time.time()
    try:
        r = requests.get(url, timeout=5)
        duration_ms = (time.time() - t_start) * 1000
        return {
            "endpoint": endpoint,
            "status": r.status_code,
            "latency_ms": duration_ms,
            "success": r.status_code == 200
        }
    except Exception as e:
        duration_ms = (time.time() - t_start) * 1000
        return {
            "endpoint": endpoint,
            "status": 0,
            "latency_ms": duration_ms,
            "success": False
        }

def run_load_benchmark(concurrent_users=20, total_requests=100):
    print("=" * 80)
    print(" AGROASSIST AI - NON-DESTRUCTIVE API LOAD & PERFORMANCE BENCHMARK")
    print(f" Concurrent Virtual Users: {concurrent_users} | Total Requests: {total_requests}")
    print(" Target:", Config.BASE_URL)
    print("=" * 80)

    # Ensure backend server is running
    server_process = None
    try:
        r = requests.get(f"{Config.API_BASE_URL}/health", timeout=2)
        if r.status_code == 200:
            print("[Load Suite] Backend API server is online.")
    except Exception:
        print("[Load Suite] Starting local backend server (node backend/src/server.js)...")
        import subprocess
        backend_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "backend")
        server_js = os.path.join(backend_dir, "src", "server.js")
        if os.path.exists(server_js):
            server_process = subprocess.Popen(["node", server_js], cwd=backend_dir)
            for attempt in range(15):
                time.sleep(1)
                try:
                    r = requests.get(f"{Config.API_BASE_URL}/health", timeout=2)
                    if r.status_code == 200:
                        print(f"[Load Suite] Server ready after {attempt+1}s.")
                        break
                except Exception:
                    pass

    endpoints = [
        "/api/health",
        "/api/market-prices",
        "/api/gov-schemes",
        "/api/history"
    ]

    request_queue = []
    for i in range(total_requests):
        ep = endpoints[i % len(endpoints)]
        request_queue.append(ep)

    start_bench = time.time()
    results = []

    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrent_users) as executor:
        futures = [executor.submit(send_api_request, ep) for ep in request_queue]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    total_bench_duration = time.time() - start_bench

    # Benchmark Calculations
    successful = [r for r in results if r["success"]]
    failed = [r for r in results if not r["success"]]
    latencies = [r["latency_ms"] for r in results]

    avg_latency = statistics.mean(latencies) if latencies else 0
    median_latency = statistics.median(latencies) if latencies else 0
    p95_latency = statistics.quantiles(latencies, n=20)[18] if len(latencies) >= 20 else max(latencies or [0])
    rps = len(results) / total_bench_duration if total_bench_duration > 0 else 0
    pass_pct = (len(successful) / len(results) * 100) if results else 0

    report_text = f"""
================================================================================
LOAD & PERFORMANCE TEST SUMMARY
================================================================================
Target URL:                  {Config.BASE_URL}
Total Requests Executed:    {len(results)}
Concurrent Virtual Users:   {concurrent_users}
Total Test Duration:        {total_bench_duration:.2f} seconds
Requests Per Second (RPS):  {rps:.2f} req/sec

Latency Performance:
  • Average Response Time:  {avg_latency:.2f} ms
  • Median Response Time:   {median_latency:.2f} ms
  • 95th Percentile (P95): {p95_latency:.2f} ms
  • Min Response Time:      {min(latencies or [0]):.2f} ms
  • Max Response Time:      {max(latencies or [0]):.2f} ms

Results Breakdown:
  • Successful (HTTP 200):  {len(successful)} ({pass_pct:.1f}%)
  • Failed / Timed Out:     {len(failed)} ({100 - pass_pct:.1f}%)
================================================================================
"""
    # Save Excel report for Load Test results
    from selenium_tests.utils.excel_reporter import ExcelReporter
    load_test_cases = []
    for idx, r in enumerate(results, 1):
        load_test_cases.append({
            "id": f"TC-LOAD-{idx:03d}",
            "module": "API Load & Performance",
            "feature": f"Concurrent Request to {r['endpoint']}",
            "page": "API Endpoint",
            "type": "Performance Load",
            "description": f"Send request {idx} to {r['endpoint']} under {concurrent_users} virtual users load",
            "preconditions": "API Server active",
            "steps": f"HTTP GET {r['endpoint']}",
            "test_data": f"Concurrent Threads={concurrent_users}",
            "expected": "Response HTTP 200 within SLA (<5000ms)",
            "actual": f"Status={r['status']}, Latency={r['latency_ms']:.2f}ms",
            "status": "PASS" if r["success"] else "FAIL",
            "execution_time": r["latency_ms"] / 1000.0,
            "browser": "HTTP Concurrent ThreadPool",
            "device": "API Endpoint",
            "screenshot": "",
            "error": "" if r["success"] else "Request timeout or non-200 status",
            "start_time": "09:30:00",
            "end_time": "09:30:00"
        })

    reporter = ExcelReporter("reports/test_results_load.xlsx")
    reporter.generate_report(load_test_cases)
    print("[Load Suite] Report successfully saved to reports/test_results_load.xlsx.")

    print(report_text)
    return report_text

if __name__ == "__main__":
    run_load_benchmark(concurrent_users=20, total_requests=100)
