<template>
  <div class="spectrum-container">
    <canvas id="spectrumCanvas"></canvas>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, watch} from 'vue';
import {useThemeStore} from '@/store/index.js';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const themeStore = useThemeStore();
let rafId = null;
let audioCtx = null;
let source = null;
let analyser = null;
let removeAudioResumeListeners = null;

onMounted(() => {
  const audio = document.getElementById("audio-player");
  const canvas = document.getElementById("spectrumCanvas");
  const rect = canvas.getBoundingClientRect();

  canvas.width = rect.width;
  canvas.height = rect.height;


  if (!audio || !canvas) {
    console.error("找不到 #audio-player 或 #spectrumCanvas，无法启动可视化");
    return;
  }

  const ctx = canvas.getContext("2d");
  audioCtx = new AudioContext();

  try {
    // createMediaElementSource 在同一页面只能被创建一次连接到同一个媒体元素。
    // 如果已经有 source 则先断开（防御性处理）
    source = audioCtx.createMediaElementSource(audio);
  } catch (e) {
    // 如果 createMediaElementSource 抛错，仍尝试继续（保持原始行为）
    console.warn("createMediaElementSource 可能已被创建:", e);
    try {
      // 若再次 create 失败，设置 source 为 null（后续绘制依赖 analyser）
      source = null;
    } catch (_) {
      source = null;
    }
  }

  analyser = audioCtx.createAnalyser();
  analyser.fftSize = 1024;

  const bufferLength = analyser.frequencyBinCount;
  const dataArray = new Uint8Array(bufferLength);

  if (source) {
    source.connect(analyser);
    analyser.connect(audioCtx.destination);
  } else {
    analyser.connect(audioCtx.destination);
  }

  // 初始设置容器高度
  const container = canvas.parentElement;
  if (container) {
    const height = props.visible ? "310px" : "0";
    const opacity = props.visible ? "1" : "0";
    container.style.height = height;
    canvas.style.opacity = opacity;
  }

  watch(() => props.visible, (val) => {
    const height = val ? "310px" : "0";
    const opacity = val ? "1" : "0";
    if (container) {
      container.style.height = height;
      canvas.style.opacity = opacity;
    }
  });

  // 渐变色配置
  let currentGradientColors = [];

  const lightGradient = [
    {stop: 0, color: "rgba(228, 216, 181, 0.64)"},
    {stop: 0.5, color: "rgba(214, 199, 161, 0.55)"},
    {stop: 1, color: "rgba(194, 176, 138, 0.48)"},
  ];

  const darkGradient = [
    {stop: 0, color: "rgba(176, 190, 197, 0.8)"},
    {stop: 0.5, color: "rgba(144, 164, 174, 0.7)"},
    {stop: 1, color: "rgba(120, 144, 156, 0.6)"},
  ];

  let backgroundColor = "#fefae8";

  function updateGradientColors() {
    currentGradientColors = themeStore.isDarkMode ? darkGradient : lightGradient;
    backgroundColor = themeStore.isDarkMode ? "#222222" : "#fefae8";
  }

  // 初始化并监听主题变化
  updateGradientColors();
  watch(() => themeStore.isDarkMode, updateGradientColors);

  function getGradient(x1, y1, x2, y2) {
    const grad = ctx.createLinearGradient(x1, y1, x2, y2);
    currentGradientColors.forEach(c => grad.addColorStop(c.stop, c.color));
    return grad;
  }

  let mode = "spectrum"; // "spectrum" / "waveform" / "circle"

  function clearCanvas() {
    ctx.fillStyle = backgroundColor;
    ctx.fillRect(0, 0, canvas.width, canvas.height);
  }

  function drawSpectrum() {
    analyser.getByteFrequencyData(dataArray);
    clearCanvas();
    const barWidth = (canvas.width / bufferLength) * 2.5;
    let x = 0;
    for (let i = 0; i < bufferLength; i++) {
      const barHeight = dataArray[i] * 0.8;
      ctx.fillStyle = getGradient(0, 0, 0, canvas.height);
      ctx.fillRect(x, canvas.height - barHeight, barWidth, barHeight);
      x += barWidth + 1;
    }
  }

  function drawWaveform() {
    analyser.getByteTimeDomainData(dataArray);
    clearCanvas();
    ctx.lineWidth = 1.5;
    ctx.strokeStyle = getGradient(0, 0, 0, canvas.height);
    ctx.beginPath();
    const sliceWidth = canvas.width / bufferLength;
    let x = 0;
    for (let i = 0; i < bufferLength; i++) {
      const v = dataArray[i] / 128.0;
      const y = (v * canvas.height) / 2;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
      x += sliceWidth;
    }
    ctx.lineTo(canvas.width, canvas.height / 2);
    ctx.stroke();
  }

  function drawCircleSpectrum() {
    analyser.getByteFrequencyData(dataArray);
    clearCanvas();
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const radius = Math.min(centerX, centerY) * 0.35;
    const bars = 128;
    const step = Math.floor(dataArray.length / bars);
    for (let i = 0; i < bars; i++) {
      const value = dataArray[i * step];
      const barHeight = value * 0.5;
      const angle = (i / bars) * Math.PI * 2;
      const x1 = centerX + Math.cos(angle) * radius;
      const y1 = centerY + Math.sin(angle) * radius;
      const x2 = centerX + Math.cos(angle) * (radius + barHeight);
      const y2 = centerY + Math.sin(angle) * (radius + barHeight);
      ctx.strokeStyle = getGradient(x1, y1, x2, y2);
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();
    }
  }

  function draw() {
    rafId = requestAnimationFrame(draw);

    if (canvas.clientWidth && canvas.clientHeight) {
      if (canvas.width !== canvas.clientWidth || canvas.height !== canvas.clientHeight) {
        canvas.width = canvas.clientWidth;
        canvas.height = canvas.clientHeight;
      }
    }

    if (!props.visible) {
      clearCanvas();
      return;
    }
    if (mode === "spectrum") drawSpectrum();
    else if (mode === "waveform") drawWaveform();
    else if (mode === "circle") drawCircleSpectrum();
  }

  function resumeAudioContext() {
    if (audioCtx && audioCtx.state === "suspended") {
      audioCtx.resume().catch(() => {
      })
    }
  }

  const clickResumeHandler = () => resumeAudioContext();
  const keydownResumeHandler = () => resumeAudioContext();

  document.addEventListener("click", clickResumeHandler);
  document.addEventListener("keydown", keydownResumeHandler);

  // 重要：用户第一次点播放器控件时，也要同步唤醒频谱使用的 AudioContext，避免必须额外点页面其他位置。
  const audioResumeEvents = ["play", "playing", "pointerdown", "mousedown", "touchstart"];
  const audioResumeHandler = () => resumeAudioContext();
  audioResumeEvents.forEach((eventName) => {
    audio.addEventListener(eventName, audioResumeHandler);
  });
  removeAudioResumeListeners = () => {
    audioResumeEvents.forEach((eventName) => {
      audio.removeEventListener(eventName, audioResumeHandler);
    });
    removeAudioResumeListeners = null;
  };

  // 按 Z 切换模式
  const modeKeyHandler = (e) => {
    if (!e.key || e.key.toLowerCase() !== "z") return;
    mode = mode === "spectrum" ? "waveform" : mode === "waveform" ? "circle" : "spectrum";
  };
  document.addEventListener("keydown", modeKeyHandler);

  // 启动绘制循环
  draw();

  // 存放清理函数到 element 上，便于卸载时找到
  canvas.__spectrumCleanup = () => {
    document.removeEventListener("click", clickResumeHandler);
    document.removeEventListener("keydown", keydownResumeHandler);
    document.removeEventListener("keydown", modeKeyHandler);
    removeAudioResumeListeners?.();
    if (rafId) cancelAnimationFrame(rafId);

    [source, analyser].forEach(node => {
      try {
        node?.disconnect();
      } catch (e) {
      }
    });
    try {
      audioCtx?.close();
    } catch (e) {
    }
    rafId = null;
    audioCtx = null;
    source = null;
    analyser = null;
  };
});

onBeforeUnmount(() => {
  // 执行清理
  const canvas = document.getElementById("spectrumCanvas");
  if (canvas && typeof canvas.__spectrumCleanup === "function") {
    canvas.__spectrumCleanup();
    delete canvas.__spectrumCleanup;
  } else {
    // 兜底：取消 raf 与关闭 audioCtx
    if (rafId) cancelAnimationFrame(rafId);
    removeAudioResumeListeners?.();
    if (analyser) analyser.disconnect();
    if (source) source.disconnect();
    if (audioCtx && typeof audioCtx.close === 'function') audioCtx.close();
  }
});
</script>

<style>
.spectrum-container {
  position: relative;
  margin: 0;
  width: 100%;
  max-width: none;
  padding: 15px 20px;
  box-sizing: border-box;
  background: var(--spectrum-bg);
  border-radius: 12px;
  overflow: hidden;
  transition-property: height, background;
  transition-duration: 0.3s;
  transition-timing-function: ease;
  min-height: 15px;
}

#spectrumCanvas {
  display: block;
  width: 100%;
  height: 280px;
  border-radius: 8px;
  background-color: #fff8f0;
  transition-property: opacity;
  transition-duration: 0.3s;
  transition-timing-function: ease;
}
</style>
