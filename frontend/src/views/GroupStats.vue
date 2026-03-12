<template>
  <div class="group-stats">
    <h1>Group Statistics</h1>
    <p>Stats for Group ID: {{ $route.params.groupId }}</p>
    <router-link :to="`${regionPrefix}/group/${$route.params.groupId}`">Back to Order</router-link>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const regionPrefix = computed(() => route.path.startsWith('/taipei') ? '/taipei' : '/taichung')
</script>

<style scoped>
.group-stats {
  padding: 2rem;
}
</style>
