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
    meta: { title: '在线作业', requiresAuth: false }
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
  if (to.meta.requiresAuth === false) {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
