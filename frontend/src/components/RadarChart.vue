<template>
  <div ref="chartRef" style="height: 320px; width: 100%"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  plan: Record<string, number>
  actual: Record<string, number>
  dims: string[]
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const render = () => {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const indicator = props.dims.map((dim) => ({
    name: dim,
    max: 1
  }))

  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: {
      data: ['计划', '实际'],
      bottom: 0
    },
    radar: {
      indicator,
      radius: '60%'
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: props.dims.map((d) => props.plan[d] || 0),
            name: '计划',
            itemStyle: { color: '#5470c6' }
          },
          {
            value: props.dims.map((d) => props.actual[d] || 0),
            name: '实际',
            itemStyle: { color: '#91cc75' }
          }
        ]
      }
    ]
  })
}

onMounted(render)
watch(() => [props.plan, props.actual], render, { deep: true })
onBeforeUnmount(() => chart?.dispose())
</script>
