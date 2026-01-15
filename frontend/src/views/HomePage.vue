<template>
  <div class="min-h-screen flex items-center justify-center p-4">
    <div class="w-full max-w-2xl bg-white rounded-[2rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] p-8 text-center border border-stone-100 transition-all duration-300">
      <div class="mb-6">
        <h1 class="text-3xl font-bold text-mocha-dark mb-2 tracking-tight">波奇探險隊</h1>
        <p class="text-mocha-text opacity-80 text-sm font-medium">簡單快速的辦公室點餐系統</p>
      </div>
      
      <!-- Store Management Section -->
      <div class="mb-8 p-4 bg-stone-50 rounded-2xl border border-stone-100 text-left">
          <label class="block text-sm font-bold text-mocha-dark mb-3">📂 選擇常吃店家</label>
          <div class="flex gap-2 mb-4">
              <select 
                  v-model="selectedStoreId" 
                  @change="loadStore"
                  class="flex-1 bg-white border border-stone-200 rounded-xl px-4 py-2 text-stone-600 focus:ring-2 focus:ring-mocha-primary/50"
              >
                  <option value="">-- 請選擇店家 --</option>
                  <option v-for="store in restaurants" :key="store.id" :value="store.id">
                      {{ store.name }}
                  </option>
              </select>
          </div>

          <hr class="border-stone-200 my-4 border-dashed"/>
          
          <label class="block text-sm font-bold text-mocha-dark mb-3">🤖 或是：智慧辨識新菜單</label>
          
          <!-- Smart Menu Input -->
          <div class="relative mb-4">
              <textarea
                v-model="rawMenuText"
                @input="parseMenuText"
                class="w-full h-24 bg-white border border-stone-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-mocha-primary/50 text-mocha-dark placeholder-stone-300 transition-all resize-none pr-12"
                placeholder="貼上文字，或按下相機按鈕辨識..."
              ></textarea>
  
              <!-- AI Scan Button -->
              <div class="absolute bottom-3 right-3 flex flex-col gap-2 scale-90">
                <button 
                  @click="triggerAIUpload"
                  class="p-2 bg-mocha-primary/10 rounded-lg shadow-sm text-mocha-primary hover:bg-mocha-primary hover:text-white hover:shadow-md transition-all border border-mocha-primary/20"
                  title="AI 辨識菜單"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0zM18.75 10.5h.008v.008h-.008V10.5z" />
                    </svg>
                </button>
              </div>
              
              <input 
                  ref="aiFileInput"
                  type="file" 
                  accept="image/*" 
                  class="hidden"
                  @change="handleAIFileSelect"
              >
              <input 
                  ref="simpleFileInput"
                  type="file" 
                  accept="image/*" 
                  class="hidden"
                  @change="handleSimpleFileSelect"
              >
              
              <div class="absolute top-2 right-2">
                 <span v-if="ocrStatus" class="text-xs text-mocha-primary animate-pulse font-bold bg-white/80 px-2 py-1 rounded">{{ ocrStatus }}</span>
              </div>
          </div>
      </div>
      
      <!-- Menu Editor -->
      <div v-if="parsedMenu.length > 0" class="mb-6 text-left animate-fade-in-up">
          <div class="flex justify-between items-center mb-2">
            <label class="block text-sm font-bold text-mocha-dark">📝 確認/編輯菜單 ({{ parsedMenu.length }} 項)</label>
            
            <div class="flex items-center gap-2">
                <!-- VIEW ORIGINAL MENU BUTTON -->
                <a v-if="menuImageUrl" :href="menuImageUrl" target="_blank" class="text-xs font-bold text-mocha-primary bg-orange-50 px-2 py-1 rounded-lg border border-orange-100 hover:bg-orange-100 transition-all flex items-center gap-1 shadow-sm">
                    🖼️ 查看原始圖片
                </a>
                <!-- UPLOAD/UPDATE IMAGE BUTTON (Standalone upload) -->
                <button @click="triggerSimpleUpload" class="text-[10px] font-bold text-stone-400 hover:text-mocha-primary transition-colors flex items-center gap-1">
                    {{ menuImageUrl ? '📷 更換圖片' : '📷 補傳圖片' }}
                </button>
            </div>
          </div>
          
          <div v-if="menuImageUrl" class="mb-4 relative group w-fit mx-auto md:mx-0">
              <img :src="menuImageUrl" class="h-20 w-auto rounded-lg border border-stone-200 shadow-sm object-cover hover:scale-105 transition-all cursor-pointer" @click="window.open(menuImageUrl, '_blank')" />
              <div class="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button @click="menuImageUrl = ''" class="bg-red-500 text-white rounded-full p-1 shadow-md">
                      <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 18L18 6M6 6l12 12"/></svg>
                  </button>
              </div>
          </div>

          <div class="bg-stone-50 rounded-xl p-2 max-h-60 overflow-y-auto border border-stone-100 space-y-2">
              <div v-for="(item, idx) in parsedMenu" :key="idx" class="flex gap-2 items-center">
                  <input 
                    v-model="item.name"
                    class="flex-1 bg-white border border-stone-200 rounded-lg px-3 py-2 text-sm text-mocha-dark"
                    placeholder="品項名稱"
                  >
                  <input 
                    v-model.number="item.price"
                    type="number"
                    class="w-20 bg-white border border-stone-200 rounded-lg px-3 py-2 text-sm text-center text-mocha-dark"
                    placeholder="$"
                  >
                  <button 
                    @click="removeMenuItem(idx)"
                    class="p-2 text-stone-400 hover:text-red-400 transition-colors"
                  >
                    ×
                  </button>
              </div>
              
              <button 
                @click="addMenuItem"
                class="w-full py-2 text-sm text-stone-500 hover:text-mocha-primary border border-dashed border-stone-300 rounded-lg hover:bg-white transition-all"
              >
                ＋ 新增品項
              </button>
          </div>
          
          <!-- Save Store Section -->
          <div class="mt-4 flex gap-2 items-end">
              <div class="flex-1">
                  <label class="block text-xs text-stone-400 mb-1">儲存這份菜單為...</label>
                  <input 
                    v-model="newStoreName"
                    class="w-full bg-stone-50 border-none rounded-lg px-3 py-2 text-sm focus:ring-1 focus:ring-mocha-primary/30"
                    placeholder="例如：巷口排骨飯"
                  >
              </div>
              <button 
                @click="saveStore"
                :disabled="!newStoreName || parsedMenu.length === 0"
                class="bg-white border border-stone-200 text-mocha-text px-4 py-2 rounded-lg text-sm hover:bg-stone-50 disabled:opacity-50 transition-all font-medium whitespace-nowrap"
              >
                  💾 儲存店家
              </button>
          </div>
      </div>
      <!-- Group Creation Form -->
      <div class="space-y-6 text-left mb-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                  <label class="block text-sm font-bold text-mocha-dark mb-2">🎈 團購名稱</label>
                  <input 
                    v-model="groupName" 
                    type="text" 
                    class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 focus:ring-2 focus:ring-mocha-primary/50 text-mocha-dark placeholder-stone-300 transition-all font-bold"
                    placeholder="例如：Mina 的週四排骨大餐"
                  >
              </div>
              <div>
                  <label class="block text-sm font-bold text-mocha-dark mb-2">⏰ 結單時間</label>
                  <input 
                    v-model="deadline" 
                    type="datetime-local" 
                    class="w-full bg-stone-50 border border-stone-100 rounded-xl px-4 py-3 focus:ring-2 focus:ring-mocha-primary/50 text-mocha-dark transition-all font-bold"
                  >
              </div>
          </div>
      </div>

      <button 
        @click="createGroup"
        :disabled="isProcessing"
        class="w-full bg-mocha-dark text-white font-medium text-lg py-4 rounded-xl shadow-lg hover:bg-[#2C2825] hover:shadow-xl hover:-translate-y-0.5 transition-all duration-300 active:scale-95 active:shadow-sm disabled:opacity-50 disabled:cursor-not-allowed mt-4"
      >
        {{ isProcessing ? '處理中...' : '＋ 開啟新團購' }}
      </button>

      <div class="mt-8 pt-6 border-t border-stone-100">
        <p class="text-xs text-stone-400">已有團購連結？直接貼上瀏覽器即可跟團</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const rawMenuText = ref('')
