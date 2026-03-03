<template>
  <el-card>
    <template #header>报表导出</template>
    <div style="display:flex;gap:12px">
      <el-button type="primary" @click="download('weekly')">下载周报 CSV</el-button>
      <el-button type="success" @click="download('monthly')">下载月报 CSV</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'

const baseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api')

const download = async (period: 'weekly' | 'monthly') => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('请先登录')
    return
  }
  const res = await fetch(`${baseUrl}/reports/${period}.csv`, {
    headers: { 'X-User-Id': userId }
  })
  if (!res.ok) {
    ElMessage.error('导出失败')
    return
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${period}-report.csv`
  a.click()
  URL.revokeObjectURL(url)
}
</script>
