<!-- src/components/MySpinner.vue -->
<template>
  <SimpleSpinner
      size="70"
      line-size="5"
      :message="message || 'loading...'"
      line-bg-color="#DDDDDD"
      line-fg-color="#938A8A"
  />
</template>

<script>
import SimpleSpinner from 'vue-simple-spinner/src'

export default {
  components: {SimpleSpinner},
  props: {
    message: String, // 允许覆盖默认文本
  },
}
</script>
