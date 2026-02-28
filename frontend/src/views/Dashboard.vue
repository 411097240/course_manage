<template>
  <div class="dashboard">
    <div class="page-header">
      <h1 class="page-title">数据概览</h1>
    </div>
    <div class="stats-grid">
      <div class="stat-card" v-for="item in stats" :key="item.label">
        <div class="stat-icon" :style="{ background: item.gradient }">
          <span v-html="item.icon"></span>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>
    <div class="welcome-card page-card">
      <h2>欢迎使用教学管理系统</h2>
      <p>您可以通过左侧菜单进入各功能模块进行操作。</p>
      <div class="quick-links">
        <router-link to="/class" class="quick-link">
          <span>📚</span> 班级管理
        </router-link>
        <router-link to="/student" class="quick-link">
          <span>👨‍🎓</span> 学生管理
        </router-link>
        <router-link v-if="userStore.isAdmin()" to="/assign" class="quick-link">
          <span>👨‍🏫</span> 教师分配
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../api'

const userStore = useUserStore()

const stats = ref([
  { label: '班级总数', value: '-', gradient: 'linear-gradient(135deg, #6366f1, #818cf8)', icon: '📚' },
  { label: '学生总数', value: '-', gradient: 'linear-gradient(135deg, #22c55e, #4ade80)', icon: '👨‍🎓' },
  { label: '当前身份', value: userStore.role === 1 ? '管理员' : '教师', gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)', icon: '👤' }
])

onMounted(async () => {
  try {
    const classRes = await api.getClassList({ current: 1, size: 1 })
    stats.value[0].value = classRes.data.total || 0
    const studentRes = await api.getStudentList({ current: 1, size: 1 })
    stats.value[1].value = studentRes.data.total || 0
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.welcome-card h2 {
  font-size: 20px;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.welcome-card p {
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.quick-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.quick-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 10px;
  color: var(--primary-light);
  text-decoration: none;
  font-weight: 500;
  font-size: 14px;
  transition: all 0.2s;
}

.quick-link:hover {
  background: rgba(99, 102, 241, 0.2);
  transform: translateY(-1px);
}
</style>