const parsedMenu = ref([])
const aiFileInput = ref(null)
const simpleFileInput = ref(null)
const ocrStatus = ref('')
const isProcessing = ref(false)
const menuImageUrl = ref('')

// Store Management
const restaurants = ref([])
const selectedStoreId = ref('')
const newStoreName = ref('')
const groupName = ref('')
const deadline = ref('')

// Initialize default deadline to 1 hour from now formatted for datetime-local (LOCAL TIME)
const initDefaults = () => {
    const tzOffset = new Date().getTimezoneOffset() * 60000; // offset in milliseconds
    const localISOTime = new Date(Date.now() + 3600000 - tzOffset).toISOString().slice(0, 16);
    deadline.value = localISOTime;
}
initDefaults()

const fetchRestaurants = async () => {
    try {
        const res = await axios.get('/api/restaurants')
        restaurants.value = res.data || []
    } catch (e) {
        console.error("Failed to load restaurants", e)
    }
}

const loadStore = () => {
    const store = restaurants.value.find(r => r.id === selectedStoreId.value)
    if (store) {
        parsedMenu.value = JSON.parse(JSON.stringify(store.menu)) // Deep copy
        newStoreName.value = store.name // Auto-fill name for reference
        menuImageUrl.value = store.menuImageUrl || '' // LOAD IMAGE
    } else {
        parsedMenu.value = []
        newStoreName.value = ''
        menuImageUrl.value = ''
    }
}

