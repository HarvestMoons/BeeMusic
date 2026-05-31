<template>
  <Transition name="entry-cover-fade">
    <section
        v-if="visible"
        class="entry-cover"
        :style="coverStyle"
        aria-label="网站入口封面"
        @click="enterSite"
    >
      <div class="entry-cover__shade"></div>
      <div class="entry-cover__content">
        <p class="entry-cover__eyebrow">Bee Music</p>
        <h1 class="entry-cover__quote">{{ quote }}</h1>
        <p class="entry-cover__hint">点击任意位置进入</p>
        <button class="entry-cover__button" type="button" @click.stop="enterSite">
          进入网站
        </button>
      </div>
    </section>
  </Transition>
</template>

<script setup>
import {computed, onBeforeUnmount, ref, watch} from 'vue'

const entryCoverImages = import.meta.glob('/src/assets/cover/entry_cover/*.{png,jpg,jpeg,webp,avif,gif}', {
  eager: true,
  query: '?url',
  import: 'default'
})

const props = defineProps({
  visible: {
    type: Boolean,
    default: true
  },
  quote: {
    type: String,
    default: '这里先放一句你喜欢的话。'
  },
  imageUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['enter'])
const randomCoverImage = ref(pickRandomCoverImage())
let previousBodyOverflow = ''
let previousHtmlOverflow = ''

function pickRandomCoverImage() {
  const images = Object.values(entryCoverImages)
  if (images.length === 0) return ''
  return images[Math.floor(Math.random() * images.length)]
}

const coverStyle = computed(() => {
  const imageUrl = props.imageUrl || randomCoverImage.value
  if (!imageUrl) return {}
  return {
    backgroundImage: `url("${imageUrl}")`
  }
})

watch(
    () => props.visible,
    (visible) => {
      if (visible) {
        previousBodyOverflow = document.body.style.overflow
        previousHtmlOverflow = document.documentElement.style.overflow
        document.body.style.overflow = 'hidden'
        document.documentElement.style.overflow = 'hidden'
      } else {
        document.body.style.overflow = previousBodyOverflow
        document.documentElement.style.overflow = previousHtmlOverflow
      }
    },
    {immediate: true}
)

onBeforeUnmount(() => {
  document.body.style.overflow = previousBodyOverflow
  document.documentElement.style.overflow = previousHtmlOverflow
})

function enterSite() {
  emit('enter')
}
</script>

<style scoped>
.entry-cover {
  position: fixed;
  inset: 0;
  z-index: 5000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  box-sizing: border-box;
  overflow: hidden;
  cursor: pointer;
  color: #fff;
  background:
      radial-gradient(circle at 20% 20%, rgba(255, 214, 102, 0.35), transparent 28%),
      radial-gradient(circle at 80% 30%, rgba(128, 203, 196, 0.28), transparent 30%),
      linear-gradient(135deg, #151515 0%, #2e2419 45%, #090909 100%);
  background-position: center;
  background-size: cover;
}

.entry-cover__shade {
  position: absolute;
  inset: 0;
  background:
      linear-gradient(180deg, rgba(0, 0, 0, 0.25), rgba(0, 0, 0, 0.58)),
      rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(1px);
}

.entry-cover__content {
  position: relative;
  z-index: 1;
  width: min(780px, 100%);
  text-align: center;
  text-shadow: 0 8px 30px rgba(0, 0, 0, 0.45);
  animation: cover-content-rise 0.8s ease both;
}

.entry-cover__eyebrow {
  margin: 0 0 18px;
  font-family: var(--round-font);
  font-size: clamp(18px, 2vw, 26px);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.86;
}

.entry-cover__quote {
  margin: 0;
  font-family: var(--round-font);
  font-size: clamp(34px, 7vw, 78px);
  font-weight: 700;
  line-height: 1.2;
}

.entry-cover__hint {
  margin: 28px 0 18px;
  font-size: 15px;
  letter-spacing: 0.12em;
  opacity: 0.78;
}

.entry-cover__button {
  padding: 12px 28px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 999px;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
  cursor: pointer;
  font: inherit;
  letter-spacing: 0.08em;
  backdrop-filter: blur(8px);
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.entry-cover__button:hover,
.entry-cover__button:focus-visible {
  border-color: rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.22);
  transform: translateY(-2px);
  outline: none;
}

.entry-cover-fade-enter-active,
.entry-cover-fade-leave-active {
  transition: opacity 0.45s ease;
}

.entry-cover-fade-enter-from,
.entry-cover-fade-leave-to {
  opacity: 0;
}

@keyframes cover-content-rise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .entry-cover {
    padding: 18px;
  }

  .entry-cover__hint {
    margin-top: 22px;
  }
}
</style>
