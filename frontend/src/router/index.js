import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/h5/homework',
    name: 'H5Homework',
    component: () => import('../views/H5Homework.vue'),
    meta: { title: '班级空间', requiresAuth: false }
  },
  {
    path: '/h5/login',
    name: 'H5Login',
    component: () => import('../views/H5Login.vue'),
    meta: { title: '教师移动端登录', requiresAuth: false }
  },
  {
    path: '/h5/classes',
    name: 'H5ClassSelect',
    component: () => import('../views/H5ClassSelect.vue'),
    meta: { title: '选择班级', requiresAuth: true }
  },
  {
    path: '/h5/rollcall/:classId',
    name: 'H5Rollcall',
    component: () => import('../views/H5Rollcall.vue'),
    meta: { title: '课堂点名', requiresAuth: true }
  },
  {
    path: '/h5/history/:classId',
    name: 'H5RollcallHistory',
    component: () => import('../views/H5RollcallHistory.vue'),
    meta: { title: '历史点名', requiresAuth: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'class',
        name: 'ClassList',
        component: () => import('../views/ClassList.vue'),
        meta: { title: '班级管理' }
      },
      {
        path: 'class/:id/course',
        name: 'CourseList',
        component: () => import('../views/CourseList.vue'),
        meta: { title: '课程排课' }
      },
      {
        path: 'class/:id/rollcall',
        name: 'RollcallList',
        component: () => import('../views/RollcallList.vue'),
        meta: { title: '点名记录' }
      },
      {
        path: 'student',
        name: 'StudentList',
        component: () => import('../views/StudentList.vue'),
        meta: { title: '学生管理' }
      },
      {
        path: 'student/:id',
        name: 'StudentDetail',
        component: () => import('../views/StudentDetail.vue'),
        meta: { title: '学生详情' }
      },
      {
        path: 'assign',
        name: 'TeacherAssign',
        component: () => import('../views/TeacherAssign.vue'),
        meta: { title: '教师班级分配', adminOnly: true }
      },
      {
        path: 'homework',
        name: 'HomeworkList',
        component: () => import('../views/HomeworkList.vue'),
        meta: { title: '班级作业管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login' && token) {
    next('/')
  } else if (to.path === '/h5/login' && token) {
    next(to.query.redirect || '/h5/classes')
  } else if (to.meta.requiresAuth === false) {
    next()
  } else if (!token) {
    if (to.path.startsWith('/h5/')) {
      next('/h5/login?redirect=' + encodeURIComponent(to.fullPath))
    } else {
      next('/login')
    }
  } else {
    next()
  }
})

export default router
