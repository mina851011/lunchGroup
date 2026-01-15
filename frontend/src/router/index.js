import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import OrderPage from '../views/OrderPage.vue'
import GroupStats from '../views/GroupStats.vue'

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
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
