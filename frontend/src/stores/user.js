import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(null)
  const username = ref('')
  const realName = ref('')
  const role = ref(null)

  function setLoginData(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    role.value = data.role
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
  }

  function loadFromStorage() {
    const info = localStorage.getItem('userInfo')
    if (info) {
      const data = JSON.parse(info)
      userId.value = data.userId
      username.value = data.username
      realName.value = data.realName
      role.value = data.role
    }
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    realName.value = ''
    role.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const isAdmin = () => role.value === 1

  // 初始化时从 localStorage 加载
  loadFromStorage()

  return { token, userId, username, realName, role, setLoginData, logout, isAdmin, loadFromStorage }
})
