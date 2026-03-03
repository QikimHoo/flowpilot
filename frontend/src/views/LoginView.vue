<template>
  <div style="height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(120deg,#f4f8ff,#eef7f7)">
    <el-card style="width:420px">
      <template #header>
        <div style="font-weight:700">项目进度管理小工具</div>
      </template>
      <el-form>
        <el-form-item label="选择用户">
          <el-select v-model="username" style="width:100%" placeholder="请选择用户">
            <el-option v-for="u in users" :key="u.username" :label="`${u.displayName} (${u.roles.join(',')})`" :value="u.username" />
          </el-select>
        </el-form-item>
        <el-button type="primary" style="width:100%" @click="login">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const users = ref<Array<{ username: string; displayName: string; roles: string[] }>>([])
const username = ref('u_leader')
const auth = useAuthStore()
const router = useRouter()

onMounted(async () => {
  try {
    users.value = await authApi.users()
  } catch (e: any) {
    // Fallback list keeps login usable when users API is temporarily unreachable.
    users.value = [
      { username: 'u_leader', displayName: '行领导', roles: ['ROLE_LEADER'] },
      { username: 'u_pmo', displayName: 'PMO管理员', roles: ['ROLE_PMO'] },
      { username: 'u_pm', displayName: '项目经理李四', roles: ['ROLE_PM'] },
      { username: 'u_owner', displayName: '模块负责人王五', roles: ['ROLE_MODULE_OWNER'] },
      { username: 'u_compliance', displayName: '合规联络人赵六', roles: ['ROLE_COMPLIANCE'] },
      { username: 'u_viewer', displayName: '只读观察员', roles: ['ROLE_VIEWER'] }
    ]
    ElMessage.error(`用户列表加载失败：${e?.response?.data?.message || e?.message || '请检查后端连接'}`)
  }
})

const login = async () => {
  if (!username.value) {
    ElMessage.warning('请选择用户')
    return
  }
  try {
    await auth.login(username.value)
    router.push('/panorama')
  } catch (e: any) {
    ElMessage.error(`登录失败：${e?.response?.data?.message || e?.message || '请检查后端连接'}`)
  }
}
</script>
