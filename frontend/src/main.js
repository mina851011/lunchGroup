import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

import axios from 'axios'

const apiBase = import.meta.env.VITE_API_BASE_URL || ''
axios.defaults.baseURL = apiBase

const app = createApp(App)
app.use(router)

// Global helper for image URLs
app.config.globalProperties.$api = (path) => {
    if (!path) return ''
    if (path.startsWith('http')) return path
    return `${apiBase}${path}`
}

app.mount('#app')
