import { createRouter, createWebHashHistory } from 'vue-router'
import LandingPage from '../views/LandingPage.vue'
import HomePage from '../views/HomePage.vue'
import OrderPage from '../views/OrderPage.vue'
import GroupStats from '../views/GroupStats.vue'
import SettlementPage from '../views/SettlementPage.vue'
import Instructions from '../views/Instructions.vue'

const regionRoutes = (prefix) => [
    {
        path: `/${prefix}`,
        name: `Home-${prefix}`,
        component: HomePage
    },
    {
        path: `/${prefix}/group/:groupId`,
        name: `Order-${prefix}`,
        component: OrderPage
    },
    {
        path: `/${prefix}/group/:groupId/stats`,
        name: `Stats-${prefix}`,
        component: GroupStats
    },
    {
        path: `/${prefix}/group/:groupId/settlement`,
        name: `Settlement-${prefix}`,
        component: SettlementPage
    },
    {
        path: `/${prefix}/instructions`,
        name: `Instructions-${prefix}`,
        component: Instructions
    }
]

const routes = [
    {
        path: '/',
        name: 'Landing',
        component: LandingPage
    },
    // Backward-compatible redirects for legacy links without region prefix.
    { path: '/group/:groupId', redirect: to => `/taichung/group/${to.params.groupId}` },
    { path: '/group/:groupId/stats', redirect: to => `/taichung/group/${to.params.groupId}/stats` },
    { path: '/group/:groupId/settlement', redirect: to => `/taichung/group/${to.params.groupId}/settlement` },
    { path: '/instructions', redirect: '/taichung/instructions' },
    ...regionRoutes('taichung'),
    ...regionRoutes('taipei'),
]

const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL),
    routes
})

export default router
