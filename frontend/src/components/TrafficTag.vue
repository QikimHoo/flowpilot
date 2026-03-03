<template>
  <el-tag :type="tagType">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ value?: string }>()

const label = computed(() => props.value || 'GREEN')
const tagType = computed(() => {
  if (props.value === 'RED') return 'danger'
  if (props.value === 'YELLOW') return 'warning'
  return 'success'
})
</script>
