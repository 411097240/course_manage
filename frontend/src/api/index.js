import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default {
  // 认证
  login: (data) => request.post('/auth/login', data),
  getUserInfo: () => request.get('/auth/info'),
  getConfigH5Url: () => request.get('/common/config/h5Url'),
  changePassword: (data) => request.post('/auth/change-password', data),

  // 班级
  getClassList: (params) => request.get('/class/list', { params }),
  getClassById: (id) => request.get(`/class/${id}`),
  addClass: (data) => request.post('/class', data),
  updateClass: (data) => request.put('/class', data),
  deleteClass: (id) => request.delete(`/class/${id}`),

  // 课程
  getCourseList: (classId) => request.get('/course/list', { params: { classId } }),
  addCourse: (data) => request.post('/course', data),
  addCourseBatch: (data) => request.post('/course/batch', data),
  updateCourse: (data) => request.put('/course', data),
  deleteCourse: (id) => request.delete(`/course/${id}`),

  // 学生
  getStudentList: (params) => request.get('/student/list', { params }),
  getStudentById: (id) => request.get(`/student/${id}`),
  addStudent: (data) => request.post('/student', data),
  updateStudent: (data) => request.put('/student', data),
  deleteStudent: (id) => request.delete(`/student/${id}`),
  getStudentSchedule: (id) => request.get(`/student/${id}/schedule`),

  // 学生班级
  joinClass: (data) => request.post('/student-class/join', data),
  leaveClass: (data) => request.post('/student-class/leave', data),
  getStudentClasses: (studentId) => request.get('/student-class/list', { params: { studentId } }),

  // 教师班级分配
  getUserClasses: (userId) => request.get('/user-class/list', { params: { userId } }),
  assignClass: (data) => request.post('/user-class/assign', data),
  removeClass: (data) => request.post('/user-class/remove', data),
  getTeachers: () => request.get('/user-class/teachers'),
  createTeacher: (data) => request.post('/user-class/teacher', data),
  resetPassword: (data) => request.post('/user-class/reset-password', data),
  toggleTeacherStatus: (data) => request.post('/user-class/toggle-status', data),

  // 文件上传
  uploadFile: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/common/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 作业管理 (PC端)
  getHomeworkList: (params) => request.get('/homework/page', { params }),
  createHomework: (data) => request.post('/homework/create', data),
  updateHomework: (data) => request.post('/homework/update', data),
  deleteHomework: (id) => request.post(`/homework/delete`, null, { params: { id } }),
  getStudentHomeworkList: (homeworkId) => request.get('/homework/studentHomeworkList', { params: { homeworkId } }),
  reviewHomework: (data) => request.post('/homework/review', data),

  // H5 作业提交流程 (免密)
  getH5HomeworkList: (token, classId) => request.get('/h5/homework/list', { params: { token, classId } }),
  getH5HomeworkDetail: (homeworkId, token) => request.get('/h5/homework/detail', { params: { homeworkId, token } }),
  submitH5Homework: (data, token) => request.post('/h5/homework/submit', data, { params: { token } }),

  // 点名管理
  getRollcallStudents: (classId, date) => request.get('/rollcall/students', { params: { classId, date } }),
  submitRollcall: (data) => request.post('/rollcall/submit', data),
  getRollcallRecords: (params) => request.get('/rollcall/page', { params }),
  getRollcallDetail: (recordId) => request.get('/rollcall/detail', { params: { recordId } }),

  // H5 班级空间
  getH5Schedule: (token, classId) => request.get('/h5/space/schedule', { params: { token, classId } }),
  getH5Attendance: (token, classId) => request.get('/h5/space/attendance', { params: { token, classId } }),
}

