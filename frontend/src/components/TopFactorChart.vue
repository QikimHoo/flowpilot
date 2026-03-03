<template>
  <div ref="chartRef" style="height: 280px; width: 100%"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{ factors: Array<{ dim: string; contrib: number }> }>()
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const render = () => {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: props.factors.map((f) => f.dim) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: props.factors.map((f) => Number((f.contrib * 100).toFixed(2))),
        itemStyle: { color: '#1f77b4' }
      }
    ]
  })
}

onMounted(render)
watch(() => props.factors, render, { deep: true })
onBeforeUnmount(() => chart?.dispose())
</script>
