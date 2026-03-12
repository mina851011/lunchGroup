import axios from 'axios'

export const apiBase = import.meta.env.VITE_API_BASE_URL || ''

export const getApiUrl = (path) => {
    if (!path) return ''
    if (path.startsWith('http')) return path
    return `${apiBase}${path}`
}

/**
 * Derive region from the current path: /taipei/... → 'taipei', else 'taichung'
 */
export const getRegion = () => {
    return window.location.hash.startsWith('#/taipei') ? 'taipei' : 'taichung'
}

// Auto-inject X-Region header into every axios request
axios.interceptors.request.use((config) => {
    config.headers['X-Region'] = getRegion()
    return config
})
