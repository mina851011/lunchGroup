<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const groupId = route.params.id

const group = ref(null)
const orders = ref([])
const loading = ref(true)

const fetchGroup = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/groups/${groupId}`)
    group.value = res.data.group
    orders.value = res.data.orders
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const totalAmount = computed(() => {
    return orders.value.reduce((sum, order) => sum + order.totalPrice, 0)
})

const itemSummary = computed(() => {
    const map = {}
    orders.value.forEach(o => {
        const key = o.itemName
        if (!map[key]) map[key] = { count: 0, total: 0 }
        map[key].count++
        map[key].total += o.totalPrice
    })
    return map
})

onMounted(fetchGroup)
</script>

<template>
  <div v-if="loading" class="flex justify-center p-12">Loading...</div>
  <div v-else class="space-y-6">
    <div class="text-center py-4 border-b border-gray-100">
        <h1 class="text-xl font-bold text-gray-900">統計結果</h1>
        <p class="text-sm text-gray-500">{{ group.name }}</p>
    </div>

    <!-- Big Number -->
    <div class="bg-gray-900 text-white rounded-2xl p-6 text-center shadow-xl shadow-gray-200">
        <div class="text-gray-400 text-sm mb-1">總金額</div>
        <div class="text-4xl font-bold tracking-tight">${{ totalAmount }}</div>
        <div class="mt-4 pt-4 border-t border-gray-800 flex justify-center gap-8 text-sm">
            <div>
                <span class="block text-gray-500">訂單數</span>
                <span class="font-medium">{{ orders.length }}</span>
            </div>
            <div>
                 <span class="block text-gray-500">品項數</span>
                 <span class="font-medium">{{ Object.keys(itemSummary).length }}</span>
            </div>
        </div>
    </div>

    <!-- Item Summary -->
    <div>
        <h3 class="font-bold text-gray-900 mb-3 px-1">餐點統計</h3>
        <div class="space-y-2">
            <div v-for="(stat, name) in itemSummary" :key="name" class="flex justify-between items-center bg-white p-4 rounded-xl border border-gray-100">
                <span class="font-medium text-gray-700">{{ name }}</span>
                <div class="flex items-center gap-3">
                    <span class="bg-gray-100 px-2 py-1 rounded text-xs font-bold text-gray-600">x{{ stat.count }}</span>
                    <span class="text-sm font-bold text-gray-900 min-w-[3rem] text-right">${{ stat.total }}</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Detail List -->
    <div>
        <h3 class="font-bold text-gray-900 mb-3 px-1 mt-6">詳細明細</h3>
        <div class="divide-y divide-gray-100 bg-white rounded-2xl overflow-hidden shadow-sm">
            <div v-for="order in orders" :key="order.id" class="p-4 flex flex-col gap-1">
                <div class="flex justify-between items-start">
                    <div class="font-medium text-gray-900">{{ order.userName }}</div>
                    <div class="font-bold text-gray-900">${{ order.totalPrice }}</div>
                </div>
                <div class="flex justify-between text-sm text-gray-500">
                    <span>
                        {{ order.itemName }}
                        <span v-if="order.riceLevel !== 'FULL'" class="text-brand-primary">({{ order.riceLevel }})</span>
                        <span v-if="order.swapVeg" class="text-red-400 ml-1">換菜</span>
                    </span>
                    <span>{{ order.note }}</span>
                </div>
            </div>
        </div>
    </div>
    
    <div class="text-center pt-8 pb-4">
        <router-link :to="`/group/${groupId}`" class="text-brand-primary font-medium hover:underline">
            ← 返回點餐
        </router-link>
    </div>
  </div>
</template>
