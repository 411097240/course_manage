<template>
  <div class="teacher-assign">
    <div class="page-header">
      <h1 class="page-title">教师班级分配</h1>
      <el-button type="primary" @click="openCreateDialog">+ 新建教师账号</el-button>
    </div>

    <div class="assign-layout">
      <!-- 教师列表 -->
      <div class="page-card teacher-panel">
        <h3 style="margin-bottom:16px;color:var(--text-primary)">👨‍🏫 教师列表</h3>
        <div v-if="teachers.length === 0" style="text-align:center;padding:40px;color:var(--text-muted)">
          暂无教师
        </div>
        <div v-for="t in teachers" :key="t.id"
             :class="['teacher-item', { selected: selectedTeacherId === t.id }]"
             @click="selectTeacher(t)">
          <div class="t-avatar">{{ t.realName?.charAt(0) }}</div>
          <div style="flex:1;min-width:0">
            <div class="t-name">{{ t.realName }}</div>
            <div class="t-username">@{{ t.username }}</div>
          </div>
          <el-button size="small" type="warning" link title="重置密码"
                     @click.stop="handleResetPassword(t)" style="padding:4px">
            🔑
          </el-button>
        </div>
      </div>

      <!-- 已分配班级 -->
      <div class="page-card class-panel">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
          <h3 style="color:var(--text-primary)">
            {{ selectedTeacherName ? `${selectedTeacherName} 管理的班级` : '请选择教师' }}
          </h3>
          <el-button v-if="selectedTeacherId" type="primary" size="small" @click="openAssignDialog">
            + 分配班级
          </el-button>
        </div>
        <el-table v-if="selectedTeacherId" :data="assignedClasses" stripe v-loading="classLoading" style="width:100%">
          <el-table-column prop="classCode" label="班级编码" min-width="160" />
          <el-table-column prop="className" label="班级名称" min-width="180" />
          <el-table-column label="操作" min-width="120">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleRemove(row.classId)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else style="text-align:center;padding:60px;color:var(--text-muted)">
          ← 请先在左侧选择一位教师
        </div>
      </div>
    </div>

    <!-- 分配班级弹窗 -->
    <el-dialog v-model="assignDialogVisible" title="分配班级" width="460px" destroy-on-close>
      <el-select v-model="assignClassId" placeholder="请选择班级" style="width:100%" filterable>
        <el-option v-for="c in allClasses" :key="c.id" :label="`${c.classCode} - ${c.className}`" :value="c.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定分配</el-button>
      </template>
    </el-dialog>

    <!-- 新建教师弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建教师账号" width="480px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入登录用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="请输入教师真实姓名" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" placeholder="默认 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const teachers = ref([])
const selectedTeacherId = ref(null)
const selectedTeacherName = ref('')
const assignedClasses = ref([])
const classLoading = ref(false)
const allClasses = ref([])
const assignDialogVisible = ref(false)
const assignClassId = ref(null)

// 新建教师
const createDialogVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const createForm = reactive({ username: '', realName: '', password: '123456' })
const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const loadTeachers = async () => {
  const res = await api.getTeachers()
  teachers.value = res.data || []
}

const selectTeacher = async (t) => {
  selectedTeacherId.value = t.id
  selectedTeacherName.value = t.realName
  classLoading.value = true
  try {
    const res = await api.getUserClasses(t.id)
    assignedClasses.value = res.data || []
  } finally {
    classLoading.value = false
  }
}

const openAssignDialog = async () => {
  assignClassId.value = null
  const res = await api.getClassList({ current: 1, size: 999 })
  allClasses.value = res.data.records || []
  assignDialogVisible.value = true
}

const handleAssign = async () => {
  if (!assignClassId.value) {
    ElMessage.warning('请选择班级')
    return
  }
  await api.assignClass({ userId: selectedTeacherId.value, classId: assignClassId.value })
  ElMessage.success('分配成功')
  assignDialogVisible.value = false
  selectTeacher({ id: selectedTeacherId.value, realName: selectedTeacherName.value })
}

const handleRemove = (classId) => {
  ElMessageBox.confirm('确定移除该班级的管理权限吗？', '提示', { type: 'warning' }).then(async () => {
    await api.removeClass({ userId: selectedTeacherId.value, classId })
    ElMessage.success('移除成功')
    selectTeacher({ id: selectedTeacherId.value, realName: selectedTeacherName.value })
  }).catch(() => {})
}

const openCreateDialog = () => {
  createForm.username = ''
  createForm.realName = ''
  createForm.password = '123456'
  createDialogVisible.value = true
}

const handleCreate = async () => {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    await api.createTeacher(createForm)
    ElMessage.success('教师账号创建成功')
    createDialogVisible.value = false
    loadTeachers()
  } finally {
    createLoading.value = false
  }
}

const handleResetPassword = (teacher) => {
  ElMessageBox.confirm(
    `确定将 ${teacher.realName} 的密码重置为 123456 吗？`,
    '重置密码',
    { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' }
  ).then(async () => {
    await api.resetPassword({ userId: teacher.id, newPassword: '123456' })
    ElMessage.success(`${teacher.realName} 的密码已重置为 123456`)
  }).catch(() => {})
}

onMounted(loadTeachers)
</script>

<style scoped>
.assign-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
}

.teacher-panel {
  max-height: calc(100vh - 180px);
  overflow-y: auto;
}

.teacher-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.teacher-item:hover {
  background: rgba(99, 102, 241, 0.08);
}

.teacher-item.selected {
  background: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.3);
}

.t-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #22c55e, #4ade80);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
}

.t-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.t-username {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
