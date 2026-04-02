<template>
  <div class="homework-list">
    <div class="page-header">
      <div class="header-content">
        <h2>📚 班级作业管理</h2>
        <p>支持发布作业，生成外部收集链接并批改。</p>
      </div>
      <div style="display:flex; gap: 16px; align-items:center;">
        <el-select v-model="filterClassId" placeholder="按班级筛选" clearable @change="handleFilterChange" style="width:200px">
          <el-option v-for="c in classes" :key="c.id" :label="`${c.classCode} - ${c.className}`" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="openCreateDialog" class="create-btn">
          + 发布新作业
        </el-button>
      </div>
    </div>

    <div class="page-card">
      <el-table :data="homeworkList" v-loading="loading">
        <el-table-column prop="title" label="作业标题" min-width="150" />
        <el-table-column prop="className" label="所属班级" min-width="150" />
        <el-table-column label="截止时间" width="180">
          <template #default="{ row }">{{ formatTime(row.deadline) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="公布答案" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.isAnswerPublished" :active-value="1" :inactive-value="0" @change="toggleAnswerStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">查看/编辑</el-button>
            <el-button link type="success" @click="openReviewDialog(row)">学生批改</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </div>

    <!-- 发布作业弹窗 -->
    <el-dialog v-model="createDialog" :title="isEdit ? '查看/编辑作业' : '发布新作业'" width="600px" @closed="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属班级" required>
          <el-select v-model="form.classId" style="width:100%" placeholder="选择班级">
            <el-option v-for="c in classes" :key="c.id" :label="`${c.classCode} - ${c.className}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题" required>
          <el-input v-model="form.title" placeholder="如：第三周课后练习" />
        </el-form-item>
        <el-form-item label="作业描述">
          <el-input type="textarea" v-model="form.description" rows="3" placeholder="请输入作业要求" />
        </el-form-item>
        <el-form-item label="截止要求">
          <el-date-picker v-model="form.deadline" type="datetime" placeholder="选择截止时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="布置附件">
          <el-upload
            action="/api/common/upload"
            :headers="headers"
            list-type="picture-card"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            :file-list="fileList"
            multiple
          >
            <el-icon><i class="el-icon-plus"></i>上传</el-icon>
            <template #file="{ file }">
              <div style="width:100%; height:100%; border-radius:6px; overflow:hidden;">
                <img class="el-upload-list__item-thumbnail" v-if="isImage(file.response ? file.response.data : file.url)" :src="file.response ? file.response.data : file.url" style="width:100%; height:100%; object-fit:cover;" />
                <div v-else style="width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; background:#f4f5f7;">
                  <span style="font-size:16px; font-weight:bold; color:#606266;">PDF文档</span>
                  <span style="font-size:12px; color:#909399; text-align:center; padding:0 5px; word-break:break-all;" class="ellipsis">{{ getFileName(file.response ? file.response.data : file.url) }}</span>
                </div>
                <span class="el-upload-list__item-actions">
                  <span class="el-upload-list__item-preview" style="cursor:pointer;" @click="openLink(file.response ? file.response.data : file.url)">
                    查看
                  </span>
                  <span class="el-upload-list__item-delete" style="cursor:pointer; margin-left:10px;" @click="handleVisualRemove(file)">
                    移除
                  </span>
                </span>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="答案附件">
          <el-upload
            action="/api/common/upload"
            :headers="headers"
            list-type="picture-card"
            :on-success="handleAnswerUploadSuccess"
            :on-remove="handleAnswerUploadRemove"
            :file-list="answerFileList"
            multiple
          >
            <el-icon><i class="el-icon-plus"></i>上传</el-icon>
            <template #file="{ file }">
              <div style="width:100%; height:100%; border-radius:6px; overflow:hidden;">
                <img class="el-upload-list__item-thumbnail" v-if="isImage(file.response ? file.response.data : file.url)" :src="file.response ? file.response.data : file.url" style="width:100%; height:100%; object-fit:cover;" />
                <div v-else style="width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; background:#f4f5f7;">
                  <span style="font-size:16px; font-weight:bold; color:#606266;">PDF文档</span>
                  <span style="font-size:12px; color:#909399; text-align:center; padding:0 5px; word-break:break-all;" class="ellipsis">{{ getFileName(file.response ? file.response.data : file.url) }}</span>
                </div>
                <span class="el-upload-list__item-actions">
                  <span class="el-upload-list__item-preview" style="cursor:pointer;" @click="openLink(file.response ? file.response.data : file.url)">
                    查看
                  </span>
                  <span class="el-upload-list__item-delete" style="cursor:pointer; margin-left:10px;" @click="handleAnswerVisualRemove(file)">
                    移除
                  </span>
                </span>
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="executeSave">确认保存</el-button>
      </template>
    </el-dialog>

    <!-- 批改作业弹窗 -->
    <el-dialog v-model="reviewDialog" :title="`批改 - ${currentHomework.title}`" width="80%" top="5vh">
      <el-table :data="studentList" height="400">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column label="提交状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status===0" type="info">待提交</el-tag>
            <el-tag v-else-if="row.status===1" type="warning">已提交</el-tag>
            <el-tag v-else-if="row.status===2" type="danger">待修正</el-tag>
            <el-tag v-else type="success">通过</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交附件（学生上传）">
          <template #default="{ row }">
            <div style="display:flex;gap:4px;flex-wrap:wrap" v-if="row.submitAttachments">
              <template v-for="(file, i) in JSON.parse(row.submitAttachments || '[]')" :key="i">
                <el-image 
                  v-if="isImage(file)"
                  style="width: 50px; height: 50px; border-radius: 4px; border: 1px solid #eee;" 
                  :src="file" 
                  :preview-src-list="JSON.parse(row.submitAttachments || '[]').filter(isImage)" 
                />
                <a v-else :href="file" target="_blank" style="display:inline-flex;flex-direction:column;align-items:center;justify-content:center;width:50px;height:50px;border-radius:4px;background:#f4f5f7;text-decoration:none;border:1px solid #eee;" title="查看文件">
                  <span style="font-size:16px;">📄</span>
                </a>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
           <template #default="{ row }">
             <el-button type="primary" size="small" @click="openEvaluateDialog(row)" :disabled="row.status===0">批改</el-button>
           </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 具体的批改打回/通过弹窗 -->
    <el-dialog v-model="evalDialog" title="学生作业批改" width="550px">
      <el-form :model="evalForm" label-width="80px">
        <el-form-item label="学生作业" v-if="currentEvaluateStudentHwFiles && currentEvaluateStudentHwFiles.length > 0">
          <div style="display:flex;gap:4px;flex-wrap:wrap">
            <template v-for="(file, i) in currentEvaluateStudentHwFiles" :key="i">
              <div v-if="isImage(file)" style="position:relative; width:80px; height:80px; border-radius: 4px; border: 1px solid #eee; overflow:hidden;" title="点击进行图上批改并上传">
                <el-image style="width: 100%; height: 100%; object-fit: cover;" :src="file" :preview-src-list="[file]" />
                <div style="position:absolute; bottom:0; padding:2px; background:rgba(0,0,0,0.6); width:100%; text-align:center; color:white; font-size:12px; cursor:pointer;" @click="openDrawBox(file)">
                  ✏️ 批注
                </div>
              </div>
              <a v-else :href="file" target="_blank" style="display:inline-flex;flex-direction:column;align-items:center;justify-content:center;width:80px;height:80px;border-radius:4px;background:#f4f5f7;text-decoration:none;border:1px solid #eee;" title="查看文件">
                <span style="font-size:16px;">📄</span>
              </a>
            </template>
          </div>
        </el-form-item>
        <el-form-item label="批改评语">
          <el-input type="textarea" v-model="evalForm.teacherComment" rows="3" placeholder="给予学生的反馈" />
        </el-form-item>
        <el-form-item label="批改附件">
          <!-- 教师批改上传 -->
          <el-upload
            action="/api/common/upload"
            :headers="headers"
            list-type="picture-card"
            :on-success="handleEvalUploadSuccess"
            :on-remove="handleEvalUploadRemove"
            :file-list="evalFileList"
          >
            <el-icon><i class="el-icon-plus"></i>上传</el-icon>
            <template #file="{ file }">
              <div style="width:100%; height:100%; border-radius:6px; overflow:hidden;">
                <img class="el-upload-list__item-thumbnail" v-if="isImage(file.response ? file.response.data : file.url)" :src="file.response ? file.response.data : file.url" style="width:100%; height:100%; object-fit:cover;" />
                <div v-else style="width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; background:#f4f5f7;">
                  <span style="font-size:16px; font-weight:bold; color:#606266;">PDF文档</span>
                  <span style="font-size:12px; color:#909399; text-align:center; padding:0 5px; word-break:break-all;" class="ellipsis">{{ getFileName(file.response ? file.response.data : file.url) }}</span>
                </div>
                <span class="el-upload-list__item-actions">
                  <span class="el-upload-list__item-preview" style="cursor:pointer;" @click="openLink(file.response ? file.response.data : file.url)">
                    查看
                  </span>
                  <span class="el-upload-list__item-delete" style="cursor:pointer; margin-left:10px;" @click="handleEvalVisualRemove(file)">
                    移除
                  </span>
                </span>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="最终裁定">
          <el-radio-group v-model="evalForm.status">
            <el-radio :label="2">待修正打回</el-radio>
            <el-radio :label="3">通过</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalDialog = false">取消</el-button>
        <el-button type="primary" @click="executeEvaluate">确认下发</el-button>
      </template>
    </el-dialog>

    <!-- 图片批注弹窗 -->
    <el-dialog v-model="drawDialog" title="批注学生作业" width="90%" top="3vh" destroy-on-close @closed="closeDrawBox">
      <div style="display:flex; flex-direction:column; align-items:center;">
        <div style="margin-bottom: 10px; display:flex; gap: 10px; align-items: center;">
          <el-button @click="clearCanvas">清除重画</el-button>
          <el-button type="primary" @click="saveCanvas" :loading="uploadingCanvas">保存并添加至批改附件</el-button>
          <span style="font-size: 13px; color: #888;">(在下方图片直接用鼠标画线批注)</span>
        </div>
        <div style="width:100%; height: 75vh; overflow: auto; border: 1px solid #ddd; background: #e2e8f0; display: flex; justify-content: center; align-items:center;">
          <canvas 
             ref="drawCanvasRef" 
             style="max-width: 100%; max-height: 100%; cursor: crosshair; box-shadow: 0 4px 12px rgba(0,0,0,0.1);" 
             @mousedown="startDraw" 
             @mousemove="drawing" 
             @mouseup="stopDraw" 
             @mouseleave="stopDraw" 
             @touchstart="startDraw" 
             @touchmove="drawing" 
             @touchend="stopDraw"
          ></canvas>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(false)
