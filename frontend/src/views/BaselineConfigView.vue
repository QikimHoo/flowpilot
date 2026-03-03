<template>
  <el-card>
    <template #header>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-model="nodeId" filterable placeholder="选择节点" style="width:360px" @change="loadNode">
          <el-option v-for="n in flatNodes" :key="n.id" :label="`${n.id} - ${n.nodeName}`" :value="n.id" />
        </el-select>
        <el-button @click="loadTree">刷新</el-button>
      </div>
    </template>

    <div v-if="node">
      <h3>权重配置（sum=1）</h3>
      <el-row :gutter="12">
        <el-col v-for="d in dims" :span="4" :key="d">
          <el-input-number v-model="weights[d]" :min="0" :max="1" :step="0.01" :precision="2" style="width:100%" />
          <div style="text-align:center;margin-top:4px">{{ d }}</div>
        </el-col>
      </el-row>
      <div style="margin-top:8px">当前合计：{{ weightSum.toFixed(2) }}</div>

      <el-divider />
      <h3>计划完成度 expectedDone</h3>
      <el-row :gutter="12">
        <el-col v-for="d in dims" :span="4" :key="d">
          <el-input-number v-model="expectedDone[d]" :min="0" :max="1" :step="0.01" :precision="2" style="width:100%" />
          <div style="text-align:center;margin-top:4px">{{ d }}</div>
        </el-col>
      </el-row>

      <div style="margin-top:16px">
        <el-button type="primary" @click="save">保存并重算</el-button>
      </div>
    </div>
    <el-empty v-else description="请选择一个节点" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { nodeApi } from '../api'
import type { NodeData } from '../types'

const dims = ['req', 'dev', 'test', 'milestone', 'compliance']
const tree = ref<NodeData[]>([])
const flatNodes = ref<NodeData[]>([])
const nodeId = ref('')
const node = ref<NodeData | null>(null)
const weights = ref<Record<string, number>>({ req: 0.15, dev: 0.35, test: 0.25, milestone: 0.15, compliance: 0.1 })
const expectedDone = ref<Record<string, number>>({ req: 0, dev: 0, test: 0, milestone: 0, compliance: 0 })

const weightSum = computed(() => dims.reduce((s, d) => s + (weights.value[d] || 0), 0))

const flatten = (nodes: NodeData[]): NodeData[] => nodes.flatMap((n) => [n, ...(n.children ? flatten(n.children) : [])])

const loadTree = async () => {
  tree.value = await nodeApi.tree()
  flatNodes.value = flatten(tree.value)
}

const loadNode = async () => {
  if (!nodeId.value) return
  node.value = await nodeApi.get(nodeId.value)
  weights.value = { ...weights.value, ...(node.value.weights || {}) }
  dims.forEach((d) => {
    expectedDone.value[d] = Number(node.value?.plan?.kpi?.[d]?.expectedDone || 0)
  })
}

const save = async () => {
  if (Math.abs(weightSum.value - 1) > 0.0001) {
    ElMessage.error('权重和必须等于 1')
    return
  }
  if (!node.value) return
  const kpi: Record<string, any> = {}
  dims.forEach((d) => {
    kpi[d] = { ...(node.value?.plan?.kpi?.[d] || {}), expectedDone: expectedDone.value[d] }
  })

  await nodeApi.update(node.value.id, {
    weights: weights.value,
    plan: { ...node.value.plan, kpi }
  })
  await nodeApi.recalc(node.value.id)
  ElMessage.success('保存成功')
  await loadNode()
}

onMounted(loadTree)
</script>
