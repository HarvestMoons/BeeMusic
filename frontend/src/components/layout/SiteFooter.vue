<template>
  <footer class="site-footer">
    <div class="copyright">© 2026 小蜜蜂. All rights reserved.</div>
    <a href="https://beian.mps.gov.cn/#/query/webSearch?code=35012202350348" rel="noreferrer" target="_blank" class="icp-link">
      <img :src="icpIcon" alt="" class="icp-icon"/>
      <span>闽公网安备35012202350348号</span>
    </a>
  </footer>
</template>

<script setup>
import icpIcon from '@/assets/icons/icp/备案图标.png'
</script>

<style scoped>
.site-footer {
  text-align: center;
  padding: 15px 0;
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.6;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: 8px;
}

.icp-link {
  display: flex;
  align-items: center;
  gap: 5px;
  text-decoration: none;
  color: inherit;
  transition: opacity 0.2s;
}

.icp-icon {
  width: 20px;
  height: 20px;
}

.icp-link:hover {
  opacity: 1;
  text-decoration: underline;
}
</style>
