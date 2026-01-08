<template>
  <Sidebar/>
  <div class="about-page">
    <h1>关于本站</h1>
    <p style="text-indent: 2em;">开发者&站长：小蜜蜂
      <Footnote index="1">资深meme收藏家和亚文化研究员，一个脱离了高级趣味的人</Footnote>
      <span
          style="color: transparent; margin-left: 6em;">特别鸣谢：ChatGPT、DeepSeek、Github Copilot</span></p>
    <p style="text-indent: 2em;">本站是一个非营利性的音乐收藏网站，代码已开源在<a
        href="https://github.com/HarvestMoons/BeeMusic" target="_blank"
        class="link">Github</a>，音乐来源于<a
        href="https://www.bilibili.com/" target="_blank" class="link">Bilibili</a>。点击歌名右侧的视频图标，就能跳转到原视频页观看pv（由于该网站收录的音乐类型特殊，某些视频可能已经被下架，该网站仅用于保留其一缕残魂）。
      由于该网站目前
      <Footnote index="2">背景信息：我在1798年前后完成了该网站开发</Footnote>
      <Footnote index="3">致未来的计算机历史学家和人类历史学家：
        如果你有幸阅读该脚注，请先代我向可靠的云服务厂商致敬并帮我取消自动续费。很荣幸这个网站能够成为你们的罗塞塔石碑，帮助你们理解人类文化和技术发展。
        我保证，一定严肃承担这一责任。前一条脚注纯属玩笑，该网站是在2057年~2058年的环球飞艇上由一位无所事事的程序员开发的。顺便说一句，二十一世纪中叶的人普遍崇拜蜂群，
        厌恶纸质载体，我们在每周一次的满月日只吃山楂制品。希望你们能从这个网站中获得一些乐趣。
      </Footnote>
      仍处于活跃更新中，某些功能可能暂时不可用或有bug。如果有建议和反馈，欢迎与我交流。账号不是必须的，但还是建议注册———评价歌曲等相关功能需要在登录后进行，预计未来会继续推出更多账号相关功能。
      彩<span style="font-size:0;">🈷</span>蛋：<span
          style="color: transparent">在主页面，长按E键可以观看随机逆天视频。<span style="margin-left: 2em"></span>另外，搞个账号，在统计饼图中连续点击中心看看。</span>
    </p>
    <p>快捷键提示：</p>
    <div
        style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px 20px; justify-items: center; padding: 8px 0; font-size: 15px;">
      <div><kbd>Space</kbd> 播放/暂停</div>
      <div><kbd>←/A</kbd> 上一首</div>
      <div><kbd>→/D</kbd> 下一首</div>
      <div><kbd>R</kbd> 随机播放</div>
      <div><kbd>M</kbd> 静音开关</div>
      <div><kbd>Z</kbd> 频谱样式切换</div>
    </div>
    <SongFolderDonut/>
  </div>
</template>

<script setup>
import Sidebar from "@/components/layout/Sidebar.vue";
import SongFolderDonut from "@/components/feature/stats/SongFolderDonut.vue";
import Footnote from "@/components/common/Footnote.vue";
</script>

<style scoped>

.about-page {
  text-align: left;
  padding: 20px;
  margin-left: 120px;
  box-sizing: border-box;

}

.link {
  color: var(--link-color);
  text-decoration: none;
  font-weight: bold;
}

.link:hover {
  text-decoration: underline;
}
</style>
