import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as null | { username: string; displayName: string; roles: string[] }
  }),
  actions: {
    async login(username: string) {
      const resp = await authApi.login(username)
      localStorage.setItem('userId', username)
      this.user = resp.user
    },
    async loadMe() {
      if (!localStorage.getItem('userId')) {
        this.user = null
        return
      }
      this.user = await authApi.me()
    },
    logout() {
      localStorage.removeItem('userId')
      this.user = null
    }
  }
})
