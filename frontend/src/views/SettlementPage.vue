<template>
  <div class="min-h-screen p-4 md:p-8">
    <div class="max-w-2xl mx-auto">
      
      <!-- Header -->
      <header v-if="group" class="text-center space-y-2 mb-8">
        <div v-if="isExpired" class="px-3 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold inline-block">
          ✅ 已結單
        </div>
        <div class="flex gap-2 justify-center" v-else>
          <button 
            @click="closeOrder"
            class="px-4 py-1.5 bg-red-100 text-red-600 rounded-full text-xs font-bold hover:bg-red-200 transition-colors inline-flex items-center gap-1"
          >
            🛑 立即結單 (發送通知)
          </button>
          <button 
            @click="quietClose"
            class="px-4 py-1.5 bg-stone-100 text-stone-600 rounded-full text-xs font-bold hover:bg-stone-200 transition-colors inline-flex items-center gap-1"
          >
            ❌ 關團 (不通知)
          </button>
        </div>
        <h1 class="text-2xl font-bold text-mocha-dark">{{ group.name }}</h1>
        <p class="text-sm text-stone-500">結算明細</p>
      </header>

      <div v-if="loading" class="text-center py-10 text-stone-400">載入中...</div>

      <!-- Summary Card -->
      <div v-if="!loading" class="bg-mocha-dark text-white rounded-2xl p-6 mb-6 shadow-xl">
        <div class="grid grid-cols-3 gap-4 text-center">
          <div>
            <div class="text-stone-400 text-xs mb-1">總金額</div>
            <div class="text-2xl font-bold">${{ totalAmount }}</div>
          </div>
          <div>
            <div class="text-stone-400 text-xs mb-1">已收款</div>
            <div class="text-2xl font-bold text-green-400">${{ paidAmount }}</div>
          </div>
          <div>
            <div class="text-stone-400 text-xs mb-1">未收款</div>
            <div class="text-2xl font-bold text-orange-400">${{ unpaidAmount }}</div>
          </div>
        </div>
        <div class="mt-4 pt-4 border-t border-stone-700 text-center text-sm text-stone-400">
          {{ paidCount }} / {{ orders.length }} 人已付款
        </div>
      </div>

      <!-- Order List -->
      <div v-if="!loading" class="space-y-3">
        <div 
          v-for="order in orders" 
          :key="order.id"
          :class="[
            'bg-white p-4 rounded-2xl border transition-all',
            order.paid ? 'border-green-200 bg-green-50/30' : 'border-stone-100'
          ]"
        >
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <label class="relative inline-flex items-center cursor-pointer">
                <input 
                  type="checkbox" 
                  :checked="order.paid"
                  @change="togglePaid(order)"
                  class="sr-only peer"
                >
                <div class="w-11 h-6 bg-stone-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-stone-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-green-500"></div>
              </label>
              <div>
                <div class="font-bold text-mocha-dark">{{ order.userName }}</div>
                <div class="text-sm text-stone-500">
                  {{ order.itemName }} 
                  <span class="text-xs text-stone-400">x{{ order.quantity }}</span>
                  <span v-if="order.riceLevel && order.riceLevel !== 'FULL'" class="text-[10px] border border-orange-200 text-orange-500 px-1.5 py-0.5 rounded-full font-bold ml-1 inline-block align-middle mb-0.5">
                    {{ getRiceLabel(order.riceLevel) }}
                  </span>
                </div>
                <div v-if="order.note" class="text-xs text-stone-400 mt-0.5 border-l-2 border-stone-200 pl-1.5 py-0.5">
                  {{ order.note }}
                </div>
              </div>
            </div>
            <div :class="['font-bold text-lg', order.paid ? 'text-green-600' : 'text-mocha-primary']">
              ${{ order.totalPrice }}
            </div>
          </div>
        </div>
      </div>

      <!-- Back Button -->
      <div class="mt-8 text-center">
        <router-link 
          :to="`${regionPrefix}/group/${groupId}`"
          class="text-mocha-primary font-medium hover:underline"
        >
          ← 返回點餐頁
        </router-link>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const riceOptions = [
  { label: '正常', value: 'FULL' },
  { label: '半', value: 'HALF' },
  { label: '少', value: 'LESS' }
]

const getRiceLabel = (val) => {
    const opt = riceOptions.find(o => o.value === val)
    return opt ? opt.label : val
}

const route = useRoute()
const groupId = route.params.groupId
const regionPrefix = computed(() => route.path.startsWith('/taipei') ? '/taipei' : '/taichung')

const group = ref(null)
const orders = ref([])
const loading = ref(true)

const totalAmount = computed(() => orders.value.reduce((sum, o) => sum + (o.totalPrice || 0), 0))
const paidAmount = computed(() => orders.value.filter(o => o.paid).reduce((sum, o) => sum + (o.totalPrice || 0), 0))
const unpaidAmount = computed(() => totalAmount.value - paidAmount.value)

const paidCount = computed(() => orders.value.filter(o => o.paid).length)

const currentTime = ref(new Date())
const timer = ref(null)

const isExpired = computed(() => {
    if (!group.value || !group.value.deadline) return false
    // Handle both ISO 'T' and space separated
    const deadlineStr = group.value.deadline.replace(' ', 'T')
    return currentTime.value > new Date(deadlineStr)
})

const fetchData = async () => {
    try {
        const res = await axios.get(`/api/groups/${groupId}`)
        group.value = res.data.group
        orders.value = res.data.orders || []
    } catch (err) {
        console.error("Fetch error", err)
        alert('無法載入資料')
    } finally {
        loading.value = false
    }
}

const togglePaid = async (order) => {
    const newStatus = !order.paid
    try {
        await axios.patch(`/api/groups/${groupId}/orders/${order.id}/paid`, { paid: newStatus })
        order.paid = newStatus
    } catch (err) {
        console.error("Update error", err)
        alert('更新失敗')
    }
}

const closeOrder = async () => {
    if (!confirm('確定要立即結單嗎？這會發送 LINE 通知。')) return
    
    try {
        const deadlineStr = getLocalDeadlineStr()
        await axios.patch(`/api/groups/${groupId}/deadline`, { deadline: deadlineStr })
        await fetchData()
    } catch (err) {
        console.error("Close order error", err)
        alert('結單失敗')
    }
}

const quietClose = async () => {
    if (!confirm('確定要關團嗎？這【不會】發送 LINE 通知。')) return
    
    try {
        const deadlineStr = getLocalDeadlineStr()
        await axios.patch(`/api/groups/${groupId}/quiet-close`, { deadline: deadlineStr })
        await fetchData()
    } catch (err) {
        console.error("Quiet close error", err)
        alert('關團失敗')
    }
}

const getLocalDeadlineStr = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hour = String(now.getHours()).padStart(2, '0')
    const minute = String(now.getMinutes()).padStart(2, '0')
    const second = String(now.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

onMounted(() => {
    fetchData()
    timer.value = setInterval(() => {
        currentTime.value = new Date()
    }, 1000)
})

onUnmounted(() => {
    if (timer.value) clearInterval(timer.value)
})
</script>