const saveStore = async () => {
    if (!newStoreName.value) return
    try {
        await axios.post('/api/restaurants', {
            id: selectedStoreId.value || null, 
            name: newStoreName.value,
            menu: parsedMenu.value,
            menuImageUrl: menuImageUrl.value // SAVE IMAGE
        })
        alert(`店家「${newStoreName.value}」儲存成功！`)
        await fetchRestaurants() // Refresh list
    } catch (e) {
        alert("儲存失敗，請檢查後端")
    }
}

// Menu Editing
const addMenuItem = () => {
    parsedMenu.value.push({ name: '', price: '' })
}
const removeMenuItem = (index) => {
    parsedMenu.value.splice(index, 1)
}

// Upload & OCR
const triggerAIUpload = () => {
    aiFileInput.value.click()
}
const triggerSimpleUpload = () => {
    simpleFileInput.value.click()
}

const handleAIFileSelect = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    isProcessing.value = true
    ocrStatus.value = 'AI 辨識中...'
    const formData = new FormData()
    formData.append('file', file)
    
    try {
        const response = await axios.post('/api/ocr/menu', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        const { items, imageUrl } = response.data
        if (items && items.length > 0) {
             parsedMenu.value = [...parsedMenu.value, ...items]
             menuImageUrl.value = imageUrl
             ocrStatus.value = '辨識成功！'
        }
    } catch (err) {
        console.error("OCR Error", err)
        alert("辨識失敗")
        ocrStatus.value = '失敗'
    } finally {
        isProcessing.value = false
        if (aiFileInput.value) aiFileInput.value.value = ''
    }
}

const handleSimpleFileSelect = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    ocrStatus.value = '上傳中...'
    const formData = new FormData()
    formData.append('file', file)
    
    try {
        const response = await axios.post('/api/ocr/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        menuImageUrl.value = response.data.imageUrl
        ocrStatus.value = '圖片已更新'
        setTimeout(() => ocrStatus.value = '', 2000)
    } catch (err) {
        console.error("Upload Error", err)
        alert("上傳失敗")
        ocrStatus.value = ''
    } finally {
        if (simpleFileInput.value) simpleFileInput.value.value = ''
    }
}

const parseMenuText = () => {
    const lines = rawMenuText.value.split('\n')
    const results = []
    for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || trimmed.length < 2) continue
        const match = trimmed.match(/^(.*?)\s*[$]?(\d+)\s*$/)
        if (match) {
            results.push({ name: match[1].trim(), price: parseInt(match[2]) })
        }
    }
    if (results.length > 0) {
        parsedMenu.value = results
    }
}

const createGroup = async () => {
  try {
    let rName = newStoreName.value
    if (selectedStoreId.value) {
        const s = restaurants.value.find(r => r.id === selectedStoreId.value)
        if (s) rName = s.name
    }
      
    const payload = {
      name: groupName.value || (rName ? rName + " - " : "") + "午餐團", 
      deadline: new Date(deadline.value).toISOString(), 
      menu: parsedMenu.value,
      restaurantName: rName || "未命名店家",
      menuImageUrl: menuImageUrl.value
    }
    
    const response = await axios.post('/api/groups', payload)
    const newGroup = response.data
    if (newGroup && newGroup.id) {
       router.push(`/group/${newGroup.id}`)
    }
  } catch (err) {
    console.error("Failed to create group:", err)
    alert("建立團購失敗")
  }
}

onMounted(() => {
    fetchRestaurants()
})
</script>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.5s ease-out;
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
