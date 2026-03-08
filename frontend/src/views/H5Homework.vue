<template>
  <div class="h5-container">
    <div class="h5-header">
      <div class="brand">{{ className ? `${className} - 作业` : '享学未来 在线作业' }}</div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-wrap">
      加载中...
    </div>

    <!-- Error State -->
    <div v-else-if="errorMsg" class="error-wrap">
      {{ errorMsg }}
    </div>

    <!-- Homework Portal -->
    <div class="h5-content" v-else>
      <div v-if="!currentHomeworkId">
        <!-- List View -->
        <h3 class="welcome-title">同学，你好 👋</h3>
        <p class="welcome-sub" v-if="homeworkList.length > 0">这里是你本班级的作业列表</p>
        <div v-if="homeworkList.length === 0" class="empty-state">
           暂无布置的作业
        </div>
        
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
const errorMsg = ref('')

const className = ref('')
const studentName = ref('')
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

const isImage = (url) => {
  if (!url) return false
  const ext = url.split('.').pop().toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)
}

const getFileName = (url) => {
  if (!url) return '未知附件'
  const s = String(url)
  // 检查 URL 中是否包含 name= 参数（新方案）
  if (s.includes('name=')) {
    const params = new URLSearchParams(s.split('?')[1])
    const name = params.get('name')
    if (name) return name
  }
  // 兼容旧方案：截取最后一段
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

const loadList = async () => {
  if (!token || !classId) {
    errorMsg.value = '无效的访问链接：缺少安全参数'
    loading.value = false
    return
  }
  
  try {
    const res = await api.getH5HomeworkList(token, classId)
    className.value = res.data.className
    studentName.value = res.data.studentName
    homeworkList.value = res.data.list
  } catch (e) {
    errorMsg.value = '获取作业列表失败'
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
  loadList()
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
  -webkit-text-fill-color: transparent;
}

.loading-wrap, .error-wrap {
  text-align: center;
  padding-top: 50px;
  color: #666;
}
.error-wrap { color: #ef4444; }

.h5-content {
  padding: 15px;
  max-width: 500px;
  margin: 0 auto;
}

.welcome-title {
  margin: 10px 0 5px;
  color: #111;
  font-size: 22px;
}
.welcome-sub {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  background: white;
  border-radius: 12px;
  color: #999;
}

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
