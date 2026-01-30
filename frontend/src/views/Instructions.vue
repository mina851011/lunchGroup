<template>
  <div class="min-h-screen bg-[#FDFBF7] p-6 md:p-12">
    <div class="max-w-3xl mx-auto">
      <div class="mb-8 flex justify-between items-center">
        <h1 class="text-3xl font-bold text-mocha-dark">📖 使用說明</h1>
        <router-link 
          to="/" 
          class="px-4 py-2 bg-white border border-stone-200 text-stone-600 rounded-xl hover:bg-stone-50 transition-colors font-bold shadow-sm"
        >
          ↩️ 回首頁
        </router-link>
      </div>

      <div class="bg-white rounded-[2rem] p-8 shadow-sm border border-stone-100 space-y-8 text-mocha-dark leading-relaxed">
        
        <section>
          <h2 class="text-xl font-bold mb-4 flex items-center gap-2">
            <span class="text-2xl">⚠️</span> 注意事項
          </h2>
          <div class="bg-orange-50 border border-orange-100 rounded-xl p-4 text-orange-800">
            <strong>常吃店家有跑出來才是有連上後端</strong><br>
            系統 15 分鐘會休眠，第一次開啟若讀取較久是正常的，請等它一分鐘起床 😴
          </div>
        </section>

        <section>
          <h2 class="text-xl font-bold mb-4 flex items-center gap-2">
            <span class="text-2xl">🥢</span> 加入新菜單
          </h2>
          <ol class="list-decimal list-inside space-y-2 pl-2 text-stone-600">
            <li>
              <strong>相機 AI 辨識菜單</strong>：點選相機圖示拍照或上傳。
              <span class="text-xs bg-stone-100 px-2 py-0.5 rounded text-stone-400">不要亂玩 QQ 不知道啥時會用光 Token</span>
              <br>
              <span class="ml-5 text-sm">匯入後可以再對一下有沒有錯誤，有的話請手動編輯修正。</span>
            </li>
            <li>
              <strong>直接輸入菜單</strong>：在文字框輸入，例如：「排骨飯 130」。
            </li>
          </ol>
          <p class="mt-3 text-sm text-stone-500 pl-2">
            💡 可以補傳菜單圖片上去大家比較好看。<br>
            💡 每次點開店家都可以再編輯菜單價格，補上備註、電話，最後記得按 <strong>"儲存店家"</strong> 🗂️
          </p>
        </section>

        <section>
          <h2 class="text-xl font-bold mb-4 flex items-center gap-2">
            <span class="text-2xl">🥄</span> 開團與分享
          </h2>
          <div class="space-y-2 text-stone-600 pl-2">
            <p>選好店家 ➡️ 按下開團之後，上方會有 <strong>分享連結</strong>，直接複製貼給同事即可。</p>
            <p class="bg-blue-50 border border-blue-100 p-3 rounded-lg text-blue-800 text-sm">
              開好團之後就先不要再開新團，Excel <strong>Orders</strong> 表只會有最新的團，舊的會跑去 <strong>History Orders</strong>。
            </p>
          </div>
        </section>

        <section>
          <h2 class="text-xl font-bold mb-4 flex items-center gap-2">
            <span class="text-2xl">💲</span> 結算收款
          </h2>
          <p class="text-stone-600 pl-2">
            還沒收單前也可以點結算頁面，不衝突～可以隨時記錄有沒有收到錢。
          </p>
        </section>

        <section>
          <h2 class="text-xl font-bold mb-4 flex items-center gap-2">
            <span class="text-2xl">📊</span> 後台 Excel
          </h2>
          <a 
            href="https://docs.google.com/spreadsheets/d/1ytTayvYMMmE67lYgfzbN_2u_KQPyR6ETGGUi5T4qY-0/edit?usp=sharing" 
            target="_blank"
            class="inline-flex items-center gap-2 text-green-600 hover:text-green-700 font-bold bg-green-50 px-4 py-3 rounded-xl border border-green-100 hover:bg-green-100 transition-colors w-full md:w-auto justify-center"
          >
            <span>打開 Google Sheets 試算表</span>
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/></svg>
          </a>
        </section>

      </div>
    </div>
  </div>
</template>

<script setup>
// No script logic needed for this static page
</script>
