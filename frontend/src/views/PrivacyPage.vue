<template>
  <Sidebar/>
  <div class="container">
    <div v-if="loading">
      <MySpinner/>
    </div>
    <div v-else v-html="renderedMarkdown"/>
  </div>
</template>

<script>
import {onMounted, ref} from 'vue'
import MarkdownIt from 'markdown-it'
import MySpinner from "@/components/effects/MySpinner.vue";
import Sidebar from "@/components/layout/Sidebar.vue";

export default {
  components: {Sidebar, MySpinner},
  setup() {
    const mdParser = new MarkdownIt()
    const renderedMarkdown = ref('')
    const loading = ref(true)
    const error = ref(null)

    const loadMarkdown = async () => {
      loading.value = true
      error.value = null
      try {
        const markdownModule = await import(
            `@/assets/markdown/zh-CN-privacy.md?raw`
            )
        renderedMarkdown.value = mdParser.render(markdownModule.default)
      } catch (err) {
        console.error('Error loading markdown:', err)
        error.value = err
      } finally {
        loading.value = false
      }
    }

    // 用 onMounted 确保组件挂载后加载
    onMounted(loadMarkdown)

    return {renderedMarkdown, loading, error}
  },
}
</script>

<style scoped>
.container {
  text-align: left;
  padding: 20px;
  margin-left: 120px; /* 给侧边栏留出空间，偏右 */
  box-sizing: border-box;
}


.container p {
  margin-bottom: 10px; /* 设置段落之间的间距 */
}

.container ul,
.container ol {
  margin-left: 20px; /* 为列表添加左边距 */
}
</style>
