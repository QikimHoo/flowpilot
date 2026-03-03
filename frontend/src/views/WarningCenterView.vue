<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between">
        <span>预警中心</span>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>

    <el-table :data="warnings" style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nodeId" label="节点" width="140" />
      <el-table-column prop="level" label="级别" width="100" />
      <el-table-column prop="deviation" label="偏差" width="120">
        <template #default="{ row }">{{ (row.deviation * 100).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="130" />
      <el-table-column prop="triggeredAt" label="触发时间" />
      <el-table-column prop="slaDueAt" label="SLA" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openAction(row)">处置</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="actionVisible" title="预警处置" width="520px">
    <el-form>
      <el-form-item label="状态">
        <el-select v-model="actionForm.status">
          <el-option label="OPEN" value="OPEN" />
          <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
          <el-option label="RESOLVED" value="RESOLVED" />
          <el-option label="CLOSED" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="actionForm.note" type="textarea" rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="actionVisible = false">取消</el-button>
      <el-button type="primary" @click="submitAction">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { warningApi } from '../api'
import type { WarningData } from '../types'

const warnings = ref<WarningData[]>([])
const actionVisible = ref(false)
const currentId = ref<number | null>(null)
const actionForm = ref({ status: 'IN_PROGRESS', note: '' })

const load = async () => {
  warnings.value = await warningApi.list()
}

const openAction = (row: WarningData) => {
  currentId.value = row.id
  actionForm.value = { status: row.status, note: '' }
  actionVisible.value = true
}

const submitAction = async () => {
  if (!currentId.value) return
  await warningApi.action(currentId.value, actionForm.value)
  ElMessage.success('处置已保存')
  actionVisible.value = false
  await load()
}

onMounted(load)
</script>
