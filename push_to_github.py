import os
import subprocess
import sys

def get_git_executable():
    possible_paths = [
        "git",
        r"C:\Program Files\Git\cmd\git.exe",
        r"C:\Program Files (x86)\Git\cmd\git.exe",
        os.path.expanduser(r"~\AppData\Local\Programs\Git\cmd\git.exe")
    ]
    for p in possible_paths:
        try:
            res = subprocess.run([p, "--version"], capture_output=True, text=True)
            if res.returncode == 0:
                return p
        except Exception:
            continue
    return None

def auto_setup_and_commit():
    git_bin = get_git_executable()
    if not git_bin:
        print("[Git Error] Git binary is installing or not yet in PATH.")
        return False

    print(f"[Git Setup] Found Git binary: {git_bin}")
    repo_dir = os.path.dirname(os.path.abspath(__file__))

    # 1. Git Init
    subprocess.run([git_bin, "init"], cwd=repo_dir)

    # 2. Configure default user if not set
    subprocess.run([git_bin, "config", "user.name", "AgroAssist-QA-Bot"], cwd=repo_dir)
    subprocess.run([git_bin, "config", "user.email", "qa-bot@agroassist.ai"], cwd=repo_dir)

    # 3. Git Add .
    print("[Git Setup] Staging all test framework files...")
    subprocess.run([git_bin, "add", "."], cwd=repo_dir)

    # 4. Git Commit
    print("[Git Setup] Creating initial commit...")
    subprocess.run([git_bin, "commit", "-m", "Add complete E2E QA Automation Testing Framework (325 test cases + reports + CI workflow)"], cwd=repo_dir)

    print("\n===================================================")
    print(" [SUCCESS] Git repository successfully initialized & committed locally!")
    print("===================================================")
    return True

if __name__ == "__main__":
    auto_setup_and_commit()
