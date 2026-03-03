<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background: #f7f9fc; border-right: 1px solid #e9edf3">
      <div style="padding: 16px; font-weight: 700">进度全景工具</div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/panorama">全景图总览</el-menu-item>
        <el-menu-item index="/warnings">预警中心</el-menu-item>
        <el-menu-item index="/compliance">合规台账</el-menu-item>
        <el-menu-item index="/baseline">基线/权重配置</el-menu-item>
        <el-menu-item index="/export">报表导出</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #eef2f7;">
        <span>{{ auth.user?.displayName || auth.user?.username }}</span>
        <el-button size="small" @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  auth.loadMe()
})

const logout = () => {
  auth.logout()
  router.push('/login')
}
</script>
