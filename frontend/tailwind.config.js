/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                'cream-bg': '#F9F8F6', // Very soft warm white
                'cream-card': '#FFFFFF',
                'mocha-text': '#5A5550', // Soft brownish grey text
                'mocha-primary': '#8D8177', // Muted earth tone for buttons
                'mocha-dark': '#463F3A',
                'soft-gray': '#E5E5E5',
            },
            fontFamily: {
                sans: ['Inter', 'system-ui', 'sans-serif'],
            }
        },
    },
    plugins: [],
}
