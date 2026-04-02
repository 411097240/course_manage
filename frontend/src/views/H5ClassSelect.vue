<template>
  <div class="h5-classes">
    <div class="header">
      <h2 style="margin:0; font-size:20px; font-weight:800; color:#111;">选择点名班级</h2>
      <div style="font-size: 15px; font-weight: 500; color: #444;" @click="logout">退出登录</div>
    </div>
    
    <div v-loading="loading && current === 1" style="padding-top: 15px;">
      <div v-if="classes.length === 0 && !loading" style="padding: 30px; text-align: center; color: #999;">
        暂无管理的班级
      </div>
      <div 
        v-infinite-scroll="loadMore"
        :infinite-scroll-disabled="loading || noMore"
        :infinite-scroll-distance="10"
        :infinite-scroll-immediate="false"
        style="height: calc(100vh - 65px); overflow-y: auto; padding-bottom: 20px;"
      >
        <div v-for="c in classes" :key="c.id" class="class-card">
          <div class="class-info" @click="goToRollcall(c.id)" style="flex:1">
            <div class="class-title">{{ c.className }}</div>
            <div class="class-code">{{ c.classCode }}</div>
          </div>
          <div class="class-actions" style="display:flex;gap:10px;">
            <el-button size="small" type="primary" plain @click="goToRollcall(c.id)">点名</el-button>
            <el-button size="small" type="info" plain @click="goToHistory(c.id)">记录</el-button>
          </div>
        </div>
        <div v-if="loading && current > 1" style="text-align: center; padding: 10px; color: #999; font-size: 13px;">加载中...</div>
        <div v-if="noMore && classes.length > 0" style="text-align: center; padding: 10px; color: #999; font-size: 13px;">没有更多班级了</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()
const classes = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(20)
const total = ref(0)

const noMore = computed(() => classes.value.length >= total.value && total.value > 0)

const loadMore = () => {
  if (loading.value || noMore.value) return
  current.value += 1
  loadData(true)
}

const loadData = async (isAppend = false) => {
  loading.value = true
  try {
    const res = await api.getClassList({ current: current.value, size: size.value })
    if (isAppend) {
      classes.value.push(...(res.data.records || []))
    } else {
      classes.value = res.data.records || []
    }
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const goToRollcall = (classId) => {
  router.push(`/h5/rollcall/${classId}`)
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
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  position: relative;
}
.class-info {
  cursor: pointer;
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
</style>
