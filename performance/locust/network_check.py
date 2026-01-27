import requests
import time

url = "https://beemusic.fun/api/public/songs/get"

print(f"Connecting to {url} ...")

try:
    # 1. Start timer
    start_time = time.time()
    
    # 2. Send request (stream=True allows us to measure TTFB)
    # Note: verify=False is used to ignore potential SSL cert issues for testing, 
    # though your site seems to have valid SSL.
    with requests.get(url, stream=True) as response:
        # 3. Time until headers are received (Time To First Byte)
        ttfb = time.time() - start_time
        
        # 4. Download content
        content = b""
        for chunk in response.iter_content(chunk_size=8192):
            if chunk:
                content += chunk
        
        # 5. End timer
        end_time = time.time()
        
    total_time = end_time - start_time
    download_time = total_time - ttfb
    size_in_bytes = len(content)
    size_in_kb = size_in_bytes / 1024
    size_in_mb = size_in_kb / 1024
    
    # Calculate approximate speed
    speed_kbps = size_in_kb / download_time if download_time > 0 else 0

    print("-" * 40)
    print("DIAGNOSTIC RESULTS")
    print("-" * 40)
    print(f"Status Code    : {response.status_code}")
    print(f"Content Size   : {size_in_kb:.2f} KB ({size_in_mb:.2f} MB)")
    print(f"Total Time     : {total_time:.4f} s")
    print("-" * 40)
    print(f"[Phase 1] Server Processing + Network Latency (TTFB): {ttfb:.4f} s")
    print(f"[Phase 2] Data Transfer (Download Time)             : {download_time:.4f} s")
    print("-" * 40)
    print(f"Avg Download Speed: {speed_kbps:.2f} KB/s")

except Exception as e:
    print(f"Error: {e}")
