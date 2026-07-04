<template>
  <div class="h5-classes">
    <div class="header">
      <h2 style="margin:0; font-size:20px; font-weight:800; color:#111;">选择点名班级</h2>
      <div style="font-size: 15px; font-weight: 500; color: #444;" @click="logout">退出登录</div>
    </div>
    
    <div v-loading="loading" style="padding-top: 15px;">
      <div v-if="courses.length === 0 && !loading" style="padding: 30px; text-align: center; color: #999;">
        今日暂无排课
      </div>
      <div style="height: calc(100vh - 65px); overflow-y: auto; padding-bottom: 20px;">
        <div v-for="item in courses" :key="item.courseId" class="class-card">
          <div class="class-info" @click="goToRollcall(item)" style="flex:1">
            <div class="class-title">{{ item.className }}</div>
            <div class="class-code">{{ item.classCode }}</div>
          </div>
          <div class="class-time">{{ item.startTime }} - {{ item.endTime }}</div>
          <div class="class-actions">
            <el-button size="small" type="primary" plain @click="goToRollcall(item)">点名</el-button>
            <el-button size="small" type="info" plain @click="goToHistory(item.classId)">记录</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()
const courses = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getTodayRollcallCourses()
    courses.value = res.data || []
  } finally {
    loading.value = false
  }
}

const goToRollcall = (item) => {
  router.push({
    path: `/h5/rollcall/${item.classId}`,
    query: { courseId: item.courseId, startTime: item.startTime, endTime: item.endTime }
  })
}

const goToHistory = (classId) => {
  router.push(`/h5/history/${classId}`)
}

const logout = () => {
  userStore.logout()
  router.push('/h5/login')
}

onMounted(loadData)
</script>

<style scoped>
.h5-classes {
  min-height: 100vh;
  background: #f4f5f7;
}
.header {
  background: #fff;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.class-card {
  background: #fff;
  margin: 0 15px 15px 15px;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  position: relative;
}
.class-info {
  cursor: pointer;
  min-width: 0;
}
.class-title {
  font-size: 19px;
  font-weight: 800;
  color: #111;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}
.class-code {
  color: #999;
  font-size: 14px;
}
.class-time {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 600;
  color: #409EFF;
  white-space: nowrap;
}
.class-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
</style>
