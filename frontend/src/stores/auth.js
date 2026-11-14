import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, login as loginApi, register as registerApi } from '../api/auth'

const TOKEN_KEY = 'fitness_token'
const USER_KEY = 'fitness_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readUser())
  const isLoggedIn = computed(() => Boolean(token.value))

  function setAuth(data) {
    token.value = data.token
    user.value = data.user
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
  }

  async function login(form) {
    const data = await loginApi(form)
    setAuth(data)
    return data
  }

  async function register(form) {
    const data = await registerApi(form)
    setAuth(data)
    return data
  }

  async function refreshMe() {
    const data = await getMe()
    user.value = data
    localStorage.setItem(USER_KEY, JSON.stringify(data))
    return data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, login, register, refreshMe, logout }
})
