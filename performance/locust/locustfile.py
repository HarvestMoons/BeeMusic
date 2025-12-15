from locust import HttpUser, task, constant

class WebsiteUser(HttpUser):
    wait_time = constant(0.01)
    host = "http://8.155.47.138:8081"
    @task
    def play(self):
        self.client.post("/api/public/songs/play/1")

from locust import LoadTestShape

class ConstantRPS(LoadTestShape):
    target_rps = 50   # 期望的 RPS
    duration = 30      # 压测时间（秒）

    def tick(self):
        run_time = self.get_run_time()

        if run_time > self.duration:
            return None

        # 核心：为达到目标 RPS 动态调整用户数
        user_count = self.target_rps
        spawn_rate = self.target_rps

        return user_count, spawn_rate

# locust -f locustfile.py --headless
# gui
# locust -f locustfile.py

