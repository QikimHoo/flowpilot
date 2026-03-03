import axios from 'axios'

const api = axios.create({
  // Use same-origin /api by default so remote browser access still works in dev via Vite proxy.
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api'
})

api.interceptors.request.use((config) => {
  const userId = localStorage.getItem('userId')
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
})

export default api
