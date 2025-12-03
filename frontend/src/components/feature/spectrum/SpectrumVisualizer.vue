<template>
  <div class="spectrum-container">
    <HelpTooltip v-show="visible">- 按 Z 切换频谱模式</HelpTooltip>
    <canvas id="spectrumCanvas"></canvas>
  </div>
</template>

<script setup>
import {onMounted, onBeforeUnmount, ref, watch} from 'vue';
import HelpTooltip from "@/components/common/HelpTooltip.vue";
import { useThemeStore } from '@/store/index.js';

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
    // 若 source 无法建立，仍把 analyser 连接到输出（尽力而为）
    try {
      analyser.connect(audioCtx.destination);
    } catch (e) {
      // ignore
    }
  }

  // 初始设置容器高度
  const container = canvas.parentElement;
  if (container) {
    // Initialize based on prop
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
    { stop: 0, color: "rgba(255,200,180,0.8)" },
    { stop: 0.5, color: "rgba(255,180,150,0.7)" },
    { stop: 1, color: "rgba(255,160,120,0.6)" },
  ];

  const darkGradient = [
    { stop: 0, color: "rgba(176, 190, 197, 0.8)" },   // 低饱和度蓝灰
    { stop: 0.5, color: "rgba(144, 164, 174, 0.7)" },
    { stop: 1, color: "rgba(120, 144, 156, 0.6)" },
  ];

  let backgroundColor = "#fff8f0";

  function updateGradientColors() {
    currentGradientColors = themeStore.isDarkMode ? darkGradient : lightGradient;
    backgroundColor = themeStore.isDarkMode ? "#222222" : "#fff8f0";
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
    if (!props.visible) {
      clearCanvas();
      return;
    }
    if (mode === "spectrum") drawSpectrum();
    else if (mode === "waveform") drawWaveform();
    else if (mode === "circle") drawCircleSpectrum();
  }

  function resumeAudioContext() {
    if (audioCtx && audioCtx.state === "suspended") audioCtx.resume();
  }

  const clickResumeHandler = () => resumeAudioContext();
  const keydownResumeHandler = () => resumeAudioContext();

  document.addEventListener("click", clickResumeHandler);
  document.addEventListener("keydown", keydownResumeHandler);

  // 按 Z 切换模式（保留原逻辑）
  const modeKeyHandler = (e) => {
    if (e.key.toLowerCase() !== "z") return;
    mode = mode === "spectrum" ? "waveform" : mode === "waveform" ? "circle" : "spectrum";
  };
  document.addEventListener("keydown", modeKeyHandler);

  // 启动绘制循环
  draw();

  // 存放清理函数到 element 上，便于卸载时找到（可选）
  canvas.__spectrumCleanup = () => {
    document.removeEventListener("click", clickResumeHandler);
    document.removeEventListener("keydown", keydownResumeHandler);
    document.removeEventListener("keydown", modeKeyHandler);
    if (rafId) cancelAnimationFrame(rafId);

    [source, analyser].forEach(node => { try { node?.disconnect(); } catch(e){} });
    try { audioCtx?.close(); } catch(e){}
    rafId = null; audioCtx = null; source = null; analyser = null;
  };
});

onBeforeUnmount(() => {
  // 执行清理
  const canvas = document.getElementById("spectrumCanvas");
  if (canvas && typeof canvas.__spectrumCleanup === "function") {
    try { canvas.__spectrumCleanup(); } catch (e) { /* ignore */ }
    delete canvas.__spectrumCleanup;
  } else {
    // 兜底：取消 raf 与关闭 audioCtx
    if (rafId) cancelAnimationFrame(rafId);
    try {
      if (analyser) analyser.disconnect();
      if (source) source.disconnect();
      if (audioCtx && typeof audioCtx.close === 'function') audioCtx.close();
    } catch (e) {}
  }
});
</script>

<style>
.spectrum-container {
  position: relative;       /* 确保内部绝对定位元素生效 */
  margin: 0;                /* 去掉居中 */
  width: 100%;              /* 占满父容器 */
  max-width: none;          /* 取消 max-width */
  padding: 15px 20px;
  box-sizing: border-box;   /* 让 padding 不增加总宽度 */
  background: var(--spectrum-bg);
  border-radius: 12px;
  overflow: hidden;
  transition: height 0.3s ease, background 0.3s ease;
  min-height: 15px;
}

#spectrumCanvas {
  display: block;
  width: 100%;
  height: 280px;
  border-radius: 8px;
  background-color: #fff8f0;
  /* 添加过渡效果 */
  transition: opacity 0.3s ease;
}

.spectrum-container .help-tooltip-wrapper {
  position: absolute;
  top: 18px;
  right: 23px;
}

</style>
