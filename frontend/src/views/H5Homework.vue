<template>
  <div class="h5-container">
    <div class="h5-header">
      <div class="brand">{{ className ? `${className} - 班级空间` : '享学未来 班级空间' }}</div>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !initialized" class="loading-wrap">
      加载中...
    </div>

    <!-- Error State -->
    <div v-else-if="errorMsg" class="error-wrap">
      {{ errorMsg }}
    </div>

    <!-- 班级空间主体 -->
    <div class="h5-content" v-else>
      <!-- Tab Header -->
      <div class="tab-bar">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          :class="['tab-item', { active: activeTab === tab.key }]"
          @click="switchTab(tab.key)"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
        </div>
      </div>

      <!-- ========== TAB 1: 课程表 ========== -->
      <div v-show="activeTab === 'schedule'" class="tab-content">
        <div v-if="scheduleLoading" class="section-loading">加载中...</div>
        <div v-else-if="scheduleList.length === 0" class="empty-state">
          📅 暂无课程排期
        </div>
        <div v-else>
          <div v-for="day in scheduleDays" :key="day" class="schedule-day-block">
            <div class="day-header">{{ dayNames[day - 1] }}</div>
            <div v-for="course in getScheduleByDay(day)" :key="course.id" class="schedule-card">
              <div class="sc-time">🕐 {{ course.startTime }} - {{ course.endTime }}</div>
              <div class="sc-detail" v-if="course.teacherName">👨‍🏫 {{ course.teacherName }}</div>
              <div class="sc-detail" v-if="course.location">📍 {{ course.location }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB 2: 签到 ========== -->
      <div v-show="activeTab === 'attendance'" class="tab-content">
        <div v-if="attendanceLoading" class="section-loading">加载中...</div>
        <div v-else>
          <!-- 汇总卡片 -->
          <div class="att-summary">
            <div class="att-stat">
              <div class="att-num present">{{ attendance.totalPresent }}</div>
              <div class="att-label">出勤</div>
            </div>
            <div class="att-stat">
              <div class="att-num late">{{ attendance.totalLate }}</div>
              <div class="att-label">迟到</div>
            </div>
            <div class="att-stat">
              <div class="att-num leave">{{ attendance.totalLeave }}</div>
              <div class="att-label">请假</div>
            </div>
            <div class="att-stat">
              <div class="att-num absent">{{ attendance.totalAbsent }}</div>
              <div class="att-label">缺勤</div>
            </div>
          </div>

          <!-- 签到明细 -->
          <div v-if="attendance.records && attendance.records.length === 0" class="empty-state">
            📋 暂无签到记录
          </div>
          <div v-else class="att-records">
            <div
              v-for="(rec, i) in attendance.records"
              :key="i"
              class="att-record-item"
            >
              <span class="att-date">{{ rec.recordDate }}</span>
              <span :class="['att-status-badge', 'att-s-' + rec.status]">
                {{ attendanceStatusText(rec.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB 3: 作业 ========== -->
      <div v-show="activeTab === 'homework'" class="tab-content">
        <div v-if="!currentHomeworkId">
          <!-- List View -->
          <div v-if="homeworkList.length === 0 && !homeworkLoading" class="empty-state">
             📝 暂无布置的作业
          </div>
          <div v-if="homeworkLoading" class="section-loading">加载中...</div>
          
          <div class="hw-list-card" 
               v-for="hw in homeworkList" 
               :key="hw.id" 
               @click="openDetail(hw.id)">
            <div class="hw-head">
              <span class="hw-title">{{ hw.title }}</span>
              <span :class="['status-badge', 'status-' + hw.status]">
                {{ statusText(hw.status) }}
              </span>
            </div>
            <div class="hw-bot">
               截止: {{ formatTime(hw.deadline) }}
            </div>
          </div>
        </div>

        <!-- Detail View -->
        <div v-else>
          <button class="btn-back" @click="currentHomeworkId = null">
            ← 返回列表
          </button>

          <!-- Title Card -->
          <div class="hw-card title-card">
            <h2>{{ detailHw.title }}</h2>
            <div class="hw-meta">
              <span>截止：{{ formatTime(detailHw.deadline) }}</span>
            </div>
            <p class="hw-desc" v-if="detailHw.description">{{ detailHw.description }}</p>
            
            <div class="teacher-attachments" v-if="teacherAtts.length">
              <h4>老师附件/题板：</h4>
              <div class="img-grid">
                <template v-for="(file, i) in teacherAtts" :key="i">
                  <el-image 
                    v-if="isImage(file)"
                    class="hw-img" 
                    :src="file" 
                    :preview-src-list="teacherAtts.filter(isImage)" 
                  />
                  <a v-else :href="file" target="_blank" class="hw-file">
                    <span class="file-icon">📄</span>
                    <span class="file-name">{{ getFileName(file) }}</span>
                  </a>
                </template>
              </div>
            </div>
          </div>

          <!-- Answer Attachments Card -->
          <div class="hw-card title-card" v-if="detailHw.isAnswerPublished === 1 && answerAtts.length">
            <h3 style="margin-top:0;font-size:18px;color:#111;">💡 老师公布的答案</h3>
            <div class="teacher-attachments" style="margin-top:10px;">
              <div class="img-grid">
                <template v-for="(file, i) in answerAtts" :key="i">
                  <el-image 
                    v-if="isImage(file)"
                    class="hw-img" 
                    :src="file" 
                    :preview-src-list="answerAtts.filter(isImage)" 
                  />
                  <a v-else :href="file" target="_blank" class="hw-file">
                    <span class="file-icon">📄</span>
                    <span class="file-name">{{ getFileName(file) }}</span>
                  </a>
                </template>
              </div>
            </div>
          </div>

          <!-- Student Submit Card -->
          <div class="hw-card submit-card">
            <div class="card-header-flex">
              <h3>我的提交</h3>
              <span :class="['status-badge', 'status-' + stdHw.status]">
                {{ statusText(stdHw.status) }}
              </span>
            </div>
            
            <div class="submit-body">
              <p class="help-text" v-if="stdHw.status === 0 || stdHw.status === 2">
                 请拍照并上传您的解答（支持多图）。
              </p>

              <el-upload
                v-if="stdHw.status === 0 || stdHw.status === 2"
                action="/api/common/upload"
                list-type="picture-card"
                :on-success="handleUploadSuccess"
                :on-remove="handleUploadRemove"
                :file-list="studentFileList"
                multiple
              >
                <el-icon><i class="el-icon-plus"></i>＋</el-icon>
              </el-upload>
              
              <div v-else class="img-grid">
                <template v-for="(img, i) in studentUploadedFiles" :key="i">
                  <el-image 
                      v-if="isImage(img)"
                      class="hw-img" 
                      :src="img" 
                      :preview-src-list="studentUploadedFiles.filter(isImage)" 
                    />
                  <a v-else :href="img" target="_blank" class="hw-file">
                    <span class="file-icon">📄</span>
                    <span class="file-name">{{ getFileName(img) }}</span>
                  </a>
                </template>
              </div>

              <button v-if="stdHw.status === 0 || stdHw.status === 2" class="btn-modern btn-submit" @click="executeSubmit">
                {{ stdHw.status === 2 ? '重新提交验证' : '确认交作业' }}
              </button>
            </div>
          </div>

          <!-- Teacher Feedback Card -->
          <div class="hw-card feedback-card" v-if="stdHw.status === 2 || stdHw.status === 3">
            <h3>👩‍🏫 老师批改与反馈</h3>
            <p class="feedback-text" v-if="stdHw.teacherComment">{{ stdHw.teacherComment }}</p>
            <p v-else class="feedback-text text-muted">老师未填写文字评语</p>
            
            <div class="teacher-attachments" v-if="evalAtts.length">
              <div class="img-grid">
                <template v-for="(file, i) in evalAtts" :key="i">
                  <el-image 
                      v-if="isImage(file)"
                      class="hw-img" 
                      :src="file" 
                      :preview-src-list="evalAtts.filter(isImage)" 
                    />
                  <a v-else :href="file" target="_blank" class="hw-file">
                    <span class="file-icon">📄</span>
                    <span class="file-name">{{ getFileName(file) }}</span>
                  </a>
                </template>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const token = route.query.token
const classId = route.query.classId

const loading = ref(true)
const initialized = ref(false)
const errorMsg = ref('')

const className = ref('')
const studentName = ref('')

// Tab state
const activeTab = ref('schedule')
const tabs = [
  { key: 'schedule', icon: '📅', label: '课程表' },
  { key: 'attendance', icon: '📋', label: '签到' },
  { key: 'homework', icon: '📝', label: '作业' },
]
const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 课程表
const scheduleLoading = ref(false)
const scheduleList = ref([])
const scheduleDays = computed(() => {
  const days = [...new Set(scheduleList.value.map(c => c.dayOfWeek))]
  return days.sort((a, b) => a - b)
})
const getScheduleByDay = (day) => scheduleList.value.filter(c => c.dayOfWeek === day)

// 签到
const attendanceLoading = ref(false)
const attendance = ref({ records: [], totalPresent: 0, totalLate: 0, totalLeave: 0, totalAbsent: 0, totalCount: 0 })

const attendanceStatusText = (status) => {
  const map = { 1: '出勤', 2: '迟到', 3: '请假', 4: '缺勤' }
  return map[status] || '未记录'
}

// 作业
const homeworkLoading = ref(false)
const homeworkList = ref([])
const currentHomeworkId = ref(null)
const detailHw = ref({})
const stdHw = ref({})
const studentUploadedFiles = ref([])
const studentFileList = ref([])

const teacherAtts = computed(() => {
  try { return JSON.parse(detailHw.value.attachments || '[]') } catch (e) { return [] }
})
const evalAtts = computed(() => {
  try { return JSON.parse(stdHw.value.teacherFeedbackAttachments || '[]') } catch (e) { return [] }
})
const answerAtts = computed(() => {
  try { return JSON.parse(detailHw.value.answerAttachments || '[]') } catch (e) { return [] }
})

const isImage = (url) => {
  if (!url) return false
  const ext = url.split('.').pop().toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)
}

const getFileName = (url) => {
  if (!url) return '未知附件'
  const s = String(url)
  if (s.includes('name=')) {
    const params = new URLSearchParams(s.split('?')[1])
    const name = params.get('name')
    if (name) return name
  }
  const base = s.includes('?') ? s.split('?')[0] : s
  const parts = base.split('/')
  return parts[parts.length - 1]
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const statusText = (status) => {
  const map = { 0: '待提交', 1: '审阅中', 2: '被打回', 3: '由老师通过' }
  return map[status] || '未知'
}

// =================== Data Loading ===================

const switchTab = (key) => {
  activeTab.value = key
  if (key === 'schedule' && scheduleList.value.length === 0 && !scheduleLoading.value) {
    loadSchedule()
  } else if (key === 'attendance' && attendance.value.totalCount === 0 && !attendanceLoading.value) {
    loadAttendance()
  } else if (key === 'homework' && homeworkList.value.length === 0 && !homeworkLoading.value) {
    loadHomeworkList()
  }
}

const loadSchedule = async () => {
  scheduleLoading.value = true
  try {
    const res = await api.getH5Schedule(token, classId)
    scheduleList.value = res.data || []
  } catch (e) {
    console.error('Failed to load schedule', e)
  } finally {
    scheduleLoading.value = false
  }
}

const loadAttendance = async () => {
  attendanceLoading.value = true
  try {
    const res = await api.getH5Attendance(token, classId)
    attendance.value = res.data || { records: [], totalPresent: 0, totalLate: 0, totalLeave: 0, totalAbsent: 0, totalCount: 0 }
  } catch (e) {
    console.error('Failed to load attendance', e)
  } finally {
    attendanceLoading.value = false
  }
}

const loadHomeworkList = async () => {
  homeworkLoading.value = true
  try {
    const res = await api.getH5HomeworkList(token, classId)
    className.value = res.data.className
    studentName.value = res.data.studentName
    homeworkList.value = res.data.list
  } catch (e) {
    console.error('Failed to load homework', e)
  } finally {
    homeworkLoading.value = false
  }
}

const initSpace = async () => {
  if (!token || !classId) {
    errorMsg.value = '无效的访问链接：缺少安全参数'
    loading.value = false
    return
  }
  
  try {
    // 先加载作业列表以获取 className
    const res = await api.getH5HomeworkList(token, classId)
    className.value = res.data.className
    studentName.value = res.data.studentName
    homeworkList.value = res.data.list
    
    initialized.value = true
    // 默认进入课程表 tab，加载课程表数据
    loadSchedule()
  } catch (e) {
    errorMsg.value = '获取班级信息失败'
  } finally {
    loading.value = false
  }
}

const openDetail = async (id) => {
  try {
    loading.value = true
    const res = await api.getH5HomeworkDetail(id, token)
    detailHw.value = res.data.homework
    stdHw.value = res.data.studentHomework
    
    studentUploadedFiles.value = JSON.parse(stdHw.value.submitAttachments || '[]')
    studentFileList.value = studentUploadedFiles.value.map(url => ({ name: 'img', url }))
    
    currentHomeworkId.value = id
  } catch (e) {
    ElMessage.error('获取详细信息失败')
  } finally {
    loading.value = false
  }
}

const handleUploadSuccess = (res, file, fileList) => {
  if (res.code === 200) {
    studentUploadedFiles.value.push(res.data)
  }
}

const handleUploadRemove = (file, fileList) => {
  const url = file.response ? file.response.data : file.url
  studentUploadedFiles.value = studentUploadedFiles.value.filter(u => u !== url)
}

const executeSubmit = async () => {
  if (studentUploadedFiles.value.length === 0) {
    ElMessage.warning('请至少上传一张清晰的作业图片解答！')
    return
  }
  await api.submitH5Homework({
    id: stdHw.value.id,
    submitAttachments: JSON.stringify(studentUploadedFiles.value)
  }, token)
  ElMessage.success('提交作业成功！')
  openDetail(currentHomeworkId.value)
}

onMounted(() => {
  initSpace()
})

</script>

<style scoped>
.h5-container {
  min-height: 100vh;
  background-color: #f4f6fb;
  font-family: 'Helvetica Neue', Arial, sans-serif;
  padding-bottom: 40px;
}

.h5-header {
  height: 60px;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  display: flex;
  align-items: center;
  padding: 0 20px;
  justify-content: center;
}

.brand {
  font-size: 16px;
  font-weight: 800;
  background: linear-gradient(135deg, #6366f1, #a78bfa);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.loading-wrap, .error-wrap {
  text-align: center;
  padding-top: 50px;
  color: #666;
}
.error-wrap { color: #ef4444; }

.h5-content {
  padding: 0 15px 15px;
  max-width: 500px;
  margin: 0 auto;
}

/* ============ Tab Bar ============ */
.tab-bar {
  display: flex;
  background: white;
  border-radius: 16px;
  margin: 15px 0;
  padding: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  gap: 4px;
}

.tab-item.active {
  background: linear-gradient(135deg, #6366f1, #818cf8);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.tab-icon {
  font-size: 20px;
}

.tab-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

.tab-item.active .tab-label {
  color: white;
}

.tab-content {
  min-height: 200px;
}

.section-loading {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 50px 0;
  background: white;
  border-radius: 16px;
  color: #999;
  font-size: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

/* ============ 课程表 Tab ============ */
.schedule-day-block {
  background: white;
  border-radius: 16px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.day-header {
  padding: 10px 16px;
  font-weight: 700;
  font-size: 15px;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.06);
  border-bottom: 1px solid rgba(99, 102, 241, 0.08);
}

.schedule-card {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.schedule-card:last-child {
  border-bottom: none;
}

.sc-time {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.sc-detail {
  font-size: 13px;
  color: #777;
  margin-top: 3px;
}

/* ============ 签到 Tab ============ */
.att-summary {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.att-stat {
  flex: 1;
  background: white;
  border-radius: 14px;
  padding: 14px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.att-num {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 4px;
}

.att-num.present { color: #22c55e; }
.att-num.late { color: #f59e0b; }
.att-num.leave { color: #6366f1; }
.att-num.absent { color: #ef4444; }

.att-label {
  font-size: 12px;
  color: #999;
  font-weight: 500;
}

.att-records {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.att-record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.att-record-item:last-child {
  border-bottom: none;
}

.att-date {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.att-status-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.att-s-1 { background: #dcfce7; color: #16a34a; }
.att-s-2 { background: #fef3c7; color: #d97706; }
.att-s-3 { background: #ede9fe; color: #7c3aed; }
.att-s-4 { background: #fee2e2; color: #dc2626; }
.att-s-null { background: #f3f4f6; color: #9ca3af; }

/* ============ 作业 Tab ============ */
.hw-list-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  cursor: pointer;
  transition: transform 0.2s;
}
.hw-list-card:active { transform: scale(0.98); }

.hw-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hw-title {
  font-weight: 600;
  font-size: 16px;
  color: #222;
}

.hw-bot {
  font-size: 13px;
  color: #888;
}

.btn-back {
  background: none;
  border: none;
  color: #6366f1;
  font-weight: 600;
  margin-bottom: 15px;
  cursor: pointer;
  padding: 0;
  font-size: 15px;
}

.hw-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}

.title-card h2 {
  font-size: 20px;
  margin: 0 0 10px;
  color: #111;
}

.hw-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
  margin-bottom: 15px;
  align-items: center;
}

.hw-desc {
  font-size: 15px;
  color: #444;
  line-height: 1.6;
  padding: 15px;
  background: #f8fafc;
  border-radius: 10px;
  margin-bottom: 15px;
}

.teacher-attachments h4 {
  font-size: 14px;
  margin-bottom: 10px;
  color: #333;
}

.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hw-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #eee;
  object-fit: cover;
}

.hw-file {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #eee;
  background: #f8fafc;
  text-decoration: none;
  color: #333;
}
.hw-file .file-icon {
  font-size: 24px;
  margin-bottom: 5px;
}
.hw-file .file-name {
  font-size: 10px;
  color: #6366f1;
  text-align: center;
  word-break: break-all;
  padding: 0 4px;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.card-header-flex h3 { margin: 0; font-size: 18px; color: #333; }

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-0, .status-2 { background: #fee2e2; color: #ef4444; }
.status-1 { background: #fef3c7; color: #f59e0b; }
.status-3 { background: #dcfce3; color: #22c55e; }

.help-text {
  font-size: 13px;
  color: #555;
  margin-bottom: 15px;
}

.btn-modern {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.btn-submit {
  margin-top: 20px;
}

.feedback-card {
  border: 2px dashed rgba(34, 197, 94, 0.2);
}

.feedback-card h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #166534;
}

.feedback-text {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  padding: 15px;
  background: #f0fdf4;
  border-radius: 10px;
  margin-bottom: 15px;
}

.text-muted {
  color: #999 !important;
  font-style: italic;
  background: #f8f9fa !important;
}
</style>
