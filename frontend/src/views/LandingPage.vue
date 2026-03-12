<template>
  <div class="landing-root">
    <div class="landing-card">
      <img src="/logo.png" class="logo" alt="Logo" />
      <h1>波奇探險隊</h1>
      <p class="subtitle">請選擇你的地區</p>

      <div class="region-buttons">
        <button class="region-btn taichung" @click="router.push('/taichung')">
          <span class="region-icon">🏙️</span>
          <span class="region-name">台中</span>
        </button>
        <button class="region-btn taipei" @click="router.push('/taipei')">
          <span class="region-icon">🗼</span>
          <span class="region-name">台北</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.landing-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fdf6ec 0%, #f5ede0 100%);
}

.landing-card {
  background: white;
  border-radius: 2rem;
  padding: 3rem 2.5rem;
  text-align: center;
  box-shadow: 0 8px 30px rgba(0,0,0,0.06);
  border: 1px solid #f0e8df;
  width: 100%;
  max-width: 380px;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 0.75rem;
  filter: drop-shadow(0 2px 6px rgba(0,0,0,0.1));
}

h1 {
  font-size: 1.875rem;
  font-weight: 800;
  color: #3c2a1a;
  margin: 0 0 0.25rem;
  letter-spacing: -0.5px;
}

.subtitle {
  color: #9c8b7a;
  font-size: 0.9rem;
  margin-bottom: 2rem;
}

.region-buttons {
  display: flex;
  gap: 1rem;
}

.region-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.25rem 1rem;
  border-radius: 1.25rem;
  border: 2px solid transparent;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;
}

.region-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.12);
}

.region-btn:active {
  transform: scale(0.97);
}

.region-btn.taichung {
  background: #fdf3e7;
  border-color: #e8c99a;
  color: #7a4a1a;
}

.region-btn.taichung:hover {
  background: #f9e8ce;
}

.region-btn.taipei {
  background: #edf3fd;
  border-color: #9ab8e8;
  color: #1a3c7a;
}

.region-btn.taipei:hover {
  background: #ddeafc;
}

.region-icon {
  font-size: 2rem;
}

.region-name {
  font-size: 1.1rem;
  font-weight: 700;
}
</style>
