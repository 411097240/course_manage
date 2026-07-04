<template>
  <div class="student-detail">
    <el-button text @click="$router.push('/student')" style="margin-bottom:12px;color:var(--text-muted)">
      ← 返回学生列表
    </el-button>

    <!-- 学生信息卡 -->
    <div class="info-card page-card" v-if="student">
      <div class="info-header">
        <div class="student-avatar">{{ student.name?.charAt(0) }}</div>
        <div>
          <h2>{{ student.name }}</h2>
          <p class="student-no">学号：{{ student.studentNo }}</p>
        </div>
        <span :class="['status-badge', student.status === 1 ? 'active' : 'inactive']" style="margin-left:auto;margin-right:16px;">
          {{ student.status === 1 ? '启用' : '禁用' }}
        </span>
        <el-button type="primary" @click="openPrintDialog">打印入班凭证</el-button>
      </div>
      <div class="info-grid">
        <div class="info-item"><label>性别</label><span>{{ student.gender === 1 ? '男' : student.gender === 2 ? '女' : '-' }}</span></div>
        <div class="info-item"><label>手机号</label><span>{{ student.phone || '-' }}</span></div>
      </div>
    </div>

    <!-- 已加入班级 -->
    <div class="page-card" style="margin-top:20px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="color:var(--text-primary)">📖 已加入班级</h3>
      </div>
      <el-table :data="classes" v-loading="classLoading">
        <el-table-column prop="classCode" label="班级编码" width="150" />
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column label="培训周期" min-width="200">
          <template #default="{ row }">
            <span v-if="row.startDate">{{ row.startDate }} ~ {{ row.endDate }}</span>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="班级状态" width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.classStatus === 1 ? 'active' : 'inactive']">
              {{ row.classStatus === 1 ? '进行中' : '已结束' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="在读状态" width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.status === 1 ? 'active' : 'inactive']">
              {{ row.status === 1 ? '在读' : '已退出' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="入班时间" width="180">
          <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="primary" plain @click="copyHomeworkLink(row.classId)">空间链接</el-button>
            <el-button v-if="row.status === 1 && row.classStatus === 1" size="small" type="danger" @click="handleLeave(row.classId)">出班</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 课程表 -->
    <div class="page-card" style="margin-top:20px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="color:var(--text-primary)">📅 课程表检索</h3>
        <el-date-picker v-model="selectedDate" type="date" placeholder="选择日期查看课表" value-format="YYYY-MM-DD" :clearable="false" />
      </div>
      <div v-if="schedule.length === 0 && !scheduleLoading" style="text-align:center;padding:40px;color:var(--text-muted)">
        暂无课程数据
      </div>
      <div v-else class="week-grid">
        <div class="week-col" v-for="day in 7" :key="day">
          <div class="week-header">{{ dayNames[day - 1] }}</div>
          <div class="week-body">
            <div v-for="course in getScheduleByDay(day)" :key="course.courseId" class="week-course-card">
              <div class="wc-time">{{ course.startTime }} - {{ course.endTime }}</div>
              <div class="wc-teacher" v-if="course.teacherName">👨‍🏫 {{ course.teacherName }}</div>
              <div class="wc-location" v-if="course.location">📍 {{ course.location }}</div>
              <div class="wc-class">{{ course.className }}</div>
            </div>
            <div v-if="getScheduleByDay(day).length === 0" class="week-empty">暂无</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 凭证班级选择弹窗 -->
    <el-dialog v-model="printDialogVisible" title="选择打印班级" width="400px" destroy-on-close>
      <p style="margin-bottom:12px;color:var(--text-secondary)">请选择要打印入班凭证的班级：</p>
      <el-select v-model="printClassId" placeholder="请选择进行中的班级" style="width:100%">
        <el-option v-for="c in activeClasses" :key="c.classId" :label="`${c.classCode} - ${c.className}`" :value="c.classId" />
      </el-select>
      <template #footer>
        <el-button @click="printDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executePrint">确认打印</el-button>
      </template>
    </el-dialog>
    
    <!-- 打印用的入班凭证模板 (仅在打印时可见) -->
    <div class="print-voucher-container">
      <div class="voucher-header">
        <div class="brand-title">享学未来</div>
        <h2>入班凭证</h2>
      </div>

      <div class="voucher-section" v-if="student">
        <h3 class="section-title">学生基本信息</h3>
        <div class="info-row">
          <span>姓名：{{ student.name }}</span>
          <span>学号：{{ student.studentNo }}</span>
          <span>性别：{{ student.gender === 1 ? '男' : student.gender === 2 ? '女' : '-' }}</span>
          <span>手机：{{ student.phone || '-' }}</span>
        </div>
      </div>

      <div class="voucher-section" v-if="selectedPrintClassData">
        <h3 class="section-title">报名班级</h3>
        <table class="voucher-table">
          <thead>
            <tr>
              <th>班级编码</th>
              <th>班级名称</th>
              <th>培训周期</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{{ selectedPrintClassData.classCode }}</td>
              <td>{{ selectedPrintClassData.className }}</td>
              <td>{{ selectedPrintClassData.startDate ? `${selectedPrintClassData.startDate} ~ ${selectedPrintClassData.endDate}` : '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="voucher-section" v-if="printSchedule.length">
        <h3 class="section-title">固定课程排期</h3>
        <table class="voucher-table">
          <thead>
            <tr>
              <th>星期</th>
              <th>时间</th>
              <th>授课教师</th>
              <th>上课地点</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="course in printSchedule" :key="course.courseId">
              <td>{{ dayNames[course.dayOfWeek - 1] }}</td>
              <td>{{ course.startTime }} - {{ course.endTime }}</td>
              <td>{{ course.teacherName || '-' }}</td>
              <td>{{ course.location || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="voucher-section hw-link-section">
        <h3 class="section-title">专属班级空间</h3>
        <div class="hw-link-box" style="display:flex; align-items:center; gap: 20px;">
          <div style="flex:1;">
            <p>请妥善保管您的专属链接，使用手机扫描右侧二维码，即可进入您的专属班级空间，查看本班作业及考勤等信息，并直接提交您的解答作业。</p>
          </div>
          <div v-if="qrCodeUrl" style="border:1px solid #eee; padding:5px; border-radius:8px; background:white;">
            <img :src="qrCodeUrl" alt="二维码" style="width:120px;height:120px;display:block;" />
          </div>
        </div>
      </div>
      
      <div class="voucher-footer">
        <p>生成时间: {{ new Date().toLocaleString() }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import api from '../api'

const route = useRoute()
const studentId = Number(route.params.id)
const student = ref(null)
const classes = ref([])
const schedule = ref([])
const classLoading = ref(false)
const scheduleLoading = ref(false)
const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const h5BaseUrl = ref(window.location.origin)
const qrCodeUrl = ref('')

const selectedDate = ref(new Date().toISOString().split('T')[0])

// 打印专用变量
const printDialogVisible = ref(false)
const printClassId = ref(null)
const activeClasses = computed(() => classes.value.filter(c => c.status === 1 && c.classStatus === 1))
const selectedPrintClassData = computed(() => activeClasses.value.find(c => c.classId === printClassId.value))
const printSchedule = computed(() => {
  if (!printClassId.value) return []
  return schedule.value.filter(c => c.classId === printClassId.value)
})

const generateVoucherShareLink = computed(() => {
  if (!student.value || !printClassId.value) return ''
  return `${h5BaseUrl.value}/h5/homework?token=${student.value.accessToken}&classId=${printClassId.value}`
})

watch(generateVoucherShareLink, async (newVal) => {
  if (newVal) {
    try {
      qrCodeUrl.value = await QRCode.toDataURL(newVal, { width: 150, margin: 1, color: { dark: '#111111', light: '#ffffff' } })
    } catch (err) {
      console.error(err)
    }
  } else {
    qrCodeUrl.value = ''
  }
})

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const getScheduleByDay = (day) => {
  return schedule.value.filter(c => {
    // 1. 匹配星期
    if (c.dayOfWeek !== day) return false
    
    // 2. 匹配选择日期的有效性 (如果有班级起止时间，必须在周期内)
    // 根据所选日期推算出“day”（1-7）对应那天的实际日期字符串 (YYYY-MM-DD)
    const dateObj = new Date(selectedDate.value)
    let selectedDayOfWeek = dateObj.getDay() || 7 // 1-7 (周一直到周日)
    
    // 计算所在周的星期一的日期
    const diffToMonday = selectedDayOfWeek - 1
    const monday = new Date(dateObj.getTime() - diffToMonday * 24 * 60 * 60 * 1000)
    
    // 计算当前遍历到的那个格子 (day) 的实际日期
    const currentGridDateObj = new Date(monday.getTime() + (day - 1) * 24 * 60 * 60 * 1000)
    const currentGridDateStr = currentGridDateObj.toISOString().split('T')[0]
    
    // 3. 判断这个格子本身的日期有没有超过班级定义的周期
    if (c.startDate && c.endDate) {
      if (currentGridDateStr < c.startDate || currentGridDateStr > c.endDate) {
        return false
      }
    }

    return true
  })
}

const loadData = async () => {
  try {
    const configRes = await api.getConfigH5Url()
    if (configRes.data) {
      let url = configRes.data
      if (url.includes('localhost') || url.includes('127.0.0.1')) {
        url = url.replace(/localhost|127\.0\.0\.1/, window.location.hostname)
      }
      h5BaseUrl.value = url
    }
  } catch(e) { console.error('Failed to get h5 url config', e) }

  const res = await api.getStudentById(studentId)
  student.value = res.data

  classLoading.value = true
  try {
    const cRes = await api.getStudentClasses(studentId)
    classes.value = cRes.data || []
  } finally {
    classLoading.value = false
  }

  scheduleLoading.value = true
  try {
    const sRes = await api.getStudentSchedule(studentId)
    schedule.value = sRes.data || []
  } finally {
    scheduleLoading.value = false
  }
}

const copyHomeworkLink = (classId) => {
  if (!student.value || !student.value.accessToken) {
    ElMessage.error('无法获取学生Token信息')
    return
  }
  const link = `${h5BaseUrl.value}/h5/homework?token=${student.value.accessToken}&classId=${classId}`
  
  if (navigator.clipboard) {
    navigator.clipboard.writeText(link).then(() => {
      ElMessage.success('已成功复制该学生的专属班级空间链接！')
    }).catch(() => {
      fallbackCopy(link)
    })
  } else {
    fallbackCopy(link)
  }
}

const fallbackCopy = (text) => {
  const textArea = document.createElement("textarea")
  textArea.value = text
  textArea.style.position = "fixed"
  document.body.appendChild(textArea)
  textArea.focus()
  textArea.select()
  try {
    document.execCommand('copy')
    ElMessage.success('已成功复制该学生的专属班级空间链接！')
  } catch (err) {
    ElMessage.error('复制失败，请手动组合链接')
  }
  document.body.removeChild(textArea)
}

const handleLeave = (classId) => {
  ElMessageBox.confirm('确定将该学生退出此班级吗？', '提示', { type: 'warning' }).then(async () => {
    await api.leaveClass({ studentId, classId })
    ElMessage.success('出班成功')
    loadData()
  }).catch(() => {})
}

const openPrintDialog = () => {
  if (activeClasses.value.length === 0) {
    ElMessage.warning('该学生当前没有处于进行中的班级，无法打印入班凭证')
    return
  }
  printClassId.value = activeClasses.value[0].classId // 默认选中第一个
  printDialogVisible.value = true
}

const executePrint = () => {
  if (!printClassId.value) {
    ElMessage.warning('请选择要打印的班级')
    return
  }
  printDialogVisible.value = false
  
  // 等待DOM通过Vue响应式更新完凭证模板的数据
  setTimeout(() => {
    const printContent = document.querySelector('.print-voucher-container').innerHTML
    
    // 创建一个隐藏的 iframe 作为干净的打印沙盒
    const iframe = document.createElement('iframe')
    iframe.style.display = 'none'
    document.body.appendChild(iframe)
    
    const doc = iframe.contentWindow.document
    doc.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>入班凭证</title>
          <style>
            body { font-family: 'Helvetica Neue', Arial, sans-serif; padding: 20px; color: #000; background: #fff; line-height: 1.5; }
            .voucher-header { text-align: center; border-bottom: 2px dashed #bbb; padding-bottom: 25px; margin-bottom: 30px; }
            .brand-title { font-size: 36px; color: #333; margin: 0 0 12px; font-weight: 800; letter-spacing: 4px; }
            .voucher-header h2 { font-size: 22px; margin: 0; color: #555; font-weight: normal; letter-spacing: 2px; }
            .voucher-header p { color: #666; margin: 0; font-size: 14px; }
            .voucher-section { margin-bottom: 30px; }
            .section-title { font-size: 18px; margin-bottom: 15px; border-left: 4px solid #6366f1; padding-left: 10px; font-weight: bold; }
            .info-row { display: flex; justify-content: space-between; flex-wrap: wrap; background: #f8f9fa; padding: 15px 20px; border-radius: 8px; border: 1px solid #ddd; }
            .info-row span { font-size: 15px; min-width: 200px; margin-bottom: 8px; }
            .voucher-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
            .voucher-table th, .voucher-table td { border: 1px solid #ccc; padding: 10px; text-align: center; font-size: 14px; }
            .voucher-table th { background-color: #f1f1f1; font-weight: bold; }
            .hw-link-box { background: #fdfdfd; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; margin-top: 10px; }
            .hw-link-box p { color: #555; margin: 0 0 10px 0; font-size: 14px; }
            .voucher-footer { margin-top: 50px; text-align: right; font-size: 13px; color: #888; border-top: 1px dashed #ccc; padding-top: 10px; }
          </style>
        </head>
        <body>
          ${printContent}
        </body>
      </html>
    `)
    doc.close()
    
    // 渲染沙盒完毕后，拉起浏览器系统的原生打印机
    setTimeout(() => {
      iframe.contentWindow.focus()
      iframe.contentWindow.print()
      // 打印对话框关闭后清理掉生成的 iframe DOM
      setTimeout(() => {
        document.body.removeChild(iframe)
      }, 1000)
    }, 200)

  }, 100)
}

onMounted(loadData)
</script>

<style scoped>
.info-card .info-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.student-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #a78bfa);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: white;
}

.info-header h2 {
  font-size: 22px;
  color: var(--text-primary);
}

.student-no {
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 2px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-item span {
  font-size: 15px;
  color: var(--text-primary);
}

.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.week-header {
  text-align: center;
  padding: 10px 0;
  font-weight: 600;
  font-size: 13px;
  color: var(--primary-light);
  background: rgba(99, 102, 241, 0.08);
  border-radius: 8px 8px 0 0;
}

.week-body {
  min-height: 100px;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.week-course-card {
  padding: 8px 10px;
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 8px;
}

.wc-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.wc-time {
  font-size: 11px;
  color: var(--primary-light);
}

.wc-teacher, .wc-location {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.wc-class {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px dashed var(--border-color);
}

.week-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
  padding: 20px 0;
}

/* --- 打印容器隐藏 --- */
.print-voucher-container {
  display: none;
}
</style>
