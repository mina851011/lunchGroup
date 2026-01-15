export const apiBase = import.meta.env.VITE_API_BASE_URL || ''

export const getApiUrl = (path) => {
    if (!path) return ''
    if (path.startsWith('http')) return path
    return `${apiBase}${path}`
}
