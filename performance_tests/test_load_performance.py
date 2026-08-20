import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import time
import requests
import concurrent.futures
import statistics
import datetime
from selenium_tests.config.config import Config

def send_single_api_request(endpoint):
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

def run_load_benchmark(concurrent_users=100, duration_seconds=60):
    print("=" * 80)
    print(" AGROASSIST AI - BASELINE LOAD & PERFORMANCE TEST")
    print(f" • Concurrent Virtual Users: {concurrent_users}")
    print(f" • Test Duration:            {duration_seconds} seconds (1 minute)")
    print(f" • Target Base URL:          {Config.BASE_URL}")
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

    results = []
    stop_time = time.time() + duration_seconds
    start_bench = time.time()

    def worker(worker_id):
        worker_results = []
        counter = 0
        while time.time() < stop_time:
            ep = endpoints[counter % len(endpoints)]
            res = send_single_api_request(ep)
            res["worker_id"] = worker_id
            worker_results.append(res)
            counter += 1
        return worker_results

    print(f"[Load Suite] Launching {concurrent_users} virtual users for {duration_seconds} seconds continuous load...")

    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrent_users) as executor:
        futures = [executor.submit(worker, u_idx) for u_idx in range(concurrent_users)]
        for future in concurrent.futures.as_completed(futures):
            results.extend(future.result())

    total_bench_duration = time.time() - start_bench

    # Benchmark Metrics Calculation
    successful = [r for r in results if r["success"]]
    failed = [r for r in results if not r["success"]]
    latencies = [r["latency_ms"] for r in results]

    min_latency = min(latencies) if latencies else 0.0
    max_latency = max(latencies) if latencies else 0.0
    avg_latency = statistics.mean(latencies) if latencies else 0.0
    median_latency = statistics.median(latencies) if latencies else 0.0
    p95_latency = statistics.quantiles(latencies, n=20)[18] if len(latencies) >= 20 else max_latency
    rps = len(results) / total_bench_duration if total_bench_duration > 0 else 0.0
    pass_pct = (len(successful) / len(results) * 100) if results else 0.0

    summary_output = f"""
================================================================================
BASELINE / LOAD TESTING SUMMARY (100 CONCURRENT USERS - 1 MINUTE)
================================================================================
Target Application URL:      {Config.BASE_URL}
Concurrent Virtual Users:   {concurrent_users} users
Test Execution Duration:    {total_bench_duration:.2f} seconds
Total Requests Handled:     {len(results)} requests

Throughput (RPS):
  - Requests Per Second:    {rps:.2f} req/sec

Response Time Metrics:
  - Minimum Response Time:  {min_latency:.2f} ms
  - Average Response Time:  {avg_latency:.2f} ms
  - Median Response Time:   {median_latency:.2f} ms
  - 95th Percentile (P95):  {p95_latency:.2f} ms
  - Maximum Response Time:  {max_latency:.2f} ms

Results Breakdown:
  • Successful Requests (200 OK): {len(successful)} ({pass_pct:.2f}%)
  • Failed / Timed Out:           {len(failed)} ({100 - pass_pct:.2f}%)
================================================================================
"""
    print(summary_output)

    # Save Excel report for Load Test results
    from selenium_tests.utils.excel_reporter import ExcelReporter
    load_test_cases = []
    for idx, r in enumerate(results, 1):
        load_test_cases.append({
            "id": f"TC-LOAD-{idx:04d}",
            "module": "Baseline Load Testing",
            "feature": f"Concurrent User Request #{r['worker_id']}",
            "page": "API Endpoint",
            "type": "Performance Load",
            "description": f"Worker {r['worker_id']} - GET {r['endpoint']} under 100 virtual users load",
            "preconditions": "100 Virtual Users Active",
            "steps": f"HTTP GET {r['endpoint']}",
            "test_data": f"100 Concurrent Threads | 60s Duration",
            "expected": "Response HTTP 200 within SLA (<5000ms)",
            "actual": f"Status={r['status']}, Latency={r['latency_ms']:.2f}ms",
            "status": "PASS" if r["success"] else "FAIL",
            "execution_time": r["latency_ms"] / 1000.0,
            "browser": "HTTP Concurrent Worker",
            "device": "API Endpoint",
            "screenshot": "",
            "error": "" if r["success"] else "Request timeout or non-200 status",
            "start_time": "09:40:00",
            "end_time": "09:41:00"
        })

    reporter = ExcelReporter("reports/test_results_load.xlsx")
    reporter.generate_report(load_test_cases)
    print(f"[Load Suite] Successfully exported {len(load_test_cases)} requests to reports/test_results_load.xlsx.")

    return summary_output

if __name__ == "__main__":
    run_load_benchmark(concurrent_users=100, duration_seconds=60)
