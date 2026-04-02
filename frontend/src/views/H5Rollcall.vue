<template>
  <div class="h5-rollcall">
    <div class="header">
      <h2 style="margin:0; font-size:18px;">课堂点名</h2>
      <el-button @click="$router.push('/h5/classes')" size="small">返回</el-button>
    </div>
    
    <div class="tip" v-if="className">
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <span>班级：{{ className }}</span>
        <el-tag v-if="hasExisting" type="warning" effect="plain" size="small">今天已点名 (编辑中)</el-tag>
      </div>
      <div>日期：{{ form.recordDate }}</div>
    </div>

    <div v-loading="loading">
      <div v-if="students.length === 0" style="padding: 20px; text-align: center; color: #999;">
        班级暂无学生
      </div>
      <div v-for="(stu, index) in students" :key="stu.id" class="stu-card">
        <div class="stu-info">
          <span class="stu-name">{{ stu.name }}</span>
          <span class="stu-no">{{ stu.studentNo }}</span>
        </div>
        <div class="stu-status">
          <el-radio-group v-model="details[index].status">
            <el-radio-button :label="1"><span style="color:#67c23a">出勤</span></el-radio-button>
            <el-radio-button :label="2"><span style="color:#e6a23c">迟到</span></el-radio-button>
            <el-radio-button :label="3"><span style="color:#909399">请假</span></el-radio-button>
            <el-radio-button :label="4"><span style="color:#f56c6c">缺勤</span></el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </div>

    <div class="bottom-bar">
      <el-button :type="hasExisting ? 'warning' : 'primary'" style="width:100%" size="large" @click="submit" :loading="submitting" :disabled="students.length === 0">
        {{ hasExisting ? '更新点名' : '提交点名' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()
const classId = route.params.classId

const loading = ref(false)
const submitting = ref(false)
const students = ref([])
const details = ref([])
const className = ref('')
const hasExisting = ref(false)

const form = ref({
  recordDate: new Date().toISOString().split('T')[0]
})

const loadData = async () => {
  loading.value = true
  try {
    const classRes = await api.getClassById(classId)
    className.value = classRes.data.className

    const res = await api.getRollcallStudents(classId, form.value.recordDate)
    students.value = res.data.students || []
    hasExisting.value = res.data.hasExisting || false
    details.value = students.value.map(s => ({
      studentId: s.id,
      status: s.status || 1
    }))
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  submitting.value = true
  try {
    await api.submitRollcall({
      record: { classId, recordDate: form.value.recordDate },
      details: details.value
    })
    ElMessage.success(hasExisting.value ? '点名记录已更新' : '点名提交成功')
    setTimeout(() => {
      router.push('/h5/classes')
    }, 1000)
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!classId) {
    ElMessage.error('缺少 classId 参数')
    router.push('/class')
    return
  }
  loadData()
})
</script>

<style scoped>
.h5-rollcall {
  min-height: 100vh;
  background: #f4f5f7;
  padding-bottom: 80px;
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
.stu-card {
  background: #fff;
  margin: 10px 15px;
  padding: 15px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}
.stu-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed #eee;
  padding-bottom: 10px;
}
.stu-name {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}
.stu-no {
  color: #999;
  font-size: 14px;
}
.stu-status {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
.stu-status :deep(.el-radio-group) {
  width: 100%;
  display: flex;
}
.stu-status :deep(.el-radio-button) {
  flex: 1;
}
.stu-status :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 8px 0;
  text-align: center;
}
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 15px 20px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  z-index: 10;
}
</style>
