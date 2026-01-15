<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const name = ref('')
const deadline = ref('')
const loading = ref(false)

const createGroup = async () => {
  if (!name.value || !deadline.value) return
  
  loading.value = true
  try {
    const response = await axios.post('http://localhost:8080/api/groups', {
      name: name.value,
      deadline: deadline.value
    })
    const group = response.data
    router.push({ name: 'order', params: { id: group.id } })
  } catch (e) {
    alert('Failed to create group')
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="p-6 space-y-8">
    <div class="text-center space-y-2">
      <h1 class="text-2xl font-bold tracking-tight text-gray-900">開團訂餐</h1>
      <p class="text-sm text-gray-500">簡單、快速的辦公室團購工具</p>
    </div>

    <form @submit.prevent="createGroup" class="space-y-6">
      <div class="space-y-2">
        <label class="text-sm font-medium text-gray-700">團體名稱</label>
        <input 
          v-model="name" 
          type="text" 
          placeholder="例如：週五便當團" 
          class="w-full px-4 py-3 rounded-xl border-gray-200 bg-gray-50 focus:bg-white focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all outline-none"
          required
        >
      </div>

      <div class="space-y-2">
        <label class="text-sm font-medium text-gray-700">截止時間</label>
        <input 
          v-model="deadline" 
          type="datetime-local" 
          class="w-full px-4 py-3 rounded-xl border-gray-200 bg-gray-50 focus:bg-white focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all outline-none"
          required
        >
      </div>

      <button 
        type="submit" 
        :disabled="loading"
        class="w-full py-4 bg-brand-primary text-white rounded-xl font-medium shadow-lg shadow-brand-primary/30 active:scale-[0.98] transition-transform flex justify-center items-center"
      >
        <span v-if="loading" class="animate-spin mr-2">⚪</span>
        {{ loading ? '建立中...' : '建立團體' }}
      </button>
    </form>
    
    <div class="pt-8 border-t border-gray-100 text-center">
        <router-link to="/history" class="text-sm text-gray-400 hover:text-brand-primary transition-colors">
            查看歷史紀錄 (開發中)
        </router-link>
    </div>
  </div>
</template>
