<template>
  <div ref="chartRef" style="height: 600px; width: 100%"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { NodeData } from '../types'

const props = defineProps<{
  data: NodeData[]
}>()

const emit = defineEmits<{
  nodeClick: [node: NodeData]
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const convertToEchartsTree = (nodes: NodeData[]): any[] => {
  return nodes.map((node) => ({
    name: node.nodeName,
    value: node.id,
    itemStyle: {
      color: getColorByTrafficLight(node.computed?.trafficLight)
    },
    label: {
      color: '#fff',
      fontSize: 12
    },
    children: node.children ? convertToEchartsTree(node.children) : []
  }))
}

const getColorByTrafficLight = (light?: string): string => {
  switch (light) {
    case 'GREEN':
      return '#67c23a'
    case 'YELLOW':
      return '#e6a23c'
    case 'RED':
      return '#f56c6c'
    default:
      return '#909399'
  }
}

const render = () => {
  if (!chartRef.value || !props.data.length) return
  if (!chart) chart = echarts.init(chartRef.value)

  const treeData = convertToEchartsTree(props.data)

  chart.setOption({
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove'
    },
    series: [
      {
        type: 'tree',
        data: treeData,
        top: '5%',
        left: '10%',
        bottom: '5%',
        right: '20%',
        symbolSize: 10,
        orient: 'LR',
        label: {
          position: 'left',
          verticalAlign: 'middle',
          align: 'right'
        },
        leaves: {
          label: {
            position: 'right',
            verticalAlign: 'middle',
            align: 'left'
          }
        },
        expandAndCollapse: true,
        animationDuration: 550,
        animationDurationUpdate: 750
      }
    ]
  })

  chart.on('click', (params: any) => {
    if (params.data && params.data.value) {
      const nodeId = params.data.value
      const findNode = (nodes: NodeData[]): NodeData | null => {
        for (const node of nodes) {
          if (node.id === nodeId) return node
          if (node.children) {
            const found = findNode(node.children)
            if (found) return found
          }
        }
        return null
      }
      const node = findNode(props.data)
      if (node) emit('nodeClick', node)
    }
  })
}

onMounted(render)
watch(() => props.data, render, { deep: true })
onBeforeUnmount(() => chart?.dispose())
</script>
