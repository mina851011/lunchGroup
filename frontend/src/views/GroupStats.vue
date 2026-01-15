<template>
  <div class="group-stats">
    <h1>Group Statistics</h1>
    <p>Stats for Group ID: {{ $route.params.groupId }}</p>
    <router-link :to="`/group/${$route.params.groupId}`">Back to Order</router-link>
  </div>
</template>

<script setup>
</script>

<style scoped>
.group-stats {
  padding: 2rem;
}
</style>
