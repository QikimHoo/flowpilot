import api from './http'
import type { ComplianceItem, NodeData, WarningData } from '../types'

export const authApi = {
  users: async () => (await api.get('/auth/users')).data,
  login: async (username: string) => (await api.post('/auth/login', { username })).data,
  me: async () => (await api.get('/auth/me')).data
}

export const nodeApi = {
  tree: async () => (await api.get<NodeData[]>('/nodes/tree')).data,
  get: async (id: string) => (await api.get<NodeData>(`/nodes/${id}`)).data,
  update: async (id: string, payload: any) => (await api.put(`/nodes/${id}`, payload)).data,
  recalc: async (id: string) => (await api.post(`/compute/recalc/${id}`)).data,
  export: async (id: string) => (await api.get(`/nodes/${id}/export`)).data,
  import: async (payload: any) => (await api.post('/nodes/import', payload)).data
}

export const warningApi = {
  list: async () => (await api.get<WarningData[]>('/warnings')).data,
  action: async (id: number, payload: any) => (await api.post(`/warnings/${id}/action`, payload)).data
}

export const complianceApi = {
  list: async (nodeId?: string) => (await api.get<ComplianceItem[]>('/compliance/items', { params: { nodeId } })).data,
  create: async (payload: any) => (await api.post('/compliance/items', payload)).data,
  update: async (id: number, payload: any) => (await api.put(`/compliance/items/${id}`, payload)).data
}

export const reportApi = {
  weeklyUrl: (rootId?: string) => `${api.defaults.baseURL}/reports/weekly.csv${rootId ? `?rootId=${rootId}` : ''}`,
  monthlyUrl: (rootId?: string) => `${api.defaults.baseURL}/reports/monthly.csv${rootId ? `?rootId=${rootId}` : ''}`
}
