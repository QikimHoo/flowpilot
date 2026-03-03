export interface NodeData {
  id: string
  nodeType: string
  nodeName: string
  owner: { userId: string; dept: string }
  parentId?: string
  path: string
  asOfDate?: string
  plan: Record<string, any>
  actual: Record<string, any>
  weights: Record<string, number>
  computed: {
    planScore?: number
    actualScore?: number
    deviation?: number
    trafficLight?: 'GREEN' | 'YELLOW' | 'RED'
    deviationTopFactors?: Array<{ dim: string; contrib: number }>
    complianceKeyOverdue?: boolean
  }
  children?: NodeData[]
}

export interface WarningData {
  id: number
  nodeId: string
  level: 'YELLOW' | 'RED'
  status: string
  deviation: number
  reason: string
  deviationTopFactors: Array<{ dim: string; contrib: number }>
  slaDueAt: string
  triggeredAt: string
  resolvedAt?: string
  assignees: string[]
  actionLog: Array<Record<string, any>>
}

export interface ComplianceItem {
  id: number
  nodeId: string
  docType: string
  isKey: boolean
  requiredAt: string
  submittedAt?: string
  status: string
  overdueDays: number
  attachmentUrl?: string
  meta: Record<string, any>
}
