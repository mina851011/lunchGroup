<template>
  <div class="min-h-screen p-4 md:p-8">
    <div class="max-w-4xl mx-auto space-y-6">
      
      <!-- Header / Group Info -->
      <header v-if="group" class="text-center space-y-2 mb-8">
        <div class="flex items-center justify-center gap-2 mb-2">
          <div class="px-3 py-1 bg-white border border-stone-200 rounded-full text-xs text-stone-500">
            團購編號 #{{ group.id.substring(0, 8) }}
          </div>
          <button 
            @click="copyShareLink"
            class="px-3 py-1 bg-mocha-primary text-white rounded-full text-xs font-bold hover:bg-mocha-dark transition-colors flex items-center gap-1 shadow-sm"
          >
            <span>🔗 分享連結</span>
            <span v-if="copied" class="text-[10px] animate-pulse">(已複製!)</span>
          </button>
          
          <!-- View Original Menu Button -->
          <a v-if="group.menuImageUrl" :href="getApiUrl(group.menuImageUrl)" target="_blank" class="px-3 py-1 bg-stone-100 text-stone-600 rounded-full text-xs font-bold hover:bg-stone-200 transition-colors flex items-center gap-1 shadow-sm border border-stone-200">
             🖼️ 查看原圖
          </a>
          
          <!-- Settlement Page Button -->
          <router-link 
            :to="`/group/${group.id}/settlement`"
            class="px-3 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold hover:bg-green-200 transition-colors flex items-center gap-1 shadow-sm border border-green-200"
          >
            💰 結算收款
          </router-link>
        </div>
        <h1 class="text-3xl font-bold text-mocha-dark">{{ group.name }}</h1>
        <p class="text-mocha-text flex items-center justify-center gap-2 flex-wrap">
          <span v-if="group.restaurantName" class="font-bold text-mocha-dark bg-orange-100 px-2 py-0.5 rounded text-sm flex items-center gap-1">
             🏠 {{ group.restaurantName }}
          </span>
          <a v-if="group.restaurantPhone" :href="`tel:${group.restaurantPhone}`" class="font-bold text-white bg-green-500 hover:bg-green-600 px-2 py-0.5 rounded text-sm flex items-center gap-1 transition-colors">
             📞 {{ group.restaurantPhone }}
          </a>
          <span v-if="group.note" class="text-stone-500 bg-stone-100 px-2 py-0.5 rounded text-sm flex items-center gap-1 border border-stone-200">
             📝 {{ group.note }}
          </span>
          <span class="w-1 h-1 rounded-full bg-stone-300"></span>
          <span>結單：{{ formatDate(group.deadline) }}</span>
        </p>
      </header>
      <div v-else-if="loading" class="text-center py-10 text-stone-400">
        載入中...
      </div>

      <div class="grid md:grid-cols-2 gap-6">
        
        <!-- Order Form -->
        <div class="bg-white rounded-[2rem] p-6 md:p-8 shadow-sm border border-stone-100 h-fit">
          <h2 class="text-xl font-bold text-mocha-dark mb-6 flex items-center gap-2">
            ✏️ 我要點餐
          </h2>
          
            <div v-if="isExpired" class="bg-red-50 border border-red-100 rounded-2xl p-6 text-center text-red-600 font-bold mb-6">
                <p class="mb-3">🚫 已超過結單時間，無法再進行點餐</p>
                <button 
                  @click="extendDeadline"
                  class="bg-white border border-red-200 text-red-500 hover:bg-red-50 px-4 py-2 rounded-xl text-sm transition-all shadow-sm border-dashed"
                >
                  ⏰ 延長結單時間
                </button>
            </div>

            <form v-else @submit.prevent="submitOrder" class="space-y-5">
              <div>
                <label class="block text-sm font-medium text-mocha-text mb-1">你的名字</label>
                <input 
                  v-model="form.userName" 
                  type="text" 
                  required
                  class="w-full bg-stone-50 border-none rounded-xl px-4 py-3 focus:ring-2 focus:ring-mocha-primary/50 text-mocha-dark placeholder-stone-300 transition-all"
                  placeholder="例如：Mina"
                >
              </div>

              <!-- SMART MENU GRID -->
              <div v-if="group && group.menu && group.menu.length > 0">
                  <label class="block text-sm font-medium text-mocha-text mb-2">選擇餐點</label>
                  <div class="grid grid-cols-2 gap-3 mb-4">
                      <button
                          type="button"
                          v-for="(item, idx) in group.menu"
                          :key="idx"
                          @click="selectMenuItem(item)"
                          :class="[
                              'p-3 rounded-xl border text-left transition-all relative overflow-hidden',
                              form.itemName === item.name 
                                  ? 'border-mocha-primary bg-orange-50/50 shadow-md ring-1 ring-mocha-primary' 
                                  : 'border-stone-100 bg-white hover:border-mocha-primary/30'
                          ]"
                      >
                          <div class="font-bold text-mocha-dark">{{ item.name }}</div>
                          <div class="text-mocha-primary text-sm">${{ item.price }}</div>
                      </button>
                  </div>
              </div>

              <div class="grid grid-cols-3 gap-4">
                <div class="col-span-2">
                  <label class="block text-sm font-medium text-mocha-text mb-1">餐點名稱</label>
                  <input 
                    v-model="form.itemName" 
                    type="text" 
                    required
                    readonly
                    class="w-full bg-stone-100 border-none rounded-xl px-4 py-3 text-mocha-dark cursor-not-allowed"
                    placeholder="請從上方選擇餐點"
                  >
                </div>
                <div>
                  <label class="block text-sm font-medium text-mocha-text mb-1">價格</label>
                  <div class="w-full bg-stone-100 rounded-xl px-4 py-3 text-mocha-dark text-center font-bold">
                    ${{ form.basePrice || 0 }}
                  </div>
                </div>
              </div>

              <div class="grid grid-cols-3 gap-4">
                <div class="col-span-1">
                  <label class="block text-sm font-medium text-mocha-text mb-1">數量</label>
                  <div class="flex items-center bg-stone-50 rounded-xl overflow-hidden border border-stone-100">
                    <button 
                      type="button" 
                      @click="form.quantity = Math.max(1, form.quantity - 1)"
                      class="px-3 py-3 hover:bg-stone-200 transition-colors text-stone-500 font-bold"
                    >-</button>
                    <input 
                      v-model.number="form.quantity" 
                      type="number" 
                      class="w-full bg-transparent border-none text-center p-0 focus:ring-0 text-mocha-dark font-bold"
                      min="1"
                    >
                    <button 
                      type="button" 
                      @click="form.quantity++"
                      class="px-3 py-3 hover:bg-stone-200 transition-colors text-stone-500 font-bold"
                    >+</button>
                  </div>
                </div>
                <div class="col-span-2">
                  <label class="block text-sm font-medium text-mocha-text mb-1">飯量調整</label>
                  <div class="flex gap-2 bg-stone-50 p-1 rounded-xl">
                    <button 
                      type="button"
                      v-for="opt in riceOptions" 
                      :key="opt.value"
                      @click="form.riceLevel = opt.value"
                      :class="[
                        'flex-1 py-2 text-xs font-bold rounded-lg transition-all',
                        form.riceLevel === opt.value 
                          ? 'bg-mocha-primary text-white shadow-sm' 
                          : 'text-stone-400 hover:text-stone-600'
                      ]"
                    >
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
              </div>

              <div>
                <label class="block text-sm font-medium text-mocha-text mb-1">備註 (例如：不要辣)</label>
                <textarea 
                  v-model="form.note" 
                  rows="2"
                  class="w-full bg-stone-50 border-none rounded-xl px-4 py-3 focus:ring-2 focus:ring-mocha-primary/50 text-mocha-dark placeholder-stone-300 transition-all resize-none"
                  placeholder="選填..."
                ></textarea>
              </div>

              <button 
                type="submit" 
                :disabled="submitting"
                class="w-full bg-mocha-dark text-white font-medium text-lg py-4 rounded-xl shadow-lg hover:bg-[#2C2825] hover:shadow-xl hover:-translate-y-0.5 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed mt-2"
              >
                {{ submitting ? '送出中...' : '送出訂單' }}
              </button>
            </form>
        </div>

        <!-- Orders List -->
        <div class="bg-white/50 backdrop-blur-sm rounded-[2rem] p-6 md:p-8 border border-stone-100">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-xl font-bold text-mocha-dark">📋 訂單列表</h2>
            <span class="bg-mocha-primary/10 text-mocha-primary px-3 py-1 rounded-full text-sm font-bold">
              {{ orders.length }} 人已點
            </span>
          </div>

          <div v-if="orders.length === 0" class="text-center py-10 text-stone-400">
            <p>還沒有人點餐，搶頭香！</p>
          </div>

          <div v-else class="space-y-3 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
            <div 
              v-for="order in orders" 
              :key="order.id"
              class="bg-white p-4 rounded-2xl shadow-[0_2px_8px_rgb(0,0,0,0.02)] border border-stone-50 h-fit"
            >
              <div class="flex justify-between items-start mb-2">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-mocha-dark">{{ order.userName }}</span>
                  <span class="text-[10px] px-2 py-0.5 bg-stone-100 rounded text-stone-400 uppercase tracking-tighter">{{ formatDateShort(order.createdAt) }}</span>
                </div>
                <div class="font-bold text-mocha-primary">
                  ${{ order.totalPrice }}
                </div>
              </div>

              <div class="flex items-center gap-3">
                 <div class="flex-1">
                    <p class="text-mocha-text text-sm font-medium flex items-center gap-2">
                        <span class="bg-stone-100 text-stone-600 px-1.5 py-0.5 rounded text-xs font-bold">x{{ order.quantity }}</span>
                        {{ order.itemName }}
                        <span v-if="order.riceLevel !== 'FULL'" class="text-[10px] border border-orange-200 text-orange-500 px-1.5 py-0.5 rounded-full font-bold">
                            {{ getRiceLabel(order.riceLevel) }}
                        </span>
                    </p>
                    <p v-if="order.note" class="text-stone-400 text-xs mt-1 border-l-2 border-stone-200 pl-2 py-0.5">
                        {{ order.note }}
                    </p>
                 </div>
                 <button 
                   v-if="!isExpired"
                   @click="deleteOrder(order.id)"
                   class="p-2 text-stone-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                   title="刪除訂單"
                 >
                   <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                     <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                   </svg>
                 </button>
              </div>
            </div>
          </div>
          
          <div v-if="orders.length > 0" class="mt-6 pt-6 border-t border-stone-100 flex justify-between items-center text-mocha-dark">
            <span class="font-medium">總計金額</span>
            <span class="text-2xl font-bold">${{ totalAmount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { getApiUrl } from '../utils/api'

const route = useRoute()
const groupId = route.params.groupId

const group = ref(null)
const orders = ref([])
const loading = ref(true)
const submitting = ref(false)
const copied = ref(false)
const currentTime = ref(new Date())
const timer = ref(null)

const isExpired = computed(() => {
    if (!group.value || !group.value.deadline) return false
    return currentTime.value > new Date(group.value.deadline)
})

const copyShareLink = () => {
  navigator.clipboard.writeText(window.location.href)
  copied.value = true
  setTimeout(() => copied.value = false, 2000)
}

const form = ref({
  userName: '',
  itemName: '',
  basePrice: '',
  riceLevel: 'FULL',
  quantity: 1,
  note: '',
})

const riceOptions = [
  { label: '正常', value: 'FULL' },
  { label: '半', value: 'HALF' },
  { label: '少', value: 'LESS' }
]

const totalAmount = computed(() => {
    return orders.value.reduce((sum, order) => sum + (order.totalPrice || 0), 0)
})

const getRiceLabel = (val) => {
    const opt = riceOptions.find(o => o.value === val)
    return opt ? opt.label : val
}

const formatDate = (dateStr) => {
    if (!dateStr) return ''
    return new Date(dateStr).toLocaleString('zh-TW', { hour: '2-digit', minute: '2-digit', month: '2-digit', day: '2-digit' })
}

const formatDateShort = (timestamp) => {
    if (!timestamp) return ''
    const d = new Date(timestamp)
    if (isNaN(d.getTime())) return '' 
    return d.toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit' })
}

const fetchGroupData = async () => {
    try {
        const res = await axios.get(getApiUrl(`/api/groups/${groupId}`))
        group.value = res.data.group
        orders.value = res.data.orders || []
    } catch (err) {
        console.error("Fetch error", err)
        alert('無法載入團購資料，請確認連結是否正確')
    } finally {
        loading.value = false
    }
}

const selectMenuItem = (item) => {
    form.value.itemName = item.name
    form.value.basePrice = item.price
}

const submitOrder = async () => {
    if (!form.value.userName || !form.value.itemName || !form.value.basePrice) return
    
    submitting.value = true
    try {
        const payload = {
            userName: form.value.userName,
            itemName: form.value.itemName,
            basePrice: form.value.basePrice,
            riceLevel: form.value.riceLevel,
            quantity: form.value.quantity,
            note: form.value.note
        }

        await axios.post(`/api/groups/${groupId}/orders`, payload)
        
        await fetchGroupData()
        form.value.itemName = ''
        form.value.basePrice = ''
        form.value.quantity = 1
        form.value.note = ''
        form.value.riceLevel = 'FULL'
        
    } catch (err) {
        console.error("Submit error", err)
        alert('訂單送出失敗，請稍後再試')
    } finally {
        submitting.value = false
    }
}

const deleteOrder = async (orderId) => {
    if (!confirm('確定要刪除這筆訂單嗎？')) return
    
    try {
        await axios.delete(`/api/groups/${groupId}/orders/${orderId}`)
        await fetchGroupData()
    } catch (err) {
        console.error("Delete error", err)
        alert('刪除失敗，請稍後再試')
    }
}

const extendDeadline = async () => {
    const defaultVal = new Date(Date.now() + 3600000).toLocaleString('sv').slice(0, 16).replace('T', ' ')
    const newDateStr = prompt("請輸入新的結單時間 (格式: YYYY-MM-DD HH:mm)", defaultVal)
    if (!newDateStr) return

    try {
        // Handle space vs T
        const normalized = newDateStr.trim().replace(' ', 'T')
        const dateObj = new Date(normalized)
        
        if (isNaN(dateObj.getTime())) {
            alert("日期格式錯誤，請依照 YYYY-MM-DD HH:mm 格式輸入")
            return
        }

        const isoDeadline = dateObj.toISOString()
        await axios.patch(`/api/groups/${groupId}/deadline`, { deadline: isoDeadline })
        alert("結單時間已延長！大家可以繼續點餐了。")
        await fetchGroupData()
    } catch (err) {
        console.error("Extend deadline error", err)
        const errorMessage = err.response?.data?.error || err.response?.data?.message || err.message
        alert(`延長失敗: ${errorMessage}`)
    }
}

onMounted(() => {
    fetchGroupData()
    timer.value = setInterval(() => {
        currentTime.value = new Date()
    }, 60000)
})

onUnmounted(() => {
    if (timer.value) clearInterval(timer.value)
})
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #E6E2DE;
  border-radius: 20px;
}
</style>
