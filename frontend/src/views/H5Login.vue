<template>
  <div class="h5-login-container">
    <div class="h5-login-box">
      <div class="h5-login-header">
        <div class="logo">📱</div>
        <h2>教师移动端登录</h2>
        <p>教学管理系统</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                    size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await api.login(form)
    userStore.setLoginData(res.data)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/h5/classes'
    router.push(redirect)
  } catch (e) {
    // 错误拦截器已处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.h5-login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f5f7;
  padding: 20px;
}
.h5-login-box {
  width: 100%;
  max-width: 400px;
  background: white;
  padding: 40px 20px;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.05);
}
.h5-login-header {
  text-align: center;
  margin-bottom: 30px;
}
.logo {
  font-size: 48px;
  margin-bottom: 10px;
}
.h5-login-header h2 {
  font-size: 22px;
  color: #333;
  margin: 0 0 5px 0;
}
.h5-login-header p {
  font-size: 14px;
  color: #999;
  margin: 0;
}
.login-btn {
  width: 100%;
  border-radius: 20px;
  margin-top: 10px;
}
</style>
