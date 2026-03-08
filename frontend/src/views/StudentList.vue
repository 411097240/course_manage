<template>
  <div class="student-list">
    <div class="page-header">
      <h1 class="page-title">学生管理</h1>
      <el-button type="primary" @click="openDialog()" v-if="userStore.isAdmin()">+ 新增学生</el-button>
    </div>
    <div class="page-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索姓名、学号或手机号..." clearable @keyup.enter="loadData"
                  prefix-icon="Search" />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" style="width:100%">
        <el-table-column prop="studentNo" label="学号" min-width="140" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column label="在读班级" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.activeClassNames">{{ row.activeClassNames }}</span>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="性别" min-width="80">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="150" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.status === 1 ? 'active' : 'inactive']">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/student/${row.id}`)">详情</el-button>
            <el-button size="small" @click="openDialog(row)" v-if="userStore.isAdmin()">编辑</el-button>
            <el-button size="small" type="primary" @click="openJoinDialog(row)" v-if="userStore.isAdmin()">入班</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)" v-if="userStore.isAdmin()">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       v-model:current-page="current" :page-size="size" @current-change="loadData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑学生' : '新增学生'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择性别" clearable>
            <el-option label="男" :value="1" /><el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="状态" v-if="editId">
          <el-select v-model="form.status">
            <el-option label="启用" :value="1" /><el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 入班弹窗 -->
    <el-dialog v-model="joinDialogVisible" title="学生入班" width="460px" destroy-on-close>
      <p style="margin-bottom:12px;color:var(--text-secondary)">
        将 <strong style="color:var(--text-primary)">{{ joinStudentName }}</strong> 加入班级：
      </p>
      <el-select v-model="joinClassId" placeholder="请选择班级" style="width:100%" filterable>
        <el-option v-for="c in classList" :key="c.id" 
                   :label="`${c.classCode} - ${c.className}`" 
                   :value="c.id" 
                   :disabled="joinedClasses.some(jc => jc.classId === c.id && jc.status === 1)" />
      </el-select>
      <template #footer>
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleJoin">确定入班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({ name: '', gender: null, phone: '', idCard: '', status: 1 })
const rules = { name: [{ required: true, message: '请输入姓名', trigger: 'blur' }] }

// 入班相关
const joinDialogVisible = ref(false)
const joinStudentId = ref(null)
const joinStudentName = ref('')
const joinClassId = ref(null)
const classList = ref([])
const joinedClasses = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.getStudentList({ current: current.value, size: size.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  editId.value = row ? row.id : null
  form.name = row ? row.name : ''
  form.gender = row ? row.gender : null
  form.phone = row ? row.phone : ''
  form.idCard = row ? row.idCard : ''
  form.status = row ? row.status : 1
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editId.value) {
      await api.updateStudent({ id: editId.value, ...form })
      ElMessage.success('修改成功')
    } else {
      await api.addStudent(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该学生吗？', '提示', { type: 'warning' }).then(async () => {
    await api.deleteStudent(id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const openJoinDialog = async (row) => {
  joinStudentId.value = row.id
  joinStudentName.value = row.name
  joinClassId.value = null
  // 加载班级列表与该学生已有的班级
  const [classRes, studentClassesRes] = await Promise.all([
    api.getClassList({ current: 1, size: 999 }),
    api.getStudentClasses(row.id)
  ])
  classList.value = classRes.data.records || []
  joinedClasses.value = studentClassesRes.data || []
  joinDialogVisible.value = true
}

const handleJoin = async () => {
  if (!joinClassId.value) {
    ElMessage.warning('请选择班级')
    return
  }
  await api.joinClass({ studentId: joinStudentId.value, classId: joinClassId.value })
  ElMessage.success('入班成功')
  joinDialogVisible.value = false
}

onMounted(loadData)
</script>
