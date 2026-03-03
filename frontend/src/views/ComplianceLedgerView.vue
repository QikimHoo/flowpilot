<template>
  <el-card>
    <template #header>
      <div style="display:flex;gap:8px;align-items:center">
        <el-input v-model="nodeId" placeholder="按节点过滤(nodeId)" style="width:220px" />
        <el-button @click="load">查询</el-button>
        <el-button type="primary" @click="openCreate">新增材料</el-button>
      </div>
    </template>

    <el-table :data="items" style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nodeId" label="节点" width="130" />
      <el-table-column prop="docType" label="材料类型" width="160" />
      <el-table-column prop="isKey" label="关键" width="80">
        <template #default="{ row }">{{ row.isKey ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="requiredAt" label="应提交" width="120" />
      <el-table-column prop="submittedAt" label="已提交" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="overdueDays" label="超期天数" width="100" />
      <el-table-column label="附件">
        <template #default="{ row }">
          <a v-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank">查看</a>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" :title="editingId ? '编辑材料' : '新增材料'" width="560px">
    <el-form label-width="110px">
      <el-form-item label="节点ID"><el-input v-model="form.nodeId" /></el-form-item>
      <el-form-item label="材料类型"><el-input v-model="form.docType" /></el-form-item>
      <el-form-item label="关键材料"><el-switch v-model="form.isKey" /></el-form-item>
      <el-form-item label="应提交日期"><el-date-picker v-model="form.requiredAt" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="已提交日期"><el-date-picker v-model="form.submittedAt" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="状态"><el-input v-model="form.status" /></el-form-item>
      <el-form-item label="超期天数"><el-input-number v-model="form.overdueDays" :min="0" /></el-form-item>
      <el-form-item label="附件链接"><el-input v-model="form.attachmentUrl" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { complianceApi } from '../api'
import type { ComplianceItem } from '../types'

const items = ref<ComplianceItem[]>([])
const nodeId = ref('')
const visible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<any>({
  nodeId: '',
  docType: '',
  isKey: false,
  requiredAt: '',
  submittedAt: '',
  status: 'PENDING',
  overdueDays: 0,
  attachmentUrl: ''
})

const load = async () => {
  items.value = await complianceApi.list(nodeId.value || undefined)
}

const openCreate = () => {
  editingId.value = null
  form.value = { nodeId: '', docType: '', isKey: false, requiredAt: '', submittedAt: '', status: 'PENDING', overdueDays: 0, attachmentUrl: '' }
  visible.value = true
}

const openEdit = (row: ComplianceItem) => {
  editingId.value = row.id
  form.value = { ...row }
  visible.value = true
}

const submit = async () => {
  if (editingId.value) {
    await complianceApi.update(editingId.value, form.value)
  } else {
    await complianceApi.create(form.value)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

onMounted(load)
</script>