const homeworkList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const classes = ref([])

const createDialog = ref(false)
const isEdit = ref(false)
const form = ref({ classId: '', title: '', description: '', deadline: '', attachments: '[]' })
const uploadedFiles = ref([])
const fileList = ref([])
const filterClassId = ref('')
const answerUploadedFiles = ref([])
const answerFileList = ref([])

const headers = ref({
  Authorization: `Bearer ${localStorage.getItem('cm_token')}`
})

const toggleAnswerStatus = async (row) => {
  try {
    await api.updateHomework(row)
    ElMessage.success(row.isAnswerPublished === 1 ? '已公布答案' : '已取消公布')
  } catch (e) {
    row.isAnswerPublished = row.isAnswerPublished === 1 ? 0 : 1 // 失败回滚
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadData()
}

onMounted(() => {
  loadClasses().then(() => {
    loadData()
  })
})

const reviewDialog = ref(false)
const currentHomework = ref({})
const studentList = ref([])

const evalDialog = ref(false)
const evalForm = ref({})
const evalUploadedFiles = ref([])
const evalFileList = ref([])

const currentEvaluateStudentHwFiles = ref([])

// 画布相关状态
const drawDialog = ref(false)
const drawCanvasRef = ref(null)
const uploadingCanvas = ref(false)
let drawCtx = null
let drawImgObj = null
let isDrawingState = false



const isImage = (url) => {
  if (!url) return false
  const l = String(url).toLowerCase()
  return l.endsWith('.jpg') || l.endsWith('.jpeg') || l.endsWith('.png') || l.endsWith('.gif') || l.endsWith('.webp')
}

const getFileName = (url) => {
  if (!url) return ''
  const s = String(url)
  // 检查 URL 中是否包含 name= 这里的参数（新方案）
  if (s.includes('name=')) {
    const params = new URLSearchParams(s.split('?')[1])
    const name = params.get('name')
    if (name) return name
  }
  // 兼容旧方案：截取最后一段
  const base = s.includes('?') ? s.split('?')[0] : s
  return base.substring(base.lastIndexOf('/') + 1)
}

const openLink = (url) => {
  if(url) window.open(url, '_blank')
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { current: currentPage.value, size: pageSize.value }
    if (filterClassId.value) {
      params.classId = filterClassId.value
    }
    const res = await api.getHomeworkList(params)
    homeworkList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  const res = await api.getClassList({ current: 1, size: 500 })
  classes.value = res.data.records
}

const openCreateDialog = () => {
  isEdit.value = false
  createDialog.value = true
  uploadedFiles.value = []
  fileList.value = []
  answerUploadedFiles.value = []
  answerFileList.value = []
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { ...row }
  try {
    uploadedFiles.value = JSON.parse(row.attachments || '[]')
  } catch(e) {
    uploadedFiles.value = []
  }
  fileList.value = uploadedFiles.value.map(url => ({ name: 'img', url }))

  try {
    answerUploadedFiles.value = JSON.parse(row.answerAttachments || '[]')
  } catch(e) {
    answerUploadedFiles.value = []
  }
  answerFileList.value = answerUploadedFiles.value.map(url => ({ name: 'img', url }))

  createDialog.value = true
  loadClasses()
}

const resetForm = () => {
  form.value = { classId: '', title: '', description: '', deadline: '', attachments: '[]', answerAttachments: '[]', isAnswerPublished: 0 }
  uploadedFiles.value = []
  fileList.value = []
  answerUploadedFiles.value = []
  answerFileList.value = []
}

const handleUploadSuccess = (response, file, fileList) => {
  if (response.code === 200) {
    uploadedFiles.value.push(response.data)
  }
}

const handleVisualRemove = (file) => {
  const url = file.response ? file.response.data : file.url
  uploadedFiles.value = uploadedFiles.value.filter(u => u !== url)
  fileList.value = fileList.value.filter(f => f.uid !== file.uid)
}

const handleUploadRemove = (file, fileList) => {
  const url = file.response ? file.response.data : file.url
  uploadedFiles.value = uploadedFiles.value.filter(u => u !== url)
}

const handleAnswerUploadSuccess = (response, file, fileList) => {
  if (response.code === 200) {
    answerUploadedFiles.value.push(response.data)
  }
}

const handleAnswerVisualRemove = (file) => {
  const url = file.response ? file.response.data : file.url
  answerUploadedFiles.value = answerUploadedFiles.value.filter(u => u !== url)
  answerFileList.value = answerFileList.value.filter(f => f.uid !== file.uid)
}

const handleAnswerUploadRemove = (file, fileList) => {
  const url = file.response ? file.response.data : file.url
  answerUploadedFiles.value = answerUploadedFiles.value.filter(u => u !== url)
}


const executeSave = async () => {
  if (!form.value.classId || !form.value.title) {
    ElMessage.warning('请填写班级和作业标题')
    return
  }
  form.value.attachments = JSON.stringify(uploadedFiles.value)
  form.value.answerAttachments = JSON.stringify(answerUploadedFiles.value)
  if (isEdit.value) {
    await api.updateHomework(form.value)
    ElMessage.success('修改成功')
  } else {
    await api.createHomework(form.value)
    ElMessage.success('发布成功')
  }
  createDialog.value = false
  loadData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除此次作业及其下级所有学生提交记录吗？', '提示', { type: 'warning' }).then(async () => {
    await api.deleteHomework(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const openReviewDialog = async (row) => {
  currentHomework.value = row
  const res = await api.getStudentHomeworkList(row.id)
  studentList.value = res.data
  reviewDialog.value = true
}

const openEvaluateDialog = (row) => {
  currentEvaluateStudentHwFiles.value = JSON.parse(row.submitAttachments || '[]')
  evalForm.value = { 
    id: row.id, 
    teacherComment: row.teacherComment || '', 
    status: row.status === 1 ? 3 : row.status, // 默认通过
    teacherFeedbackAttachments: row.teacherFeedbackAttachments || '[]'
  }
  
  evalUploadedFiles.value = JSON.parse(evalForm.value.teacherFeedbackAttachments || '[]')
  evalFileList.value = evalUploadedFiles.value.map(url => ({ name: 'img', url }))
  evalDialog.value = true
}

const handleEvalUploadSuccess = (res, file, fileList) => {
  if (res.code === 200) {
    evalUploadedFiles.value.push(res.data)
  }
}

const handleEvalVisualRemove = (file) => {
  const url = file.response ? file.response.data : file.url
  evalUploadedFiles.value = evalUploadedFiles.value.filter(u => u !== url)
  evalFileList.value = evalFileList.value.filter(f => f.uid !== file.uid)
}

const handleEvalUploadRemove = (file, fileList) => {
  const url = file.response ? file.response.data : file.url
  evalUploadedFiles.value = evalUploadedFiles.value.filter(u => u !== url)
}

const executeEvaluate = async () => {
  evalForm.value.teacherFeedbackAttachments = JSON.stringify(evalUploadedFiles.value)
  await api.reviewHomework(evalForm.value)
  ElMessage.success('批改完成')
  evalDialog.value = false
  // 刷新内部的列表
  const res = await api.getStudentHomeworkList(currentHomework.value.id)
  studentList.value = res.data
}

// ================= 画布批注逻辑 =================
const getEventPos = (e) => {
  const canvas = drawCanvasRef.value
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvas.width / rect.width
  const scaleY = canvas.height / rect.height

  let clientX = e.clientX
  let clientY = e.clientY
  if (e.touches && e.touches.length > 0) {
    clientX = e.touches[0].clientX
    clientY = e.touches[0].clientY
  }
  return { 
    x: (clientX - rect.left) * scaleX, 
    y: (clientY - rect.top) * scaleY 
  }
}

const openDrawBox = (url) => {
  drawDialog.value = true
  setTimeout(() => {
    const canvas = drawCanvasRef.value
    if (!canvas) return
    drawCtx = canvas.getContext('2d')
    const img = new Image()
    img.crossOrigin = 'Anonymous'
    img.onload = () => {
      canvas.width = img.width
      canvas.height = img.height
      drawCtx.drawImage(img, 0, 0, img.width, img.height)
      drawImgObj = img
    }
    img.src = url
  }, 100)
}

const closeDrawBox = () => {
  drawCanvasRef.value = null
  drawCtx = null
  drawImgObj = null
  isDrawingState = false
}

const startDraw = (e) => {
  e.preventDefault()
  isDrawingState = true
  drawCtx.beginPath()
  const pos = getEventPos(e)
  drawCtx.moveTo(pos.x, pos.y)
}

const drawing = (e) => {
  if (!isDrawingState || !drawCtx) return
  e.preventDefault()
  const pos = getEventPos(e)
  drawCtx.lineTo(pos.x, pos.y)
  drawCtx.strokeStyle = 'red'
  
  // 画笔粗细按原图尺寸动态等比放宽，避免在高分辨率图下线太细
  const canvas = drawCanvasRef.value
  drawCtx.lineWidth = Math.max(4, canvas.width / 200)

  drawCtx.lineCap = 'round'
  drawCtx.lineJoin = 'round'
  drawCtx.stroke()
}

const stopDraw = () => {
  if (isDrawingState && drawCtx) {
    drawCtx.closePath()
    isDrawingState = false
  }
}

const clearCanvas = () => {
  if (drawCtx && drawImgObj && drawCanvasRef.value) {
    drawCtx.clearRect(0, 0, drawCanvasRef.value.width, drawCanvasRef.value.height)
    drawCtx.drawImage(drawImgObj, 0, 0, drawImgObj.width, drawImgObj.height)
  }
}

const dataURLtoFile = (dataurl, filename) => {
  let arr = dataurl.split(',')
  let mime = arr[0].match(/:(.*?);/)[1]
  let bstr = atob(arr[1])
  let n = bstr.length
  let u8arr = new Uint8Array(n)
  while(n--) { u8arr[n] = bstr.charCodeAt(n) }
  return new File([u8arr], filename, {type: mime})
}

const saveCanvas = async () => {
  if (!drawCanvasRef.value) return
  uploadingCanvas.value = true
  try {
    const dataUrl = drawCanvasRef.value.toDataURL('image/jpeg', 0.8)
    const file = dataURLtoFile(dataUrl, 'annotated_' + new Date().getTime() + '.jpg')
    const res = await api.uploadFile(file)
    if (res.code === 200) {
      evalUploadedFiles.value.push(res.data)
      evalFileList.value.push({ name: '批改附件', url: res.data })
      ElMessage.success('批注图片已保存并添加到批改附件列表！')
      drawDialog.value = false
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch(e) {
    console.error(e)
    ElMessage.error('转换或上传失败，请检查图片跨域问题')
  } finally {
    uploadingCanvas.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-content h2 { font-size: 24px; color: var(--text-primary); }
.header-content p { color: var(--text-muted); font-size: 14px; margin-top: 4px; }
.page-card {
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
