<template>
  <div class="h5-history">
    <div class="header">
      <h2 style="margin:0; font-size:18px; font-weight:800; color:#111;">历史签到记录</h2>
      <el-button @click="$router.push('/h5/classes')" size="small">返回</el-button>
    </div>
    
    <div class="tip" v-if="className">
      <span>班级：{{ className }}</span>
    </div>

    <div v-loading="loading && current === 1">
      <div v-if="records.length === 0 && !loading" style="padding: 20px; text-align: center; color: #999;">
        暂无签到记录
      </div>
      <div 
        v-infinite-scroll="loadMore"
        :infinite-scroll-disabled="loading || noMore"
        :infinite-scroll-distance="10"
        :infinite-scroll-immediate="false"
        style="height: calc(100vh - 120px); overflow-y: auto; padding-bottom: 20px;"
      >
        <div v-for="r in records" :key="r.id" class="record-card" @click="openDetail(r)">
        <div class="record-info">
          <div class="date">📅 {{ r.recordDate }}</div>
          <div class="teacher" style="color: #666; font-size: 13px; margin-top: 5px;">
            点名人员: {{ r.teacherName }}
          </div>
        </div>
        <div class="record-action">
          <span style="font-size:14px;color:#409EFF">查看详情</span>
        </div>
        </div>
        <div v-if="loading && current > 1" style="text-align: center; padding: 10px; color: #999; font-size: 13px;">加载中...</div>
        <div v-if="noMore && records.length > 0" style="text-align: center; padding: 10px; color: #999; font-size: 13px;">没有更多数据了</div>
      </div>
    </div>

    <!-- Detail Overlay -->
    <el-drawer v-model="detailVisible" direction="b" size="80%" :title="`签到明细 - ${selectedDate}`" :with-header="true">
      <div v-loading="detailLoading" class="detail-container">
        <div v-if="details.length === 0" style="padding: 20px; text-align: center; color: #999;">
          暂无明细
        </div>
        <div v-for="d in details" :key="d.studentId" class="detail-item">
          <div>
            <span class="stu-name">{{ d.studentName }}</span>
            <span class="stu-no">({{ d.studentNo }})</span>
          </div>
          <div>
            <el-tag v-if="d.status===1" type="success" effect="dark">出勤</el-tag>
            <el-tag v-else-if="d.status===2" type="warning" effect="dark">迟到</el-tag>
            <el-tag v-else-if="d.status===3" type="info" effect="dark">请假</el-tag>
            <el-tag v-else type="danger" effect="dark">缺勤</el-tag>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const classId = route.params.classId
const className = ref('')

const loading = ref(false)
const records = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

const detailVisible = ref(false)
const detailLoading = ref(false)
const details = ref([])
const selectedDate = ref('')

const noMore = computed(() => records.value.length >= total.value && total.value > 0)

const loadMore = () => {
  if (loading.value || noMore.value) return
  current.value += 1
  loadData(true)
}

const loadData = async (isAppend = false) => {
  loading.value = true
  try {
    const classRes = await api.getClassById(classId)
    className.value = classRes.data.className

    const res = await api.getRollcallRecords({ classId, current: current.value, size: size.value })
    if (isAppend) {
      records.value.push(...(res.data.records || []))
    } else {
      records.value = res.data.records || []
    }
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const openDetail = async (record) => {
  selectedDate.value = record.recordDate
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await api.getRollcallDetail(record.id)
    details.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  if (!classId) {
    ElMessage.error('缺少班级参数')
    return
  }
  loadData()
})
</script>

<style scoped>
.h5-history {
  min-height: 100vh;
  background: #f4f5f7;
  padding-bottom: 20px;
}
.header {
  background: #fff;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.tip {
  padding: 10px 20px;
  color: #666;
  font-size: 14px;
}
.record-card {
  background: #fff;
  margin: 10px 15px;
  padding: 15px 20px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  cursor: pointer;
  position: relative;
}
.record-card:active {
  background: #f9fafc;
}
.date {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}
.detail-container {
  height: 100%;
  overflow-y: auto;
  padding: 0 20px 20px 20px;
}
.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #eee;
}
.stu-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}
.stu-no {
  font-size: 13px;
  color: #999;
  margin-left: 6px;
}
</style>
