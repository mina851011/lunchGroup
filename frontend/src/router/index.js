import { createRouter, createWebHashHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import OrderPage from '../views/OrderPage.vue'
import GroupStats from '../views/GroupStats.vue'
import SettlementPage from '../views/SettlementPage.vue'
import Instructions from '../views/Instructions.vue'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: HomePage
    },
    {
        path: '/group/:groupId',
        name: 'Order',
        component: OrderPage
    },
    {
        path: '/group/:groupId/stats',
        name: 'Stats',
        component: GroupStats
    },
    {
        path: '/group/:groupId/settlement',
        name: 'Settlement',
        component: SettlementPage
    },
    {
        path: '/instructions',
        name: 'Instructions',
        component: Instructions
    }
]

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes
})

export default router
