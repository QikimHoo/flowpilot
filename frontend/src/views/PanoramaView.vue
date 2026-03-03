<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card>
        <template #header>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
            <el-select v-model="lightFilter" clearable placeholder="信号灯筛选" style="width:120px">
              <el-option label="GREEN" value="GREEN" />
              <el-option label="YELLOW" value="YELLOW" />
              <el-option label="RED" value="RED" />
            </el-select>
            <el-input v-model="keyword" placeholder="关键字" clearable style="flex:1;min-width:120px" />
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="list">列表</el-radio-button>
              <el-radio-button value="chart">图表</el-radio-button>
            </el-radio-group>
          </div>
        </template>

        <el-tree
          v-if="viewMode === 'list'"
          :data="filteredTree"
          node-key="id"
          :props="treeProps"
          @node-click="onNodeClick"
          default-expand-all
          style="max-height:70vh;overflow:auto"
        >
          <template #default="{ data }">
            <div style="display:flex;align-items:center;gap:8px">
              <span>{{ data.nodeName }}</span>
              <TrafficTag :value="data.computed?.trafficLight" />
            </div>
          </template>
        </el-tree>

        <TreeChart
          v-else
          :data="filteredTree"
          @node-click="onNodeClick"
        />
      </el-card>
    </el-col>

    <el-col :span="16">
      <el-card v-if="selected">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>{{ selected.nodeName }} ({{ selected.id }})</span>
            <TrafficTag :value="selected.computed?.trafficLight" />
          </div>
        </template>

        <el-row :gutter="12">
          <el-col :span="8"><el-statistic title="计划得分" :value="selected.computed?.planScore || 0" /></el-col>
          <el-col :span="8"><el-statistic title="实际得分" :value="selected.computed?.actualScore || 0" /></el-col>
          <el-col :span="8"><el-statistic title="偏差" :value="(selected.computed?.deviation || 0) * 100" suffix="%" /></el-col>
        </el-row>

        <el-divider />
        <el-tabs>
          <el-tab-pane label="维度进度条">
            <div v-for="dim in dims" :key="dim" style="margin-bottom:10px">
              <div style="display:flex;justify-content:space-between;font-size:13px">
                <span>{{ dim }}</span>
                <span>计划 {{ pct(planDone(dim)) }}% / 实际 {{ pct(actualDone(dim)) }}%</span>
              </div>
              <el-progress :percentage="pct(actualDone(dim))" :stroke-width="10" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="雷达图">
            <RadarChart :plan="planData" :actual="actualData" :dims="dims" />
          </el-tab-pane>
        </el-tabs>

        <el-divider />
        <div style="font-weight:600;margin-bottom:8px">偏差来源 Top3（贡献度 %）</div>
        <TopFactorChart :factors="selected.computed?.deviationTopFactors || []" />

        <el-divider />
        <el-row :gutter="12">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>阻塞摘要</template>
              <div>blockedTasks: {{ selected.actual?.kpi?.dev?.blockedTasks || 0 }}</div>
              <div>reworkTasks: {{ selected.actual?.kpi?.dev?.reworkTasks || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>合规摘要</template>
              <div>overdueDocs: {{ selected.actual?.kpi?.compliance?.overdueDocs || 0 }}</div>
              <div>keyOverdue: {{ selected.computed?.complianceKeyOverdue ? '是' : '否' }}</div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
      <el-empty v-else description="请选择左侧节点" />
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { nodeApi } from '../api'
import type { NodeData } from '../types'
import TrafficTag from '../components/TrafficTag.vue'
import TopFactorChart from '../components/TopFactorChart.vue'
import RadarChart from '../components/RadarChart.vue'
import TreeChart from '../components/TreeChart.vue'

const tree = ref<NodeData[]>([])
const selected = ref<NodeData | null>(null)
const lightFilter = ref('')
const keyword = ref('')
const viewMode = ref<'list' | 'chart'>('list')

const treeProps = { children: 'children', label: 'nodeName' }
const dims = ['req', 'dev', 'test', 'milestone', 'compliance']

const pct = (v: number) => Number((v * 100).toFixed(1))
const planDone = (dim: string) => selected.value?.plan?.kpi?.[dim]?.expectedDone || 0
const actualDone = (dim: string) => selected.value?.actual?.kpi?.[dim]?.done || 0

const planData = computed(() => {
  const data: Record<string, number> = {}
  dims.forEach((d) => {
    data[d] = planDone(d)
  })
  return data
})

const actualData = computed(() => {
  const data: Record<string, number> = {}
  dims.forEach((d) => {
    data[d] = actualDone(d)
  })
  return data
})

const filteredTree = computed(() => {
  const clone = JSON.parse(JSON.stringify(tree.value)) as NodeData[]
  const filter = (nodes: NodeData[]): NodeData[] => {
    return nodes
      .map((n) => ({ ...n, children: filter(n.children || []) }))
      .filter((n) => {
        const matchLight = !lightFilter.value || n.computed?.trafficLight === lightFilter.value
        const matchKeyword = !keyword.value || n.nodeName.includes(keyword.value) || n.id.includes(keyword.value)
        return (matchLight && matchKeyword) || (n.children && n.children.length > 0)
      })
  }
  return filter(clone)
})

const onNodeClick = async (node: NodeData) => {
  selected.value = await nodeApi.get(node.id)
}

onMounted(async () => {
  tree.value = await nodeApi.tree()
})
</script>
