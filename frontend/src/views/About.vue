<template>
  <Sidebar />
  <div class="about-page">
    <h1>关于本站</h1>
    <p>开发者：小蜜蜂 <span style="color: transparent">     特别鸣谢：ChatGPT、DeepSeek、Github Copilot</span></p>
    <p>本站是一个个人音乐网站，基于 Vue3 + Spring 开发，音乐来源于Bilibili网站。点击歌名右侧的视频图标，就能跳转到原视频页观看pv（由于该网站收录的音乐类型特殊，某些视频可能已经被下架，该网站仅用于保留其一缕残魂）。
      由于该网站目前仍处于活跃更新中，某些功能可能暂时不可用或有bug。如果有任何想法，可以在“隐私政策”页面下找到我的联系方式与我交流。
      彩蛋：<span style="color: transparent">在主页面，长按E键可以观看随机逆天视频。</span></p>
    <p>账号不是必须的，但还是建议注册———评价歌曲等相关功能需要在登录前提下进行，未来会继续推出更多账号相关功能。</p>
    <p>快捷键提示：</p>
    <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 20px; justify-items: center; padding: 8px 0; font-size: 15px;">
      <div><kbd>Space</kbd> 播放/暂停</div>
      <div><kbd>← A</kbd> 上一首</div>
      <div><kbd>→ D</kbd> 下一首</div>
      <div><kbd>R</kbd> 随机播放</div>
      <div><kbd>M</kbd> 静音开关</div>
      <div><kbd>Z</kbd> 频谱样式切换</div>
    </div>

    <section class="programmer-honor-shame" aria-labelledby="ps-title">
      <h3 id="ps-title">程序员八荣八耻</h3>
      <ul>
        <li>以瞎猜接口为耻，以认真查阅为荣。</li>
        <li>以模糊执行为耻，以寻求确认为荣。</li>
        <li>以盲想业务为耻，以人类确认为荣。</li>
        <li>以创造接口为耻，以复用现有为荣。</li>
        <li>以跳过验证为耻，以主动测试为荣。</li>
        <li>以破坏架构为耻，以遵循规范为荣。</li>
        <li>以假装理解为耻，以诚实无知为荣。</li>
        <li>以盲目修改为耻，以谨慎重构为荣。</li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import Sidebar from "../components/layout/Sidebar.vue";
</script>

<style scoped>

.about-page {
  text-align: left;
  padding: 20px;
  margin-left: 120px; /* 给侧边栏留出空间，偏右 */
  box-sizing: border-box;

}
</style>
