<script setup>
import {onBeforeUnmount, onMounted} from 'vue';

// props: 保持和原脚本能传入的属性等价（字符串或数字都接受）
const props = defineProps({
  zIndex: {type: [String, Number], default: -1},
  opacity: {type: [String, Number], default: 0.5},
  color: {type: String, default: '0,0,0'}, // format: "r,g,b"
  count: {type: [String, Number], default: 99}
});

let canvas, ctx, animationId;
let width = 0, height = 0;
let particles = [];
let mouse = {x: null, y: null, max: 20000};
let allParticles = [];
let mounted = false;

function setCanvasSize() {
  if (!canvas) return;
  canvas.width = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
  canvas.height = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
  width = canvas.width;
  height = canvas.height;
}

function createParticles(count) {
  particles = [];
  for (let i = 0; i < count; i++) {
    const x = Math.random() * width;
    const y = Math.random() * height;
    const vx = (Math.random() * 2 - 1);
    const vy = (Math.random() * 2 - 1);
    particles.push({x, y, vx, vy, max: 6000});
  }
  allParticles = particles.concat([mouse]);
}

function drawFrame() {
  if (!ctx) return;
  ctx.clearRect(0, 0, width, height);

  // 迭代绘制
  for (let i = 0; i < particles.length; i++) {
    const p1 = particles[i];

    // 更新位置
    p1.x += p1.vx;
    p1.y += p1.vy;

    // 边界反弹
    if (p1.x > width || p1.x < 0) p1.vx *= -1;
    if (p1.y > height || p1.y < 0) p1.vy *= -1;

    ctx.fillRect(p1.x - 0.5, p1.y - 0.5, 1, 1);

    // 连线（从 i+1 遍历到末尾，避免重复计算）
    for (let j = i + 1; j < allParticles.length; j++) {
      const p2 = allParticles[j];
      if (!p2 || p2.x == null || p2.y == null) continue;

      const dx = p1.x - p2.x;
      const dy = p1.y - p2.y;
      const distSq = dx * dx + dy * dy;

      if (distSq < p2.max) {
        if (p2 === mouse && distSq > p2.max / 2) {
          // 鼠标靠近时产生排斥（保留原逻辑系数）
          p1.x -= dx * 0.03;
          p1.y -= dy * 0.03;
        }

        const lineOpacity = (p2.max - distSq) / p2.max;
        ctx.beginPath();
        ctx.lineWidth = lineOpacity / 2;
        ctx.strokeStyle = `rgba(${props.color},${Math.max(0, Math.min(1, lineOpacity + 0.2))})`;
        ctx.moveTo(p1.x, p1.y);
        ctx.lineTo(p2.x, p2.y);
        ctx.stroke();
      }
    }
  }

  animationId = window.requestAnimationFrame(drawFrame);
}

onMounted(() => {
  mounted = true;

  // 创建 canvas 并附加到 body（保持原脚本把 canvas 放 body 的行为）
  canvas = document.createElement('canvas');
  ctx = canvas.getContext('2d');

  // id 加随机后缀，避免冲突
  canvas.id = 'canvas_background_' + String(Date.now()) + '_' + Math.floor(Math.random() * 10000);

  // 样式（与原脚本一致：fixed 全屏、pointer-events:none）
  canvas.style.cssText = `position:fixed;top:0;left:0;z-index:${props.zIndex};opacity:${props.opacity};pointer-events:none;`;

  document.body.appendChild(canvas);

  // 初始化尺寸与粒子
  setCanvasSize();
  // 转为数字处理
  const countNum = typeof props.count === 'string' ? parseInt(props.count, 10) || 99 : Number(props.count || 99);
  createParticles(countNum);

  // 事件监听（使用 addEventListener，不覆盖全局）
  const resizeHandler = () => {
    setCanvasSize();
    // 重新生成粒子以适应新尺寸（与原脚本保持 setCanvasSize 但原脚本并没有重置粒子；为最接近原脚本，只调整尺寸，不重建）
    // 注：原脚本 setCanvasSize 并未重建粒子，所以不在这里重建
  };
  const mousemoveHandler = (e) => {
    mouse.x = e.clientX;
    mouse.y = e.clientY;
  };
  const mouseoutHandler = () => {
    mouse.x = null;
    mouse.y = null;
  };

  window.addEventListener('resize', resizeHandler);
  window.addEventListener('mousemove', mousemoveHandler);
  window.addEventListener('mouseout', mouseoutHandler);

  // 启动动画（延时保持与原脚本近似）
  setTimeout(() => {
    // 如果画布宽高变动导致粒子位置超出，可选择不重建（原脚本不重建），此处保留不重建以严格保持行为
    animationId = window.requestAnimationFrame(drawFrame);
  }, 100);

  // 存放引用以便卸载时清理
  canvas.__bgParticlesCleanup = () => {
    // 清理 RAF
    if (animationId) {
      cancelAnimationFrame(animationId);
      animationId = null;
    }
    // 移除事件
    window.removeEventListener('resize', resizeHandler);
    window.removeEventListener('mousemove', mousemoveHandler);
    window.removeEventListener('mouseout', mouseoutHandler);
    // 移除 canvas
    try {
      if (canvas && canvas.parentNode) canvas.parentNode.removeChild(canvas);
    } catch (e) {
    }
    // 清空引用
    canvas = null;
    ctx = null;
    particles = [];
    allParticles = [];
    mouse = {x: null, y: null, max: 20000};
  };
});

onBeforeUnmount(() => {
  // 执行清理
  if (canvas && typeof canvas.__bgParticlesCleanup === 'function') {
    try {
      canvas.__bgParticlesCleanup();
    } catch (e) { /* ignore */
    }
    delete canvas.__bgParticlesCleanup;
  } else {
    // 兜底清理
    if (animationId) cancelAnimationFrame(animationId);
    window.removeEventListener('resize', setCanvasSize);
    window.removeEventListener('mousemove', () => {
    });
    window.removeEventListener('mouseout', () => {
    });
  }
});
</script>