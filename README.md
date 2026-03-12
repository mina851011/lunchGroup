# 波奇探險隊 (Poké Expedition) - 辦公室點餐系統 🐧🍱

這是一個專為辦公室設計的點餐管理系統，旨在簡化團購流程、自動加總訂單並整合 Google Sheets 作為後端儲存。

## � 線上展示 (Demo)
- **前端預覽**：[https://mina851011.github.io/lunchGroup/](https://mina851011.github.io/lunchGroup/)
> [!NOTE]
> 由於 GitHub Pages 僅託管靜態前端，若後端未部署，功能（如點餐、OCR）將暫時無法使用。

## �🌟 主要功能

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
- **本地開發**：
  - 填入 `google.sheets.spreadsheet-id-taichung` 與 `google.sheets.spreadsheet-id-taipei`。
  - 並將金鑰 JSON 放在指定路徑。
- **雲端部署 (Render/Koyeb)**：
  - 設定環境變數 `SPREADSHEET_ID_TAICHUNG`（或 `GOOGLE_SHEETS_SPREADSHEET_ID_TAICHUNG`）。
  - 設定環境變數 `SPREADSHEET_ID_TAIPEI`（或 `GOOGLE_SHEETS_SPREADSHEET_ID_TAIPEI`）。
  - 設定環境變數 `GOOGLE_SHEETS_CREDENTIALS_JSON`（或 `GOOGLE_CREDENTIALS_JSON`），直接把 **整份 JSON 檔案的內容** 貼進去。系統會優先讀取此變數。
  - 可選：設定資料保留策略
    - `DATA_RETENTION_ENABLED=true`
    - `DATA_RETENTION_DAYS=10`
    - `DATA_RETENTION_CRON=0 30 3 * * *`（每天 03:30，Asia/Taipei）
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
1. `Groups`: 團購主檔（含 `region` 欄位）。
2. `Menus`: 菜單品項。
3. `Orders`: 用於存放進行中的訂單。
4. `History Orders`: 用於存放已結單的訂單。
5. `Restaurants`: 用於存放常用店家清單。

## 🌍 多地區入口
- 入口頁：`#/`
- 台中：`#/taichung`
- 台北：`#/taipei`

前端會依網址自動帶 `X-Region` request header：
- `#/taichung` → `X-Region: taichung`
- `#/taipei` → `X-Region: taipei`

## 🔔 LINE 通知規則
- 結單 LINE 通知僅使用 `taichung` 資料來源。
- `taipei` 不會觸發結單 LINE 推播。

## 📝 備註
本系統由 **波奇探險隊** 榮譽出品。
🐜✨
