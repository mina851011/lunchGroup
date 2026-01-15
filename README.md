# 波奇探險隊 (Poké Expedition) - 辦公室點餐系統 🐧🍱

這是一個專為辦公室設計的點餐管理系統，旨在簡化團購流程、自動加總訂單並整合 Google Sheets 作為後端儲存。

## 🌟 主要功能

- **🚀 快速建立團購**：輸入店家名稱、截單時間即可開啟。
- **🤖 智慧菜單辨識 (AI OCR)**：支援直接上傳菜單圖片，自動辨識品項與價格。
- **📊 Google Sheets 整合**：所有訂單即時同步至 Google Sheets，並自動在表末產生「總計列」。
- **⏰ 延期功能**：如果大家還沒訂完，主揪可以一鍵延長結單時間。
- **📦 訂單歸檔**：結單後自動將訂單移至歷史紀錄，保持當前表單簡潔。
- **💅 精美介面**：現代化的 UI 設計，支援自訂飯量 (FULL/HALF) 與備註。

## 🛠️ 技術棧

- **Frontend**: Vue 3, Vite, TailwindCSS
- **Backend**: Java 17, Spring Boot 3.x
- **Storage**: Google Sheets API v4
- **OCR**: Integrated Gemini AI / Google Vision API

## 🚀 快速開始

### 1. 複製專案
```bash
git clone https://github.com/mina851011/lunchGroup.git
cd lunchGroup
```

### 2. 後端設置 (Spring Boot)
- 進入 `backend/` 目錄。
- 複製 `src/main/resources/application.properties.example` 為 `application.properties`。
- 填入您的 `google.sheets.id` 與 Google Service Account 金鑰路徑。
- 執行專案：
```bash
./mvnw spring-boot:run
```

### 3. 前端設置 (Vue 3)
- 進入 `frontend/` 目錄。
- 安裝依賴：
```bash
npm install
```
- 啟動開發伺服器：
```bash
npm run dev
```

## 📈 Google Sheet 結構要求
請確保您的 Google Sheet 包含以下工作表：
1. `Orders`: 用於存放進行中的訂單。
2. `History Orders`: 用於存放已結單的訂單。
3. `Restaurants`: 用於存放常用店家清單。

## 📝 備註
本系統由 **波奇探險隊** 榮譽出品。
🐜✨
