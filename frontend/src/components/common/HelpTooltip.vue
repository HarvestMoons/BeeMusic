<!-- HelpTooltip.vue -->
<template>
  <div class="help-tooltip-wrapper" @mouseenter="showTooltip" @mouseleave="show = false">
    <!-- 问号图标 -->
    <div class="help-icon">?</div>

    <!-- 浮窗 -->
    <div v-if="show" class="tooltip-box">
      <slot/>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HelpTooltip',
  data() {
    return {
      show: false,
    }
  },
  methods: {
    showTooltip() {
      this.show = true
      this.$nextTick(() => {
        const tooltip = this.$el.querySelector('.tooltip-box')
        const rect = this.$el.getBoundingClientRect()
        tooltip.style.top = `${(rect.bottom + rect.top) / 2}px`
        tooltip.style.right = `${
            document.documentElement.scrollWidth - (rect.right + rect.left) / 2
        }px`
      })
    },
  },
}
</script>

<style scoped>
.help-tooltip-wrapper {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 100;
}

.help-icon {
  width: 16px;
  height: 16px;
  background-color: #938a8a;
  color: white;
  font-weight: bold;
  text-align: center;
  line-height: 16px;
  border-radius: 50%;
  font-size: 12px;
  cursor: pointer;
}

.tooltip-box {
  position: fixed;
  top: auto; /* 会用 JS 设置 */
  left: auto;
  background-color: #333;
  color: #fff;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  white-space: pre-wrap;
  width: 140px;
  z-index: 9999;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
</style>
